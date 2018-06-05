package at.ac.tuwien.caa.docscan.camera.threads;

import android.graphics.PointF;
import android.media.ExifInterface;
import android.os.Looper;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;

public class Cropper {

    public static final String BORDER_COORDS_PREFIX = "<Border><Coords points=\"";
    public static final String BORDER_COORDS_POSTFIX = "\"/></Border>";
    private static final String CLASS_TAG = "Cropper";

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

        Mat mg = new Mat();
        Imgproc.cvtColor(inputMat, mg, Imgproc.COLOR_RGBA2RGB);

//            TODO: put this into AsyncTask:
        DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);

        if (polyRects.length > 0 && polyRects[0] != null) {
            ArrayList<PointF> normedPoints = normPoints(polyRects[0], inputMat.width(), inputMat.height());
            return normedPoints;
        }

        return null;

    }

    public static void rotate90Degrees(String fileName) {

        ArrayList<PointF> points = getNormedCropPoints(fileName);
        for (PointF point : points) {
            rotateNormedPoint(point, 90);
        }

        try {
            savePointsToExif(fileName, points);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static ArrayList<PointF> normPoints(DkPolyRect rect, int width, int height) {

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

    static void rotateNormedPoint(PointF point, int angle) {

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

    public static void savePointsToExif(String fileName, ArrayList<PointF> points) throws IOException {

        ExifInterface exif = new ExifInterface(fileName);
        if (exif != null) {
            // Save the coordinates of the page detection:
            if (points != null) {
                String coordString = Cropper.getCoordString(points);
                if (coordString != null) {
                    exif.setAttribute(ExifInterface.TAG_MAKER_NOTE, coordString);
                    exif.saveAttributes();
                    Log.d(CLASS_TAG, "savePointsToExif: coordString" + coordString);
                }
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
    static String getCoordString(ArrayList<PointF> points) {

        if (points == null)
            return null;
        else if (points.size() != 4)
            return null;

//        <Border><Coords points="53,21 53,2444 1696,2444 1696,20"/></Border>
        String result = BORDER_COORDS_PREFIX;

        int idx = 0;
        for (PointF point : points) {
            result += point.x + "," + point.y;
            if (idx < 3)
                result += " ";
            idx++;
        }

        result += BORDER_COORDS_POSTFIX;

        return result;
    }


    public static ArrayList<PointF> getScaledCropPoints(String fileName, int width, int height) {

        ArrayList<PointF> points = getNormedCropPoints(fileName);
        scalePoints(points, width, height);

        return points;

    }

    /**
     * Searches in the exif data for the maker note and returns the page detection coordinates if
     * the attribute is correctly formatted according to the prima PAGE XML definition.
     * @return
     */
    public static ArrayList<PointF> getNormedCropPoints(String fileName) {

        try {
            ExifInterface exif = new ExifInterface(fileName);
            if (exif != null) {
                String coordString = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE);
                if (coordString != null) {
//  Take care that the string is well formed:
                    if (coordString.startsWith(BORDER_COORDS_PREFIX) &&
                            coordString.endsWith(BORDER_COORDS_POSTFIX)) {
                        int idxStart = BORDER_COORDS_PREFIX.length();
                        int idxEnd = coordString.length() - BORDER_COORDS_POSTFIX.length();
                        String coords = coordString.substring(idxStart, idxEnd);

                        String[] coordPairs = coords.split(" ");

                        ArrayList<PointF> points = new ArrayList<>();
                        for (String coordPair : coordPairs) {
                            String[] coord = coordPair.split(",");
                            float x = Float.parseFloat(coord[0]);
                            float y = Float.parseFloat(coord[1]);
                            points.add(new PointF(x, y));
                        }

                        return points;

                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        If no point set is found use the entire image dimension:
        return getNormedDefaultPoints();

    }

    static void scalePoints(ArrayList<PointF> points, int width, int height) {

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
//        I am not sure if we have to take care about the direction (cw vs. ccw):
        points.add(new PointF(0, 0));
        points.add(new PointF(0, 1));
        points.add(new PointF(1, 1));
        points.add(new PointF(0, 1));

        return points;

    }

//    /**
//     * Returns points that are located at the image corners.
//     * @param width
//     * @param height
//     * @return
//     */
//    static ArrayList<PointF> getDefaultPoints(int width, int height) {
//
//        ArrayList<PointF> points = new ArrayList<>();
////        I am not sure if we have to take care about the direction (cw vs. ccw):
//        points.add(new PointF(0, 0));
//        points.add(new PointF(0, height));
//        points.add(new PointF(width, height));
//        points.add(new PointF(0, height));
//
//        return points;
//
//    }





}
