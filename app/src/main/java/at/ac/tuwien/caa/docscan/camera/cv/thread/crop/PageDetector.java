package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Looper;
import androidx.exifinterface.media.ExifInterface;
import android.util.Log;
import android.util.Xml;

import com.crashlytics.android.Crashlytics;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import at.ac.tuwien.caa.docscan.camera.cv.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.DkVector;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

public class PageDetector {

    private static final String BORDER_COORDS_PREFIX = "<Border><Coords points=\"";
    private static final String UNFOCUSED_TAG = "<Focused value=\"false\"/>";
    private static final String BORDER_COORDS_POSTFIX_1 = "\"/>";
    private static final String BORDER_COORDS_POSTFIX_2 = "</Border>";
//    private static final String BORDER_COORDS_POSTFIX = "\"/></Border>";
    private static final String CLASS_TAG = "PageDetector";
    private static final String CROPPING_PREFIX = "<Cropping applied=\"true\"/>";

    //Matthias
    private static final String CROPPED = "cropped";
    private static final String IS_CROPPED = "name=\"cropped\" value=\"true\"";
    private static final String IS_NOT_CROPPED= "name=\"cropped\" value=\"false\"";
    private static final String FOCUSED = "focused";
    private static final String IS_FOCUSED = "name=\"focused\" value=\"true\"";
    private static final String IS_UNFOCUSED = "name=\"focused\" value=\"false\"";

    /**
     * Performs the page detection. Note this is done in the calling thread, so assure you are not
     * running this on main thread.
     * @param fileName
     * @return
     */
    static ArrayList<PointF> findRect(String fileName) {

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            Log.d(CLASS_TAG, "findRect: you should not perform this on the main thread!");
        }

        Mat inputMat = Imgcodecs.imread(fileName);

        if (inputMat.empty())
            return null;

        Mat mg = new Mat();
        Imgproc.cvtColor(inputMat, mg, Imgproc.COLOR_RGBA2RGB);

//        Resize the image:
        NativeWrapper.resize(mg);

        DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);

        ArrayList<PointF> result = null;
        if (polyRects.length > 0 && polyRects[0] != null) {
            result = normPoints(polyRects[0], mg.width(), mg.height());
        }

        inputMat.release();
        mg.release();

        return result;

    }

    public static PageFocusResult findRectAndFocus(String fileName) {

        Log.d(CLASS_TAG, "findRectAndFocus");

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            Log.d(CLASS_TAG, "findRect: you should not perform this on the main thread!");
        }

        Mat inputMat = Imgcodecs.imread(fileName);

//TOD: uncomment:
        if (inputMat.empty())
            return null;

        Mat mg = new Mat();
        Imgproc.cvtColor(inputMat, mg, Imgproc.COLOR_RGBA2RGB);

//        Resize the image:
        NativeWrapper.resize(mg);

        DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);

        PageFocusResult result = null;

        if (polyRects != null && polyRects.length > 0 && polyRects[0] != null)  {
            ArrayList<PointF> points = normPoints(polyRects[0], mg.width(), mg.height());

            Patch[] patches = NativeWrapper.getFocusMeasures(mg);
//            In case an error happened the result is null and we assume that the image is sharp:
            boolean isSharp = true;
//            Otherwise use the real patches result:
            if (patches != null)
                isSharp = isSharp(polyRects[0], patches);

            result = new PageFocusResult(points, isSharp);
        }

