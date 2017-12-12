package at.ac.tuwien.caa.docscan.ui;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.DkVector;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.logic.Helper;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

/**
 * Created by fabian on 24.11.2017.
 */

public class MapViewActivity  extends BaseNoNavigationActivity {

    private ImageView mImageView;
    private String mFileName;

    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_crop_view);

        super.initToolbarTitle(R.string.map_crop_view_title);

        Log.d(getClass().getName(), "mapview");

        mImageView = (ImageView) findViewById(R.id.map_crop_view);
        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
        initCropInfo(cropInfo);

    }

    private void initCropInfo(CropInfo cropInfo) {
        // Unfortunately the exif orientation is not used by BitmapFactory:

        mFileName = cropInfo.getFileName();

        Mat transformedMat = cropAndTransform(mFileName, cropInfo.getPoints());
        if (transformedMat == null)
            showNoTransformationAlert();

        else {
            Bitmap resultBitmap = matToBitmap(transformedMat);
            mImageView.setImageBitmap(resultBitmap);
        }

    }

    private void showNoTransformationAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.map_crop_view_no_transformation_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.map_crop_view_no_transformation_text);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private Bitmap matToBitmap(Mat transformedMat) {

        Bitmap result = Bitmap.createBitmap(transformedMat.cols(), transformedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(transformedMat, result);
//        return BitmapFactory.decodeFile(fileName);
        return result;
    }


    // TODO: error handling
    private Mat cropAndTransformBitmap(Bitmap bitmap, ArrayList<PointF> cropPoints) {

        PointF center = getCenter(cropPoints);

        int bottomLeftIdx = getBottomLeftIdx(cropPoints, center);
        ArrayList<PointF> srcPoints = sortSrcPoints(cropPoints, bottomLeftIdx);

//        DkVector[] vectors = getVectors(cropPoints, bottomLeftIdx);
        DkVector[] vectors = getVectors(cropPoints);
        float height = (float) Math.max(vectors[UP].length(), vectors[DOWN].length());
        float width = (float) Math.max(vectors[RIGHT].length(), vectors[LEFT].length());

        ArrayList destPoints = getDestinationPoints(height, width);

        return warpBitmap(bitmap, srcPoints, destPoints, Math.round(width), Math.round(height));

    }

    // TODO: error handling
    private Mat cropAndTransform(String fileName, ArrayList<PointF> cropPoints) {

        Mat inputMat = Imgcodecs.imread(fileName);

        scalePointList(cropPoints, inputMat.width(), inputMat.height());
//        printPointList(cropPoints, "scaled");

        PointF center = getCenter(cropPoints);

        int bottomLeftIdx = getBottomLeftIdx(cropPoints, center);
        ArrayList<PointF> srcPoints = sortSrcPoints(cropPoints, bottomLeftIdx);

        printPointList(srcPoints, "sorted");

        DkVector[] vectors = getVectors(srcPoints);

        float height = (float) Math.max(vectors[UP].length(), vectors[DOWN].length());
        float width = (float) Math.max(vectors[RIGHT].length(), vectors[LEFT].length());

        Log.d(getClass().getName(), "w : " + width + " h: " + height);

        ArrayList destPoints = getDestinationPoints(width, height);


        printPointList(destPoints, "dest points");
        return warpMat(inputMat, srcPoints, destPoints, Math.round(width), Math.round(height));
//        if (orientation == 0)
//            dstPoints = getDestinationPoints(width, height);
//        else
//            dstPoints = getDestinationPoints(height, width);

//        dstPoints.add(dstPoints.remove(0));
//        dstPoints.add(dstPoints.remove(0));
//        ArrayList dstPoints = getDestinationPoints(height, width, orientation);
//        rotatePoints(dstPoints, dstPoints, orientation);

//        return warpMat(inputMat, srcPoints, dstPoints, Math.round(width), Math.round(height));

    }

    private void printPointList(ArrayList<PointF> points, String name) {

        for (PointF point : points)
            Log.d(getClass().getName(), name + " " + point);
    }

    private void flipCoordinates(ArrayList<PointF> points, int orientation) {

        switch (orientation) {
            case 0:
//                for (PointF point : points) {
//                    float tmp = point.y;
//                    point.y = point.x;
//                    point.x = tmp;
//                }
//                points.add(points.remove(0));
//                points.add(points.remove(0));
//                points.add(points.remove(0));
                break;
        }

    }

    private void scalePointList(ArrayList<PointF> points, int width, int height) {

        float x, y;
        for (PointF point : points) {
            point.x *= width;
            point.y *= height;
        }


    }

    private ArrayList<PointF> scalePoints(ArrayList<PointF> points, int width, int height) {

        ArrayList<PointF> scaledPoints = new ArrayList<>();

        float x, y;
        for (PointF point : points) {
            x = point.x * width;
            y = point.y * height;
            scaledPoints.add(new PointF(x, y));
        }

        return scaledPoints;

    }

    private int getExifOrientation(String fileName) {

        try {
            ExifInterface exif = new ExifInterface(fileName);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int angle = Helper.getAngleFromExif(orientation);
            return angle;
        } catch (IOException e) {
            return -1;
        }

    }

    @NonNull
    private ArrayList getDestinationPoints(float width, float height) {
        ArrayList destPoints = new ArrayList();
        destPoints.add(new PointF(0, height));
        destPoints.add(new PointF(0, 0));
        destPoints.add(new PointF(width, 0));
        destPoints.add(new PointF(width, height));
        return destPoints;
    }

    private void rotatePoints(ArrayList<PointF> points1, ArrayList<PointF> points2, int orientation) {

        switch(orientation) {
            case 0:
                points1.add(points1.remove(0));
//                points2.add(points2.remove(0));
//                for (PointF point : points1) {
//                    Log.d(getClass().getName(), "point: " + point);
//                    float t = point.y;
//                    point.y = point.x;
//                    point.x = t;
//                }


        }

    }

    @NonNull
    private ArrayList getDestinationPoints(float height, float width, int orientation) {

        ArrayList destPoints = new ArrayList();

        Log.d(getClass().getName(), "orientation: " + orientation);

        switch (orientation) {

            case 90:
                destPoints.add(new PointF(0, height));
                destPoints.add(new PointF(0, 0));
                destPoints.add(new PointF(width, 0));
                destPoints.add(new PointF(width, height));
                break;

//                TODO: check what happens if no orientation is found!
            // Handle orientation = 0 (no rotation) or orientation = -1 (no exif orientation found):
            case 0:
                destPoints.add(new PointF(0, height));
                destPoints.add(new PointF(0, 0));
                destPoints.add(new PointF(width, 0));
                destPoints.add(new PointF(width, height));
                break;

        }

        return destPoints;
    }


    private DkVector[] getVectors(ArrayList<PointF> cropPoints) {

        DkVector[] result = new DkVector[4];

//        result[UP] = new DkVector(
//                cropPoints.get(bottomLeftIdx),
//                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()));
//        result[RIGHT] = new DkVector(
//                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()),
//                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()));
//        result[DOWN] = new DkVector(
//                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()),
//                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()));
//        result[LEFT] = new DkVector(
//                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()),
//                cropPoints.get((bottomLeftIdx + 4) % cropPoints.size()));

        result[UP] = new DkVector(cropPoints.get(0), cropPoints.get(1));
        result[RIGHT] = new DkVector(cropPoints.get(1), cropPoints.get(2));
        result[DOWN] = new DkVector(cropPoints.get(2), cropPoints.get(3));
        result[LEFT] = new DkVector(cropPoints.get(3), cropPoints.get(0));

        return result;

    }

    private ArrayList sortSrcPoints(ArrayList<PointF> cropPoints, int bottomLeftIdx) {
        ArrayList srcPoints;
        if (bottomLeftIdx == 0)
            srcPoints = cropPoints;
        else {

            srcPoints = new ArrayList();

            for (int i = 0; i < cropPoints.size(); i++) {
                int idx = (bottomLeftIdx + i) % cropPoints.size();
                srcPoints.add(cropPoints.get(idx));
            }

        }
        return srcPoints;
    }

    private Mat warpBitmap(Bitmap bitmap, ArrayList<PointF> cropPoints, ArrayList<PointF> destPoints, int width, int height) {

        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat mat = new Mat();
        Utils.bitmapToMat(bmp32, mat);
        Mat result = new Mat(height, width, mat.type());

        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(cropPoints);
        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(destPoints);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPointsMat, dstPointsMat);


        if (perspectiveTransform.empty())
            return null;

        Imgproc.warpPerspective(mat,
                result,
                perspectiveTransform,
                new Size(result.width(), result.height()),
                Imgproc.INTER_CUBIC);
//        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB,0);

        return result;

    }

    private Mat warpMat(Mat mat, ArrayList<PointF> cropPoints, ArrayList<PointF> destPoints, int width, int height) {

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
        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB,0);

        return result;

    }



    private int getBottomLeftIdx(ArrayList<PointF> points, PointF center) {

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

    private PointF getCenter(ArrayList<PointF> points) {

        PointF result = new PointF(0, 0);

        for (PointF point : points) {
            result.x += point.x;
            result.y += point.y;
        }

        result.x = result.x / points.size();
        result.y = result.y / points.size();

        return result;

    }
    private MatOfPoint2f convertToOpenCVPoints(ArrayList<PointF> points) {

        ArrayList<Point> openCVPoints = new ArrayList<>();

        for (PointF point : points) {
            openCVPoints.add(new Point(point.x, point.y));
        }

        MatOfPoint2f result = new MatOfPoint2f();
        result.fromList(openCVPoints);

        return result;

    }

}
