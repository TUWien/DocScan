package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.cv.DkVector;
import timber.log.Timber;

public class Mapper {

    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;

    /**
     * Applies the cropping to the provided file.
     * Post-Condition: Existing exif data is lost after a successfully operation.
     */
    public static File applyCropping(File file, ArrayList<PointF> points) {
        Mat transformedMat = null;
        try {
            transformedMat = cropAndTransform(file, points);
            if (transformedMat != null) {
                File newFile = replaceImage(file, transformedMat);
                transformedMat.release();
                return newFile;
            }
        } catch (Exception e) {
            Timber.e(e, "replaceWithMappedImage has failed!");
            return null;
        } finally {
            if (transformedMat != null)
                transformedMat.release();
        }
        return null;
    }

    private static File replaceImage(File file, Mat mat) {
        try {
            boolean fileSaved = Imgcodecs.imwrite(file.getAbsolutePath(), mat);
            if (fileSaved) {
                return file;
            } else {
                return null;
            }
        } catch (Exception e) {
            Timber.e(e, "replaceImage has failed!");
        }
        return null;
    }

    private static Mat cropAndTransform(File file, ArrayList<PointF> srcPoints) {

        if (!file.exists())
            return null;

        Mat inputMat = null;

        try {
            inputMat = Imgcodecs.imread(file.getAbsolutePath());

            // Scale the points since they are normed:
            scalePoints(srcPoints, inputMat.width(), inputMat.height());
//        Add an offset to the crop coordinates:
            srcPoints = PageDetector.getParallelPoints(srcPoints, file.getAbsolutePath());

            // Sort the points so that the bottom left corner is on the first index:
            sortPoints(srcPoints);
//        Determine the size of the output image:
            Size size = getRectSize(srcPoints);
            float width = (float) size.width;
            float height = (float) size.height;

//        Get the destination points:
            ArrayList<PointF> destPoints = getDestinationPoints(width, height);

            // Transform the image:
            return warpMat(inputMat, srcPoints, destPoints, Math.round(width), Math.round(height));

        } finally {
            if (inputMat != null)
                inputMat.release();
        }
    }

    private static Mat warpMat(Mat mat, ArrayList<PointF> cropPoints, ArrayList<PointF> destPoints, int width, int height) {

        MatOfPoint2f srcPointsMat = null;
        MatOfPoint2f dstPointsMat = null;
        Mat perspectiveTransform = null;

        try {

            Mat result = new Mat(height, width, mat.type());

            srcPointsMat = convertToOpenCVPoints(cropPoints);
            dstPointsMat = convertToOpenCVPoints(destPoints);
            perspectiveTransform = Imgproc.getPerspectiveTransform(srcPointsMat, dstPointsMat);

            // TODO: handle case where no transform is found
            Imgproc.warpPerspective(mat,
                    result,
                    perspectiveTransform,
                    new Size(result.width(), result.height()),
                    Imgproc.INTER_CUBIC);
//        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB,0);

            return result;
        } finally {
//            result.release();
            if (srcPointsMat != null)
                srcPointsMat.release();
            if (dstPointsMat != null)
                dstPointsMat.release();
            if (perspectiveTransform != null)
                perspectiveTransform.release();
        }

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
    private static ArrayList<PointF> getDestinationPoints(float width, float height) {
        ArrayList<PointF> destPoints = new ArrayList<>();
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
}
