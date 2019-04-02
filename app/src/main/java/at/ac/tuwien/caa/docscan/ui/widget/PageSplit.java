package at.ac.tuwien.caa.docscan.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import at.ac.tuwien.caa.docscan.logic.Helper;

import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.core.CvType.CV_8UC1;

/**
 *   Created by Matthias Wödlinger on 14.02.2019
 */

/**
 *  This class can be used to apply the page-split trained version of dhSegment (https://arxiv.org/abs/1804.10371)
 *  to an image file. The constructor will look for a model named MODEL_NAME.tflite in the assets folder.
 *  After initialization the model can be applied to a bitmap with {@link #applyPageSplit(Uri, Context)}.
 */
public class PageSplit {

    public static final String TAG = "PageSplit";

    /** Name of the model file stored in Assets. */
    private static final String MODEL_NAME = "mobilenet2_2103_1256_from_resized_images_output";
    private static final String MODEL_PATH = MODEL_NAME + ".tflite";

    /** Output folder */
    private static final String FOLDER_NAME = "PageSplit";

    /** TfLite model only works for fixed input dimension.
     *  The input will be resized internally. */
    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;

    /** An instance of the TfLite Interpreter */
    private Interpreter mTflite;


    /**
     *  Initializes the tensorflow-lite model.
     *
     *  @param  mContext ... the current context.
     *  @throws IOException if the model can not be opened.
     *          It will look for a model named MODEL_NAME.tflite in the assets folder.
     */
    public PageSplit(Context mContext) throws IOException {

        long startTime = SystemClock.uptimeMillis();
        mTflite = new Interpreter(loadModelFile(mContext));
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Created a Tensorflow Lite model in " + Long.toString((endTime - startTime)) + "ms");
    }


    /**
     * Applies the page split model to a saved bitmap.
     * The model produces three bitmaps:    - The output mask of the page split model
     *                                      - The input bitmap
     *                                      - An overlay of the output mask and the input bitmap.
     * The bitmaps will be saved in FOLDER_NAME and are visible in the gallery.
     *
     * @param   uri ... the Uri of the bitmap file.
     * @param   mContext ... the current context. Will be used to access the assets folder.
     * @return  returns 1 if the tflite model has not been initialized yet.
     *          Returns 0 after successful run.
     * @throws  IOException if there is a problem loading the bitmap.
     */
    public int applyPageSplit(Uri uri, Context mContext) throws IOException {

        if (mTflite == null) {
            Log.e(TAG, "PageSplit has not been initialized; Skipped.");
            return 1;
        }

        // Load input bitmap
        Bitmap inBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
        inBitmap = Bitmap.createScaledBitmap(inBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);

        final int rotationInDegrees = getExifAngle(uri);
        Matrix inMatrix = new Matrix();
        inMatrix.postRotate(rotationInDegrees+90); // The model needs the book rotated by 90°; TODO: find out why
        inBitmap = Bitmap.createBitmap(inBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inMatrix, true);


        // The TfLite model needs a float array as input.
        // For alternatives see: https://www.tensorflow.org/lite/apis#running_a_model_2
        float[][][][] inFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];
        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                int color = inBitmap.getPixel(x,y);
                inFloat[0][x][y][0] = Color.red(color);
                inFloat[0][x][y][1] = Color.green(color);
                inFloat[0][x][y][2] = Color.blue(color);
            }
        }

        // The output of the model will be stored in outFloat
        float[][][][] outFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];

        // Running the model
        long startTime = SystemClock.uptimeMillis();
        mTflite.run(inFloat, outFloat);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "tflite.run took " + Long.toString((endTime - startTime)) + "ms");


        // Create outBitmap and feed it with the pixels
        Bitmap outBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
        Mat pages = new Mat(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, CV_8UC1);
        Mat split = new Mat(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, CV_8UC1);

        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                final int r = (int) (255.0 * outFloat[0][x][y][0]);
                final int g = (int) (255.0 * outFloat[0][x][y][1]);
                final int b = (int) (255.0 * outFloat[0][x][y][2]);
                pages.put(DIM_IMG_SIZE_X-x, y, g+b); //page mask. g+b to make sure if finds both pages together and not separated
                split.put(DIM_IMG_SIZE_X-x, y, b); //pageSplit mask
                outBitmap.setPixel(x, y, Color.argb(255, r, g, b));
            }
        }
