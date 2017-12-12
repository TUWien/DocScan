package at.ac.tuwien.caa.docscan.ui;

import android.graphics.Bitmap;
import android.graphics.PointF;
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

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.DkVector;
import at.ac.tuwien.caa.docscan.crop.CropInfo;

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

        mImageView = findViewById(R.id.map_crop_view);
        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
        initCropInfo(cropInfo);

    }

    private void initCropInfo(CropInfo cropInfo) {

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
        return result;

    }


    private Mat cropAndTransform(String fileName, ArrayList<PointF> srcPoints) {

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

    private Size getRectSize(ArrayList<PointF> points) {

        DkVector[] vectors = getVectors(points);

        float height = (float) Math.max(vectors[UP].length(), vectors[DOWN].length());
        float width = (float) Math.max(vectors[RIGHT].length(), vectors[LEFT].length());

        return new Size(width, height);

    }



    private void printPointList(ArrayList<PointF> points, String name) {

        for (PointF point : points)
            Log.d(getClass().getName(), name + " " + point);
    }

    private void scalePoints(ArrayList<PointF> points, int width, int height) {

        float x, y;
        for (PointF point : points) {
            point.x *= width;
            point.y *= height;
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

    private DkVector[] getVectors(ArrayList<PointF> cropPoints) {

        DkVector[] result = new DkVector[4];
        result[UP] = new DkVector(cropPoints.get(0), cropPoints.get(1));
        result[RIGHT] = new DkVector(cropPoints.get(1), cropPoints.get(2));
        result[DOWN] = new DkVector(cropPoints.get(2), cropPoints.get(3));
        result[LEFT] = new DkVector(cropPoints.get(3), cropPoints.get(0));

        return result;

    }

    private void sortPoints(ArrayList<PointF> points) {

        PointF center = getCenter(points);
        int bottomLeftIdx = getBottomLeftIdx(points, center);

        for (int i = 0; i < bottomLeftIdx; i++)
            points.add(points.remove(0));

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