//
////        Log.d(CLASS_TAG, "issharp: " + isSharp);
//
//        if (polyRects.length > 0 && polyRects[0] != null) {
////            result = normPoints(polyRects[0], mg.width(), mg.height());
//        }

        inputMat.release();
        mg.release();

        return result;


    }

    private static boolean isSharp(DkPolyRect polyRect, Patch[] patches) {

        int sharpCnt = 0;
        int unsharpCnt = 0;

        for (Patch patch : patches) {
            if (polyRect != null) {
                if (patch != null  && patch.getPoint() != null
                        && patch.getIsForeGround() && polyRect.isInside(patch.getPoint())) {
                    if (patch.getIsSharp())
                        sharpCnt++;
                    else
                        unsharpCnt++;
                }
            }
        }

        return Math.round(((float) sharpCnt / (sharpCnt + unsharpCnt)) * 100) >= 50;


    }

    public static void rotate90Degrees(String fileName) {

        PageFocusResult result = getNormedCropPoints(fileName);
        if (result == null)
            return;

//        ArrayList<PointF> points = getNormedCropPoints(fileName);
        for (PointF point : result.getPoints()) {
            rotateNormedPoint(point, 90);
        }

        try {
            savePointsToExif(fileName, result.getPoints(), result.isFocused());
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

    }

    private static ArrayList<PointF> normPoints(DkPolyRect rect, int width, int height) {

        ArrayList<PointF> normedPoints = new ArrayList<>();

        for (PointF point : rect.getPoints()) {
            PointF normedPoint = new PointF();
            normedPoint.x = point.x / width;
            normedPoint.y = point.y / height;
            normedPoints.add(normedPoint);
        }

        return normedPoints;

    }

//    /**
//     * Reads the points from the exif and rotates them 90 degrees and saves it again to the exif.
//     * @param fileName
//     */
//    static void rotatePoints90Degrees(String fileName, int width, int height) {
//
//        ArrayList<PointF> points = getNormedCropPoints(fileName, width, height);
//
//    }

    private static void rotateNormedPoint(PointF point, int angle) {

        switch (angle) {

            case 90:
                float tmpY = point.x;
                point.x = 1-point.y;
                point.y = tmpY;
                break;
            case 180:
                point.x = 1-point.x;
                point.y = 1-point.y;
                break;
            case 270:
                float tmpX = point.y;
                point.y = 1-point.x;
                point.x = tmpX;
                break;
            default:
                break; // do not alter the point here.

        }

    }

    public static void savePointsToExif(String fileName, ArrayList<PointF> points, boolean isFocused)
            throws IOException {

        ExifInterface exif = new ExifInterface(fileName);
        // Save the coordinates of the page detection:
        if (points != null) {
            String coordString = getCoordString(points, isFocused, fileName);
            if (coordString != null) {
                exif.setAttribute(ExifInterface.TAG_MAKER_NOTE, coordString);
                exif.saveAttributes();
                Log.d(CLASS_TAG, "savePointsToExif: coordString" + coordString);
            }
        }

    }

    /**
     *  Returns a string describing the result of the NativeWrapper.getPageSegmentation page
     *  detection.
     *  The result string is formatted according to the prima PAGE XML:
     *  @see <a href="https://github.com/PRImA-Research-Lab/PAGE-XML/wiki">https://github.com/PRImA-Research-Lab/PAGE-XML/wiki</a>
     * @param points
     * @return
     */
    private static String getCoordString(ArrayList<PointF> points, boolean isFocused, String filename) throws IOException {
        Log.d(CLASS_TAG,"getCoordString");

        if (points == null)
            return null;
        else if (points.size() != 4)
            return null;

        final ExifInterface exif = new ExifInterface(filename);


        String creator = exif.getAttribute(ExifInterface.TAG_ARTIST);
        if (creator == null) {
            creator = "DocScan App";
        }
        final String created = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        final String lastChange = exif.getAttribute(ExifInterface.TAG_DATETIME);
        final String imageFilename = filename.substring(filename.lastIndexOf("/")+1);
        final String imageWidth = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
        final String imageHeight = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

        final String tagPcGts = "PcGts";
        final String tagCreator = "Creator";
        final String tagCreated = "Created";
        final String tagLastChange = "lastChange";
        final String tagMetadata = "Metadata";
        final String tagMetadataItem = "MetadataItem";
        final String tagPage = "Page";
        final String tagCustomRegion = "CustomRegion";
        final String tagCoords = "Coords";
        final String attributePcGtsId = "pcGtsId";
        final String attributeImageFilename = "imageFilename";
        final String attributeImageWidth = "imageWidth";
        final String attributeImageHeight = "imageHeight";
        final String attributePoints = "points";



        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        Log.d(CLASS_TAG, "start xml creation");
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", tagPcGts);
                serializer.attribute("", "xmlns", "http://schema.primaresearch.org/PAGE/gts/pagecontent/2013-07-15");
                serializer.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                serializer.attribute("", "xsi:schemaLocation", "http://schema.primaresearch.org/PAGE/gts/pagecontent/2018-07-15 http://schema.primaresearch.org/PAGE/gts/pagecontent/2018-07-15/pagecontent.xsd");
                serializer.attribute("", attributePcGtsId, "DocScan");
                serializer.startTag("", tagMetadata);
                    serializer.startTag("", tagCreator);
                        serializer.text(creator);
                    serializer.endTag("", tagCreator);
                    serializer.startTag("", tagCreated);
                        serializer.text(created);
                    serializer.endTag("", tagCreated);
                    serializer.startTag("", tagLastChange);
                        serializer.text(lastChange);
                    serializer.endTag("", tagLastChange);
                    serializer.startTag("", tagMetadataItem);
                        serializer.attribute("", "name", CROPPED);
                        serializer.attribute("", "value", "false");
                    serializer.endTag("", tagMetadataItem);
                    serializer.startTag("", tagMetadataItem);
                        serializer.attribute("", "name", FOCUSED);
                        if (!isFocused) {
                            serializer.attribute("", "value", "false");
                        } else {
                            serializer.attribute("", "value", "true");
                        }
                    serializer.endTag("", tagMetadataItem);
                serializer.endTag("", tagMetadata);
                serializer.startTag("", tagPage);
                    serializer.attribute("", attributeImageFilename, imageFilename);
                    serializer.attribute("", attributeImageWidth, imageWidth);
                    serializer.attribute("", attributeImageHeight, imageHeight);


                    if (points.size() == 4) {
                        int idx = 0;
                        String coordinates = "";

                        for (PointF point : points) {

                            coordinates += point.x + "," + point.y;
                            if (idx < 3)
                                coordinates += " ";
                            idx++;
                        }

                        serializer.startTag("", tagCustomRegion);
                        serializer.attribute("", "id", "l1");
                        serializer.startTag("", tagCoords);
                        serializer.attribute("", attributePoints, coordinates);
                        serializer.endTag("", tagCoords);
                        serializer.endTag("", tagCustomRegion);
                    } else if (points.size() == 8) {
                        String coordinates1 = "";
                        String coordinates2 = "";

                        for (int i = 0; i < points.size(); ++i) {
                            if (i < 4) {
                                coordinates1 += points.get(i).x + "," + points.get(i).y;
                                if (i < 3)
                                    coordinates1 += " ";
                            } else {
                                coordinates2 += points.get(i).x + "," + points.get(i).y;
                                if (i < 7)
                                    coordinates2 += " ";
                            }
                        }


                        serializer.startTag("", tagCustomRegion);
                        serializer.attribute("", "id", "l1");
                        serializer.startTag("", tagCoords);
                        serializer.attribute("", attributePoints, coordinates1);
                        serializer.endTag("", tagCoords);
                        serializer.endTag("", tagCustomRegion);


                        serializer.startTag("", tagCustomRegion);
                        serializer.attribute("", "id", "r1");
                        serializer.startTag("", tagCoords);
                        serializer.attribute("", attributePoints, coordinates2);
                        serializer.endTag("", tagCoords);
                        serializer.endTag("", tagCustomRegion);

                    } else {
                        Log.e(CLASS_TAG, "Expected either 4 or 8 corner points. Got " + points.size());
                    }


                serializer.endTag("", tagPage);
            serializer.endTag("", tagPcGts);
            serializer.endDocument();

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


//    public static ArrayList<PointF> getScaledCropPoints(String fileName, int width, int height) {
//
////        ArrayList<PointF> points = getNormedCropPoints(fileName);
//        PageFocusResult result = getNormedCropPoints(fileName);
//        scalePoints(result.getPoints(), width, height);
//
//        return result.getPoints();
//
//    }

    public static PageFocusResult getScaledCropPoints(String fileName, int width, int height) {

//        ArrayList<PointF> points = getNormedCropPoints(fileName);
        PageFocusResult result = getNormedCropPoints(fileName);
        scalePoints(result.getPoints(), width, height);

        return result;

    }



    public static ArrayList<PointF> getParallelPoints(ArrayList<PointF> points, String fileName) {

//        Took me some time to figure out that you cannot use simply the vertex centroid. Instead
//        one should have used the area centroid. However you can simply calculate the parallel
//        lines of the quadrilateral and intersect them (much easier):
//        https://stackoverflow.com/a/50873087/9827698

        float diagLength = getDiagonalLength(fileName);
        float offset = diagLength * 0.01f; // add an offset of 1% to the coordinates

        ArrayList<PointF> outerPoints = new ArrayList<>();

        int p1Idx, p2Idx, p3Idx;

        for (int i = 0; i < points.size(); i++) {

            p2Idx = i;
            p1Idx = (i - 1) % 4;
            if (p1Idx < 0)
                p1Idx += 4;
            p3Idx = (i + 1) % 4;

            PointF intersection = getIntersection(
                    points.get(p1Idx), points.get(p2Idx), points.get(p3Idx), offset);

            outerPoints.add(intersection);

        }

        return outerPoints;

    }

    private static float getDiagonalLength(String fileName) {

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        return (float) Math.sqrt(
                options.outWidth * options.outWidth + options.outHeight * options.outHeight);

    }


    private static PointF getIntersection(PointF p1, PointF p2, PointF p3, float offset) {

        DkVector vl = new DkVector(p1, p2);
//        Construct the normal vector:
        DkVector vln = new DkVector(-vl.y, vl.x).norm().multiply(offset);

        DkVector vr = new DkVector(p3, p2);
//        Construct the normal vector:
        DkVector vrn = new DkVector(vr.y, -vr.x).norm().multiply(offset);

        PointF plt = new PointF(p1.x + vln.x, p1.y + vln.y); // p1
        float x1 = plt.x;
        float y1 = plt.y;

        PointF plb = new PointF(p2.x + vln.x, p2.y + vln.y); // p2
        float x2 = plb.x;
        float y2 = plb.y;

        PointF prt = new PointF(p3.x + vrn.x, p3.y + vrn.y); // p3
        float x3 = prt.x;
        float y3 = prt.y;

        PointF prb = new PointF(p2.x + vrn.x, p2.y + vrn.y); // p4
        float x4 = prb.x;
        float y4 = prb.y;

//        Formula taken from: https://de.wikipedia.org/wiki/Schnittpunkt#Schnittpunkt_zweier_Geraden
        float xs = ((x4 - x3) * (x2 * y1 - x1 * y2) - (x2 - x1) * (x4 * y3 - x3 * y4)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        float ys = ((y1 - y2) * (x4 * y3 - x3 * y4) - (y3 - y4) * (x2 * y1 - x1 * y2)) /
                ((y4 - y3) * (x2 - x1) - (y2 - y1) * (x4 - x3));

        PointF intersect = new PointF(xs, ys);

        return intersect;

    }


    /**
     * Searches in the exif data for the maker note and returns the page detection coordinates if
     * the attribute is correctly formatted according to the prima PAGE XML definition.
     * @return
     */
    public static PageFocusResult getNormedCropPoints(String fileName) {
        Log.d("PD","getNormedCropPoints");

        try {
            ExifInterface exif = new ExifInterface(fileName);
            String XMLString = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE);
            if (XMLString != null) {
//  Take care that the string is well formed:
                if (XMLString.contains(BORDER_COORDS_PREFIX)) {
                    int idxStart = BORDER_COORDS_PREFIX.length();
                    int idxEnd = XMLString.indexOf(BORDER_COORDS_POSTFIX_1);
//                    int idxEnd = coordString.length() - BORDER_COORDS_POSTFIX_1.length();
                    String coords = XMLString.substring(idxStart, idxEnd);

                    String[] coordPairs = coords.split(" ");

                    ArrayList<PointF> points = new ArrayList<>();
                    for (String coordPair : coordPairs) {
                        String[] coord = coordPair.split(",");
                        float x = Float.parseFloat(coord[0]);
                        float y = Float.parseFloat(coord[1]);
                        points.add(new PointF(x, y));
                    }

                    boolean isFocused = !XMLString.contains(UNFOCUSED_TAG);

                    return new PageFocusResult(points, isFocused);

                } else if (XMLString.startsWith("<?xml")) {
                    try {
                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        XmlPullParser xpp = factory.newPullParser();
                        xpp.setInput( new StringReader( XMLString ) );

                        int eventType = xpp.getEventType();
                        ArrayList<PointF> points = new ArrayList<>();

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            String name = xpp.getName();
                            switch (eventType) {
                                case XmlPullParser.START_TAG:
                                    break;
                                case XmlPullParser.END_TAG:
                                    if (name.equals("Coords")) {
                                        final String coords = xpp.getAttributeValue(null, "points");
                                        String[] coordPairs = coords.split(" ");

                                        for (String coordPair : coordPairs) {
                                            String[] coord = coordPair.split(",");
                                            float x = Float.parseFloat(coord[0]);
                                            float y = Float.parseFloat(coord[1]);
                                            points.add(new PointF(x, y));
                                        }
                                    }
                            }

                            eventType = xpp.next();
                        }

                        boolean isFocused = !XMLString.contains(IS_UNFOCUSED);

                        if (points.size() == 4) {
                            return new PageFocusResult(points, isFocused);
                        } else if (points.size() == 8) {
                            ArrayList<PointF> pointsDoublePage = new ArrayList<>();
                            pointsDoublePage.add(points.get(0));//top left corner
                            pointsDoublePage.add(points.get(1));//top pageSplit corner
                            pointsDoublePage.add(points.get(5));//top right corner
                            pointsDoublePage.add(points.get(6));//bottom right corner
                            pointsDoublePage.add(points.get(2));//bottom pageSplit corner
                            pointsDoublePage.add(points.get(3));//bottom left corner

                            return new PageFocusResult(pointsDoublePage, isFocused);
                        } else {
                            Log.e(CLASS_TAG, "Invalid number of points: " + points.size());
                        }



                    }
                    catch (XmlPullParserException e) {
                        Log.e(CLASS_TAG, "Could not create XmlPullParser Instance");
                    }
                }

            }
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

//        If no point set is found use the entire image dimension:
        return new PageFocusResult(getNormedDefaultPoints(), true);

    }

    private static void scalePoints(ArrayList<PointF> points, int width, int height) {

        for (PointF point : points) {
            point.x *= width;
            point.y *= height;
        }

    }


    /**
     * Returns points that are located at the image corners.
     * @return
     */
    static ArrayList<PointF> getNormedDefaultPoints() {

        ArrayList<PointF> points = new ArrayList<>();
//        Take care about the direction, must be clock-wise:
        points.add(new PointF(0, 0));
        points.add(new PointF(1, 0));
        points.add(new PointF(1, 1));
        points.add(new PointF(0, 1));

        return points;

    }

    public static void saveAsCropped(String fileName) {

        try {
            ExifInterface exif = new ExifInterface(fileName);
            String XMLString = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE);

            if (XMLString != null) {
                if (XMLString.contains(BORDER_COORDS_PREFIX)) {
                    XMLString = CROPPING_PREFIX + XMLString;
                    exif.setAttribute(ExifInterface.TAG_MAKER_NOTE, XMLString);
                    exif.saveAttributes();
                } else if (XMLString.startsWith("<?xml")) {
                    final String tagMetadataItem = "MetadataItem";
                    final String attributeName = "MetadataItem";
                    final String attributeValue = "MetadataItem";

                    try {
                        final String modifedXMLString = modifyAttributeValue(XMLString, tagMetadataItem, attributeName, attributeValue);
                        Log.d(CLASS_TAG, modifedXMLString);

                        exif.setAttribute(ExifInterface.TAG_MAKER_NOTE, modifedXMLString);
                        exif.saveAttributes();
                    }
                    catch (Exception e) {
                        Log.e(CLASS_TAG, "Could not parse XML string");
                    }

                } else {
                    Log.e(CLASS_TAG, "Invalid XML format");
                }
            }

        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }


    }


    /***
     * Modifies an existing attribute of a page xml String
     * @param XMLString The String containing the page XML file
     * @param tagName Name of tag
     * @param attributeName Name of attribute in tag
     * @param attributeValue New value of attribute
     * @return Modified xml String
     */
    public static String modifyAttributeValue(String XMLString, String tagName, String attributeName, String attributeValue) throws Exception {
        // Based on: https://stackoverflow.com/questions/36107697/update-modify-an-xml-file-in-android
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(XMLString)));

        NodeList nodeslist = doc.getElementsByTagName(tagName);

        for(int i = 0; i < nodeslist.getLength(); ++i){
            Node node = nodeslist.item(i);
            NamedNodeMap att = node.getAttributes();

            int h = 0;
            boolean isCroppingAttribute = false;
            while(h < att.getLength()) {
                Node metadataInfo = att.item(h);

                if(metadataInfo.getNodeValue().equals(attributeName))
                    isCroppingAttribute= true;
                if(h == 1 && isCroppingAttribute)
                    metadataInfo.setNodeValue(attributeValue);
                h += 1;
            }
        }
        return toString(doc);
    }

//    Taken from https://stackoverflow.com/questions/2567416/xml-document-to-string

    /***
     * Return string with content from xml file in doc
     * @param doc A Document containing an xml file
     * @return the xml String
     */
    public static String toString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public static boolean isCropped(String fileName) {
        Log.d("PD","isCropped");

        try {
            ExifInterface exif = new ExifInterface(fileName);
            String XMLString = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE);
            if (XMLString != null) {
                if (XMLString.startsWith(CROPPING_PREFIX))//Old
                    return true;
                else if (XMLString.contains(IS_CROPPED))
                    return true;
            }

        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        return false;

    }

    public static class PageFocusResult {

        private ArrayList<PointF> mPoints;
        private boolean mIsFocused;

        public PageFocusResult(ArrayList<PointF> points, boolean isFocused) {
            mPoints = points;
            mIsFocused = isFocused;
        }

        public boolean isFocused() {
            return mIsFocused;
        }

        public ArrayList<PointF> getPoints() {
            return mPoints;
        }

    }



}