//        Rewrite for Bytebuffer:
//        Mat outMat = new Mat(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, CvType.CV_32FC3, outFloat);

        // Create binary mask for pages
        Log.d(TAG, "Obtain contours for pages");
        Mat pMask = applyThreshold(pages, -1);
        pMask = bwClean(pMask, 5);
        final List<MatOfPoint> pContours = findPolygonalRegions(pMask, 0.01, 5);
        if (pContours.isEmpty()) {
            Log.e(TAG, "no contours for pages found");
            return 1;
        }
        List<Point> pCorners = getCorners(pContours);



        // Create binary mask for pageSplit
        Log.d(TAG, "Obtain contours for split");
        Mat sMask = applyThreshold(split, -1);
        sMask = bwClean(sMask, 3);
        final List<MatOfPoint> sContours = findPolygonalRegions(sMask, 0.001, 3);
        if (sContours.isEmpty()) {
            Log.e(TAG, "no contours for split found");
            return 1;
        }

        List<Point> sCorners = getCorners(sContours);


        // Compute the seperator points. Top and Bottom. Will be used as corner points for the bounding rectangles
        double lambda = getSeparatorFraction(pCorners, sCorners);

        final Point tLeft = pCorners.get(0);
        final Point tRight = pCorners.get(1);
        final Point tDirection = new Point(tRight.x-tLeft.x, tRight.y-tLeft.y);
        final Point sTop = new Point(tLeft.x + lambda*tDirection.x, tLeft.y + lambda*tDirection.y);
//        Log.d(TAG, "Top: x: "+Double.toString(sTop.x)+" y: "+Double.toString(sTop.y));
//        final Point sTop = new Point((sCorners.get(0).x + sCorners.get(1).x)/2, (sCorners.get(0).y + sCorners.get(1).y)/2);

        final Point bLeft = pCorners.get(3);
        final Point bRight = pCorners.get(2);
        final Point bDirection = new Point(bRight.x-bLeft.x, bRight.y-bLeft.y);
        final Point sBottom = new Point(bLeft.x + lambda*bDirection.x, bLeft.y + lambda*bDirection.y);
//        Log.d(TAG, "Bottom: x: "+Double.toString(sBottom.x)+" y: "+Double.toString(sBottom.y));
//        final Point sBottom = new Point((sCorners.get(3).x + sCorners.get(3).x)/2, (sCorners.get(3).y + sCorners.get(2).y)/2);


        // Draw the bounding rectangles and corner points
        Bitmap contourOverlay = inBitmap.copy(inBitmap.getConfig(), true);
        Mat tmpMat = new Mat();

        Matrix overlayRotMatrix = new Matrix();
        overlayRotMatrix.postRotate(-90);
        contourOverlay = Bitmap.createBitmap(contourOverlay, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, overlayRotMatrix, true);


        bitmapToMat(contourOverlay, tmpMat);
        //fill mask with pages
        tmpMat.copyTo(pMask, pMask);


//        Imgproc.drawContours(tmpMat, pContours, -1, new Scalar(0,255,0));
//        Imgproc.drawContours(tmpMat, sContours, -1, new Scalar(0,0,255));

        Log.d(TAG, "TOP: x = "+Double.toString(sTop.x)+", y = "+Double.toString(sTop.y));
        Log.d(TAG, "BOTTOM: x = "+Double.toString(sBottom.x)+", y = "+Double.toString(sBottom.y));
        Imgproc.line(tmpMat, pCorners.get(0), sTop, new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, sTop, sBottom, new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, sBottom, pCorners.get(3), new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, pCorners.get(0), pCorners.get(3), new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, sTop, pCorners.get(1), new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, pCorners.get(1), pCorners.get(2), new Scalar(0, 255, 0), 2);
        Imgproc.line(tmpMat, sBottom, pCorners.get(2), new Scalar(0, 255, 0), 2);

//        for (int j = 0; j < 4; j++) {
//            Imgproc.line(tmpMat, pPoints[j], pPoints[(j+1) % 4], new Scalar(0,255,0));
//            Imgproc.line(tmpMat, sPoints[j], sPoints[(j+1) % 4], new Scalar(0,0,255));
//        }

