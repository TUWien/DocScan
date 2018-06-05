package at.ac.tuwien.caa.docscan.camera.threads;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.cv.DkVector;

public class Mapper {

    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;

    private static final String CLASS_NAME = "Mapper";

    public static void mapImage(String fileName, ArrayList<PointF> points) {

        Mat transformedMat = cropAndTransform(fileName, points);
//        TODO: handle cases where no transformation is found!
//        if (transformedMat == null)
//            showNoTransformationAlert();

        if (transformedMat != null) {

            boolean fileSaved = Imgcodecs.imwrite(fileName, transformedMat);

//            File file = new File(fileName);
//            String name = file.getName();
//            String ext = name.substring(name.lastIndexOf("."));
//
//            String nameOut = name.substring(0, name.lastIndexOf("."))
//                    + "-mapped" + ext;
//
//            File fileOut = new File(file.getParent(), nameOut);
//
//            boolean fileSaved = Imgcodecs.imwrite(fileOut.getAbsolutePath(), transformedMat);

//            TODO: use the fileSaved state to handle errors!



//            Log.d(CLASS_NAME, "mapImage: saved file: " + fileOut.getAbsoluteFile());
//
//            MediaScannerConnection.scanFile(
//                    getApplicationContext(),
//                    new String[]{file.getAbsolutePath()},
//                    null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        @Override
//                        public void onScanCompleted(String path, Uri uri) {
//                            Log.v("grokkingandroid",
//                                    "file " + path + " was scanned seccessfully: " + uri);
//                        }
//                    });
//
//
////            Bitmap resultBitmap = matToBitmap(transformedMat);
////            return resultBitmap;
//
//        }
        }
//        return null;

    }

    private static Bitmap matToBitmap(Mat transformedMat) {

        Bitmap result = Bitmap.createBitmap(transformedMat.cols(), transformedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(transformedMat, result);
        return result;

    }

    private static Mat cropAndTransform(String fileName, ArrayList<PointF> srcPoints) {

        Mat inputMat = Imgcodecs.imread(fileName);

        // Scale the points since they are normed:
        scalePoints(srcPoints, inputMat.width(), inputMat.height());
        // Sort the points so that the bottom left corner is on the first index:
        sortPoints(srcPoints);
        printPointList(srcPoints, "sorted");

//        Determine the size of the output image:
        Size size = getRectSize(srcPoints);
        float width = (float) size.width;
        float height = (float) size.height;

//        Get the destination points:
        ArrayList destPoints = getDestinationPoints(width, height);
        printPointList(destPoints, "dest points");

        // Transform the image:
        return warpMat(inputMat, srcPoints, destPoints, Math.round(width), Math.round(height));

    }

    private static Mat warpMat(Mat mat, ArrayList<PointF> cropPoints, ArrayList<PointF> destPoints, int width, int height) {

        Mat result = new Mat(height, width, mat.type());

        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(cropPoints);
        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(destPoints);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPointsMat, dstPointsMat);

        // TODO: handle case where not transform is found
        Imgproc.warpPerspective(mat,
                result,
                perspectiveTransform,
                new Size(result.width(), result.height()),
                Imgproc.INTER_CUBIC);
//        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB,0);

        return result;

    }

    private static MatOfPoint2f convertToOpenCVPoints(ArrayList<PointF> points) {

        ArrayList<Point> openCVPoints = new ArrayList<>();

        for (PointF point : points) {
            openCVPoints.add(new Point(point.x, point.y));
        }

        MatOfPoint2f result = new MatOfPoint2f();
        result.fromList(openCVPoints);

        return result;

    }

    private static void scalePoints(ArrayList<PointF> points, int width, int height) {

        for (PointF point : points) {
            point.x *= width;
            point.y *= height;
        }

    }

    private static DkVector[] getVectors(ArrayList<PointF> cropPoints) {

        DkVector[] result = new DkVector[4];
        result[UP] = new DkVector(cropPoints.get(0), cropPoints.get(1));
        result[RIGHT] = new DkVector(cropPoints.get(1), cropPoints.get(2));
        result[DOWN] = new DkVector(cropPoints.get(2), cropPoints.get(3));
        result[LEFT] = new DkVector(cropPoints.get(3), cropPoints.get(0));

        return result;

    }

    @NonNull
    private static ArrayList getDestinationPoints(float width, float height) {
        ArrayList destPoints = new ArrayList();
        destPoints.add(new PointF(0, height));
        destPoints.add(new PointF(0, 0));
        destPoints.add(new PointF(width, 0));
        destPoints.add(new PointF(width, height));
        return destPoints;
    }

    private static Size getRectSize(ArrayList<PointF> points) {

        DkVector[] vectors = getVectors(points);

        float height = (float) Math.max(vectors[UP].length(), vectors[DOWN].length());
        float width = (float) Math.max(vectors[RIGHT].length(), vectors[LEFT].length());

        return new Size(width, height);

    }

    private static void sortPoints(ArrayList<PointF> points) {

        PointF center = getCenter(points);
        int bottomLeftIdx = getBottomLeftIdx(points, center);

        for (int i = 0; i < bottomLeftIdx; i++)
            points.add(points.remove(0));

    }

    private static PointF getCenter(ArrayList<PointF> points) {

        PointF result = new PointF(0, 0);

        for (PointF point : points) {
            result.x += point.x;
            result.y += point.y;
        }

        result.x = result.x / points.size();
        result.y = result.y / points.size();

        return result;

    }


    private static int getBottomLeftIdx(ArrayList<PointF> points, PointF center) {

        int idx = 0;

        for (PointF point : points) {

            PointF vec = new PointF();
            vec.x = point.x - center.x;
            vec.y = point.y - center.y;

            if (vec.x < 0 && vec.y > 0)
                return idx;

            idx++;

        }

        return -1;

    }

    private static void printPointList(ArrayList<PointF> points, String name) {

        for (PointF point : points)
            Log.d(CLASS_NAME, name + " " + point);
    }

}
