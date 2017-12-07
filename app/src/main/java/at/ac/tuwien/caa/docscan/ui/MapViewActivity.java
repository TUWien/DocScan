package at.ac.tuwien.caa.docscan.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.os.Bundle;
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
        try {

            mFileName = cropInfo.getFileName();
            Bitmap bitmap = BitmapFactory.decodeFile(mFileName);

            ExifInterface exif = new ExifInterface(cropInfo.getFileName());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int angle = Helper.getAngleFromExif(orientation);
            if (angle != -1) {
                //Rotate the image:
                Matrix mtx = new Matrix();
                mtx.setRotate(angle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);
            }

//            mCropView.setBitmapAndPoints(bitmap, cropInfo.getPoints());
            mImageView.setImageBitmap(bitmap);

//            mapTriangle(bitmap, cropInfo.getPoints());
//            mapTest(bitmap);
//            simpleMap(bitmap, cropInfo.getPoints());

            Mat transformedMat = cropAndTransformBitmap(bitmap, cropInfo.getPoints());
            Bitmap resultBitmap = matToBitmap(transformedMat);

            mImageView.setImageBitmap(resultBitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap matToBitmap(Mat transformedMat) {
        String fileName = Helper.getMediaStorageUserSubDir(getResources().getString(R.string.app_name)).toString() + "/test4.png";
        Imgcodecs.imwrite(fileName, transformedMat);
        return BitmapFactory.decodeFile(fileName);
    }


    // TODO: error handling
    private Mat cropAndTransformBitmap(Bitmap bitmap, ArrayList<PointF> cropPoints) {

        PointF center = getCenter(cropPoints);

        int bottomLeftIdx = getBottomLeftIdx(cropPoints, center);
        Log.d(getClass().getName(), "center point: " + center);
        Log.d(getClass().getName(), "bottom left point: " + cropPoints.get(bottomLeftIdx));

        DkVector[] vectors = getVectors(cropPoints, bottomLeftIdx);

        float height = (float) Math.max(vectors[UP].length(), vectors[DOWN].length());
        float width = (float) Math.max(vectors[RIGHT].length(), vectors[LEFT].length());


        ArrayList<PointF> srcPoints = sortSrcPoints(cropPoints, bottomLeftIdx);
        ArrayList destPoints = new ArrayList();
        destPoints.add(new PointF(0, height));
        destPoints.add(new PointF(0, 0));
        destPoints.add(new PointF(width, 0));
        destPoints.add(new PointF(width, height));

        return warpBitmap(bitmap, srcPoints, destPoints, Math.round(width), Math.round(height));

    }

    private DkVector[] getVectors(ArrayList<PointF> cropPoints, int bottomLeftIdx) {

        DkVector[] result = new DkVector[4];

        result[UP] = new DkVector(
                cropPoints.get(bottomLeftIdx),
                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()));
        result[RIGHT] = new DkVector(
                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()));
        result[DOWN] = new DkVector(
                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()));
        result[LEFT] = new DkVector(
                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 4) % cropPoints.size()));

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
            Log.d(getClass().getName(),"point: " + point);
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