//        Imgproc.circle(tmpMat, sTop, 10, new Scalar(255,0,0),2);
////        Imgproc.circle(tmpMat, sBottom, 10, new Scalar(255,0,0),2);
////        for (int j = 0; j < pCorners.size(); ++j) {
////            Imgproc.circle(tmpMat, pCorners.get(j), 10, new Scalar(255,0,0),2);
////        }


        matToBitmap(tmpMat, contourOverlay);


        // draw masks
        Bitmap pMaskBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
        matToBitmap(pMask, pMaskBitmap);

        Bitmap sMaskBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
        matToBitmap(sMask, sMaskBitmap);


        // Creates the overlay
        Bitmap overlayBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
        Canvas inOutOverlay = new Canvas(overlayBitmap);
        Paint paint = new Paint();
        inOutOverlay.drawBitmap(inBitmap, 0, 0, paint);
        paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN));
        inOutOverlay.drawBitmap(outBitmap, 0, 0, paint);


        overlayBitmap = Bitmap.createBitmap(overlayBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, overlayRotMatrix, true);

        // save the bitmaps.
        final String filename = MODEL_NAME + ".jpg";

        outBitmap = Bitmap.createBitmap(outBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, overlayRotMatrix, true);
        saveBitmap(outBitmap, filename, mContext);
        saveBitmap(contourOverlay, "Contours.jpg", mContext);
        saveBitmap(overlayBitmap, "Overlay.jpg", mContext);
        saveBitmap(pMaskBitmap, "Page_mask.jpg", mContext);
        saveBitmap(sMaskBitmap, "Split_mask.jpg", mContext);


        return 0;
    }

    private double getSeparatorFraction(List<Point> pCorners, List<Point> sCorners) {
        final double pMeanTop = (pCorners.get(0).y + pCorners.get(1).y)/2.0;
        final double pMeanBottom = (pCorners.get(2).y + pCorners.get(3).y)/2.0;
        final double sMeanTop = (sCorners.get(0).y + sCorners.get(1).y)/2.0;
        final double sMeanBottom = (sCorners.get(2).y + sCorners.get(3).y)/2.0;

        double lambda = 0;

        if (Math.abs(pMeanTop-sMeanTop) < Math.abs(pMeanBottom-sMeanBottom)) {
            final double sMeanX = (sCorners.get(0).x + sCorners.get(1).x)/2;
            final Point left = pCorners.get(0);
            final Point right = pCorners.get(1);
            final Point direction = new Point(right.x-left.x, right.y-left.y);

            Log.d(TAG,"Left.x = "+Double.toString(left.x)+" sMeanX = "+Double.toString(sMeanX));
            if (left.x > sMeanX) {
                return 0;
            } else {
                for (int i = 0; i < 100; ++i){
                    if ((left.x + lambda*direction.x) > sMeanX) {
                        break;
                    }
                    lambda = ((double) i)/100;
                }
                Log.d(TAG, "lambda: "+Double.toString(lambda));
            }

        } else {
            final double sMeanX = (sCorners.get(2).x + sCorners.get(3).x)/2;
            final Point left = pCorners.get(3);
            final Point right = pCorners.get(2);
            final Point direction = new Point(right.x-left.x, right.y-left.y);

            Log.d(TAG,"Left.x = "+Double.toString(left.x)+" sMeanX = "+Double.toString(sMeanX));
            if (left.x > sMeanX) {
                return 0;
            } else {
                for (int i = 0; i < 100; ++i){
                    if ((left.x + lambda*direction.x) > sMeanX) {
                        break;
                    }
                    lambda = ((double) i)/100;
                }
                Log.d(TAG, "lambda: "+Double.toString(lambda));
            }

        }


        return lambda;
    }

    private List<Point> getCorners(List<MatOfPoint> contours) {

        final double angleThreshold = 2*Math.PI/3; // 120 degrees
//        Log.d(TAG,"threshold: "+Double.toString(angleThreshold));
        final List<Point> pPolygonList = contours.get(0).toList();
        List<Point> cornerCandidates = new ArrayList<>();

        Log.d(TAG, "corner candidates");
        for (Point c: pPolygonList) {
            Log.d(TAG, "x:"+Double.toString(c.x)+"y:"+Double.toString(c.y));
        }

        if (pPolygonList.size() > 2) {
            Point p1 = pPolygonList.get(pPolygonList.size()-1);
            Point p2 = pPolygonList.get(0);
            Point p3;

            for (int i = 0; i < pPolygonList.size(); ++i) {
                if (i == pPolygonList.size()-1) {
                    p3 = pPolygonList.get(0);
                } else {
                    p3 = pPolygonList.get(i+1);
                }

                //Calculate angle: compute p1-p2 and p3-p2 to obtain vector to the points p1,p3 from p2.
                //then calculate the dot product and compute the angle with the arccos function.
                //cos(alpha) = dot(v1,v3)/(length(v1)*length(v3))

                final Point v1 = new Point(p1.x-p2.x, p1.y-p2.y);
                final Point v3 = new Point(p3.x-p2.x, p3.y-p2.y);
                final double l1 = Math.sqrt(Math.pow(v1.x,2) + Math.pow(v1.y,2));
                final double l3 = Math.sqrt(Math.pow(v3.x,2) + Math.pow(v3.y,2));

                final double alpha = Math.acos((v1.x*v3.x + v1.y*v3.y)/(l1*l3));

                if (Math.abs(alpha) < angleThreshold) {
                    cornerCandidates.add(p2);
                }

                p1 = p2;
                p2 = p3;
            }
        } else if (pPolygonList.size() == 2){
            cornerCandidates.add(pPolygonList.get(0));
            cornerCandidates.add(pPolygonList.get(1));
        } else {
            cornerCandidates.add(pPolygonList.get(0));
        }

        Log.d(TAG, "corner candidates after angle threshold");
        for (Point c: cornerCandidates) {
            Log.d(TAG, "x:"+Double.toString(c.x)+"y:"+Double.toString(c.y));
        }


        final Point c1 = new Point(0,0);
        final Point c2 = new Point(DIM_IMG_SIZE_X,0);
        final Point c3 = new Point(DIM_IMG_SIZE_X,DIM_IMG_SIZE_Y);
        final Point c4 = new Point(0,DIM_IMG_SIZE_Y);
        final List<Point> imageCorners = new ArrayList<>(Arrays.asList(c1,c2,c3,c4));

        List<Point> corners = new ArrayList<>();

        for (Point c: imageCorners) {
            boolean first = true;
            double distOld = DIM_IMG_SIZE_X+DIM_IMG_SIZE_Y;
            for (Point cornerCandidate: cornerCandidates) {
                double distNew = Math.sqrt(Math.pow(c.x-cornerCandidate.x,2) + Math.pow(c.y-cornerCandidate.y,2));
                if (first) {
                    corners.add(cornerCandidate);
                    distOld = distNew;
                    first = false;
                } else if (distNew < distOld) {
                    corners.remove(corners.size()-1);
                    corners.add(cornerCandidate);
                    distOld = distNew;
                }
            }
        }


        Log.d(TAG, "corners found");
        for (Point c: corners) {
            Log.d(TAG, "x:"+Double.toString(c.x)+"y:"+Double.toString(c.y));
        }

        return corners;
    }

    private List<MatOfPoint> findPolygonalRegions(Mat mask, double minArea, int epsilon) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE );

        // Remove small contours
        final double minA = minArea*DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y;
        for (int i = contours.size(); i > 0; --i) {
            if (Imgproc.contourArea(contours.get(i-1)) < minA) {
                contours.remove(contours.get(i-1));
            }
        }

        //sort by area: https://stackoverflow.com/questions/18939856/using-comparator-without-adding-class
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                return (int) (Imgproc.contourArea(c1) - Imgproc.contourArea(c2));
            }
        });


        // Compute the convex hulls
        List<MatOfPoint> hulls = new ArrayList<>(contours.size());
        for (MatOfPoint c: contours) {
            MatOfInt h = new MatOfInt();
            Imgproc.convexHull(c, h);

            MatOfPoint mopOut = new MatOfPoint();
            mopOut.create((int)h.size().height,1, CvType.CV_32SC2);

            for(int i = 0; i < h.size().height ; ++i)
            {
                int index = (int)h.get(i, 0)[0];
                double[] point = new double[] {
                        c.get(index, 0)[0], c.get(index, 0)[1]
                };
                mopOut.put(i, 0, point);
            }
            hulls.add(mopOut);
        }

        // Approximate the hulls with a polygon
        List<MatOfPoint> pApproxPolys = new ArrayList<>();
        for (MatOfPoint c: hulls) {
            MatOfPoint2f pApproxPoly2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(c.toArray()), pApproxPoly2f, epsilon, true);
            MatOfPoint pApproxPoly = new MatOfPoint(pApproxPoly2f.toArray());
            pApproxPolys.add(pApproxPoly);
        }

        return pApproxPolys;
    }

    private Mat bwClean(Mat mask, int size) {
        final Mat kernel = new Mat(new Size(size,size), CV_8UC1, new Scalar(255));
        Mat tmp = new Mat();
        Mat out = new Mat();

        Imgproc.morphologyEx(mask, tmp, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(tmp, out, Imgproc.MORPH_CLOSE, kernel);

        return out;
    }

    private Mat applyThreshold(Mat img, int threshold) {
        Mat mask = new Mat();

        if (threshold < 0) {
            Imgproc.threshold(img, mask, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        } else {
            Imgproc.threshold(img, mask, 0, 255, Imgproc.THRESH_BINARY);
        }

        return mask;
    }

    /** Save Bitmap in seperate folder named FOLDER_NAME shown in gallery */
    private void saveBitmap(final Bitmap bitmap, final String filename, Context mContext) {
        File mediaDir = Helper.getMediaStorageDir(FOLDER_NAME);//mContext.getString(R.string.app_name));

        final File file = new File(mediaDir, filename);
        if (file.exists()) {
            file.delete();
//            Log.d(TAG, "Existing File overwritten");
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e(TAG, "Failed to save the file");
        }
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        Log.d(TAG, "PageSplit output image saved under " + filename);
    }

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Context mContext) throws IOException {
        AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private static int getExifAngle(Uri uri) throws IOException {
        final ExifInterface exif = new ExifInterface(uri.getPath());
        final int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotation = 0;

        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            rotation = 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            rotation =  180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            rotation =  270;
        }

        return rotation;
    }

    /**
     * Closes tflite to release resources.
     */
    public void close() {
        mTflite.close();
        mTflite = null;
    }
}

