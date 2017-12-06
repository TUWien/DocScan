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
import org.opencv.video.Video;

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

    private DkVector[] mDirVecs;

    private static final int LEFT = 0;
    private static final int BOTTOM = 1;
    private static final int RIGHT = 2;
    private static final int UP = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_crop_view);

        super.initToolbarTitle(R.string.map_crop_view_title);

        Log.d(getClass().getName(), "mapview");

        initDirVecs();


        mImageView = (ImageView) findViewById(R.id.map_crop_view);
        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
        initCropInfo(cropInfo);



//        initCropInfo(cropInfo);

//        ImageButton button = (ImageButton) findViewById(R.id.confirm_crop_view_button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startMapView();
//            }
////        });

    }

    private void initDirVecs() {

        mDirVecs = new DkVector[4];
        mDirVecs[LEFT] =    new DkVector(new PointF(1,1), new PointF(0,1));
        mDirVecs[BOTTOM] =  new DkVector(new PointF(1,0), new PointF(1,1));
        mDirVecs[RIGHT] =   new DkVector(new PointF(0,0), new PointF(1,0));
        mDirVecs[UP] =      new DkVector(new PointF(0,1), new PointF(0,0));

//        mDirVecs = new DkVector[4];
//        mDirVecs[LEFT] =    new DkVector(new PointF(0,0), new PointF(1,0));
//        mDirVecs[BOTTOM] =  new DkVector(new PointF(1,0), new PointF(1,1));
//        mDirVecs[RIGHT] =   new DkVector(new PointF(1,1), new PointF(0,1));
//        mDirVecs[UP] =      new DkVector(new PointF(0,1), new PointF(0,0));

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

            middlePointMap(bitmap, cropInfo.getPoints());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void middlePointMap(Bitmap bitmap, ArrayList<PointF> cropPoints) {

        PointF center = getMassCenter(cropPoints);

        int bottomLeftIdx = getBottomLeftIdx(cropPoints, center);
        Log.d(getClass().getName(), "center point: " + center);
        Log.d(getClass().getName(), "bottom left point: " + cropPoints.get(bottomLeftIdx));

        DkVector vector1 = new DkVector(
                cropPoints.get(bottomLeftIdx),
                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()));
        DkVector vector2 = new DkVector(
                cropPoints.get((bottomLeftIdx + 1) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()));

//        int height = (int) Math.round(vector1.length());
//        int width = (int) Math.round(vector2.length());

        DkVector vector3 = new DkVector(
                cropPoints.get((bottomLeftIdx + 2) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()));
        DkVector vector4 = new DkVector(
                cropPoints.get((bottomLeftIdx + 3) % cropPoints.size()),
                cropPoints.get((bottomLeftIdx + 4) % cropPoints.size()));

        int height = (int) Math.round(Math.max(vector1.length(), vector3.length()));
        int width = (int) Math.round(Math.max(vector2.length(), vector4.length()));

//        int height = (int)Math.round((vector1.length() + vector3.length())/2);
//        int width = (int) Math.round((vector2.length() + vector4.length())/2);
        // TODO: check if cast is necessary:
        double ratio = height / (double) width;
        Log.d(getClass().getName(), "ratio: " + ratio);

        int bestAngleIdx = getBestAngleIdx(cropPoints);
        Size size = getBestSize(bestAngleIdx, cropPoints, center);

        ArrayList srcPoints = sortSrcPoints(cropPoints, bottomLeftIdx);


        ArrayList destPoints = new ArrayList();
        destPoints.add(new PointF(0, (float) Math.max(height, height * vector3.length() / vector1.length())));
        destPoints.add(new PointF(0, 0));
        destPoints.add(new PointF((float) Math.max(width, width * vector4.length() / vector2.length()), 0));
        destPoints.add(new PointF((float) Math.max(width, width * vector2.length() / vector4.length()),
                (float) Math.max(height, height * vector1.length() / vector3.length())));
        mapBitmap(bitmap, srcPoints, destPoints, width, height);


//        ArrayList destPoints = new ArrayList();
//        destPoints.add(new PointF(0, (float) size.height));
//        destPoints.add(new PointF(0, 0));
//        destPoints.add(new PointF((float) size.width, 0));
//        destPoints.add(new PointF((float) size.width, (float) size.height));
//        mapBitmap(bitmap, srcPoints, destPoints, (int) Math.round(size.width), (int) Math.round(size.height));

//        nearly working:

//        int height = (int)Math.round((vector1.length() + vector3.length())/2);
//        int width = (int) Math.round((vector2.length() + vector4.length())/2);
//        ArrayList destPoints = new ArrayList();
//        destPoints.add(new PointF(0, height));
//        destPoints.add(new PointF(0, 0));
//        destPoints.add(new PointF(width, 0));
//        destPoints.add(new PointF((float) (width * vector4.length() / vector2.length()),
//                (float) (height * vector3.length() / vector1.length())));
//        mapBitmap(bitmap, srcPoints, destPoints, width, height);

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

    private void mapBitmap(Bitmap bitmap, ArrayList<PointF> cropPoints, ArrayList<PointF> destPoints, int width, int height) {


        for (int i = 0; i < cropPoints.size(); i++) {
            Log.d(getClass().getName(), "cropPoint: " + cropPoints.get(i));
            Log.d(getClass().getName(), "destPoints: " + destPoints.get(i));
        }


//        cropPoints.remove(3);
//        destPoints.remove(3);
//        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(cropPoints);
//        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(destPoints);
//        Mat h = Imgproc.getAffineTransform(srcPointsMat, dstPointsMat);


        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(cropPoints);
        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(destPoints);
        Mat h = Video.estimateRigidTransform(srcPointsMat, dstPointsMat, false);


        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat mat = new Mat();
        Utils.bitmapToMat(bmp32, mat);
        Mat result = new Mat(height, width, mat.type());

        Imgproc.warpAffine(mat, result, h, result.size());

        Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGB,0);

        String fileName = Helper.getMediaStorageUserSubDir(getResources().getString(R.string.app_name)).toString() + "/test4.png";
        Imgcodecs.imwrite(fileName, result);

        Log.d(getClass().getName(), "saved img to: " + fileName);



        Bitmap resultBitmap = BitmapFactory.decodeFile(fileName);
//            mCropView.setBitmapAndPoints(bitmap, cropInfo.getPoints());
        mImageView.setImageBitmap(resultBitmap);

    }

    private org.opencv.core.Size getBestSize(int bestAngleIdx, ArrayList<PointF> points, PointF center) {

        PointF vec = new PointF();
        vec.x = points.get(bestAngleIdx).x - center.x;
        vec.y = points.get(bestAngleIdx).y - center.y;

        DkVector v1 = new DkVector(getModPoint(points, bestAngleIdx-1), points.get(bestAngleIdx));
        DkVector v2 = new DkVector(points.get(bestAngleIdx), getModPoint(points, bestAngleIdx+1));

        double width, height;

        if ((vec.x < 0 && vec.y > 0) || (vec.x > 0 && vec.y <0)) {
            width = v1.length();
            height = v2.length();
        }
        else {
            width = v2.length();
            height = v1.length();
        }

        return new org.opencv.core.Size(width, height);


    }

    private int getBestAngleIdx(ArrayList<PointF> points) {

        double maxAngle = 0;
        int maxAngleIdx = -1;
        for (int i = 0; i < points.size(); i++) {
            DkVector v1 = new DkVector(getModPoint(points, i-1), points.get(i));
            DkVector v2 = new DkVector(points.get(i), getModPoint(points, i+1));

            double angle = v1.cosv(v2);
            if (angle > maxAngle) {
                maxAngle = angle;
                maxAngleIdx = i;
            }
            Log.d(getClass().getName(), "point: " + points.get(i) + " cos: " + angle);

        }

        int bestAngleIdx = (maxAngleIdx + 2) % points.size();

        return bestAngleIdx;

    }

    private PointF getModPoint(ArrayList<PointF> points, int idx) {

        return points.get(mod((idx), points.size()));

    }

    private int mod(int x, int y)
    {
        int result = x % y;
        if (result < 0)
            result += y;
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

    private PointF getMassCenter(ArrayList<PointF> points) {

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

    private void simpleMap(Bitmap bitmap, ArrayList<PointF> cropPoints) {

//        int largestSideIdx = getLargestSideIdx(cropPoints, -1);
//        DkVector longSide = new DkVector(
//                cropPoints.get(largestSideIdx), cropPoints.get((largestSideIdx+1) % cropPoints.size()));
//
//        int height = (int) Math.round(longSide.length());
//
//        int shortSideIdx = (largestSideIdx + 1) % cropPoints.size();
//        DkVector shortSide = new DkVector(
//                cropPoints.get(shortSideIdx), cropPoints.get((shortSideIdx+1) % cropPoints.size()));

//        int width = (int) Math.round(shortSide.length());

        DkVector vector1 = new DkVector(cropPoints.get(0), cropPoints.get(1));
        DkVector vector2 = new DkVector(cropPoints.get(1), cropPoints.get(2));
        int direction = getDirection(vector1);

        int width, height;
        if (direction == UP || direction == BOTTOM) {
            height = (int) Math.round(vector1.length());
            width =  (int) Math.round(vector2.length());
        }
        else {
            height = (int) Math.round(vector2.length());
            width =  (int) Math.round(vector1.length());
        }

        Log.d(getClass().getName(), "width: " + width + " height: " + height);
        ArrayList destPoints = null;

        ArrayList pointsUpDir = new ArrayList();
        pointsUpDir.add(new PointF(0, height));
        pointsUpDir.add(new PointF(0, 0));
        pointsUpDir.add(new PointF(width, 0));
        pointsUpDir.add(new PointF(width, height));

        if (direction == UP)
            destPoints = pointsUpDir;
        else {

            int offset = -1;
            switch (direction) {
                case RIGHT:
                    offset = 1;
                    break;
                case BOTTOM:
                    offset = 2;
                    break;
                case LEFT:
                    offset = 3;
            }

            destPoints = new ArrayList();
            for (int i = 0; i < pointsUpDir.size(); i++) {
                int idx = (i + offset) % pointsUpDir.size();
                destPoints.add(pointsUpDir.get(idx));
            }
        }

        ArrayList<PointF> p1 = cropPoints;
        p1.remove(3);
        ArrayList<PointF> p2 = destPoints;
        p2.remove(3);

        p1.remove(3);
        p2.remove(3);

//        getMappingError(p1, p2);

        mapBitmap(bitmap, cropPoints, destPoints, width, height);

    }

//    private void mapTest(Bitmap bitmap) {
//
//
//        ArrayList<PointF> cropPoints = new ArrayList<>();
//        cropPoints.add(new PointF(0, 0));
//        cropPoints.add(new PointF(1000, 0));
//        cropPoints.add(new PointF(0, 1000));
//
//        ArrayList<PointF> destPoints = new ArrayList<>();
//        destPoints.add(new PointF(0, 0));
//        destPoints.add(new PointF(1000, 0));
//        destPoints.add(new PointF(0, 1000));
//
//        mapBitmap(bitmap, cropPoints, destPoints);
//
//
//    }

//    private void getMappingError(ArrayList<PointF> srcPoints, ArrayList<PointF> dstPoints) {
//
//
//        for (int i = 0; i < srcPoints.size(); i++) {
//            Log.d(getClass().getName(), "srcPoints: " + srcPoints.get(i));
//            Log.d(getClass().getName(), "dstPoints: " + dstPoints.get(i));
//        }
//
//
//        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(srcPoints);
//        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(dstPoints);
//
//        Mat h = Imgproc.getAffineTransform(srcPointsMat, dstPointsMat);
//        Mat t = Video.estimateRigidTransform(srcPointsMat, dstPointsMat, false);
//
//        Log.d(getClass().getName(), "t : " + t);
//
//        Mat dest = new Mat();
//        Core.transform(srcPointsMat, dest, h);
//
//        Log.d(getClass().getName(), "srcPointsMat: " + srcPointsMat);
//
//    }



//    private void mapTriangle(Bitmap bitmap, ArrayList<PointF> cropPoints) {
//
//        cropPoints.remove(3);
//
//
//        // The diagonal has the longest length, we skip that side:
//        int diagonalIdx = getLargestSideIdx(cropPoints, -1);
//        // Determine the longest side:
//        int longestSideIdx = getLargestSideIdx(cropPoints, diagonalIdx);
//
//
//        // Create the vectors:
//        DkVector longSide = null;
//        DkVector shortSide = null;
//
//        Log.d(getClass().getName(), "longestSideIdx: " + longestSideIdx);
//        Log.d(getClass().getName(), "diagonalIdx: " + diagonalIdx);
//
//        for (int i = 0; i < cropPoints.size(); i++) {
//
//            if (i == diagonalIdx)
//                continue;
//
//            DkVector vector = new DkVector(
//                    cropPoints.get(i), cropPoints.get((i+1) % cropPoints.size()));
//
//            if (i == longestSideIdx)
//                longSide = vector;
//            else
//                shortSide = vector;
//
//        }
//
//        Log.d(getClass().getName(), "mapping...");
//
//        if (longSide == null)
//            Log.d(getClass().getName(), "longside null");
//
//        if (shortSide == null)
//            Log.d(getClass().getName(), "shortSide null");
//
//        if ((longSide == null) || (shortSide == null))
//            return;
//
//        int longestSideDir = getDirection(longSide);
//
//        int width, height;
//        // The line is horizontal:
//        if ((longestSideDir == UP) || (longestSideDir == BOTTOM)) {
//            width = (int) Math.round(longSide.length());
//            height = (int) Math.round(shortSide.length());
//        }
//        else {
//            height = (int) Math.round(longSide.length());
//            width = (int) Math.round(shortSide.length());
//        }
//
//        int dir = longestSideDir;
//        ArrayList<PointF> srcPoints = new ArrayList<>();
//        ArrayList<PointF> dstPoints = new ArrayList<>();
//
//
//
//        for (int i = 0; i < cropPoints.size(); i++) {
//
//            int sideIdx = (longestSideIdx + i) % cropPoints.size();
//
//            if (sideIdx == diagonalIdx)
//                continue;
//
//            PointF srcP1 = cropPoints.get(sideIdx);
//            PointF srcP2 = cropPoints.get((sideIdx + 1) % cropPoints.size());
//
//            if (!srcPoints.contains(srcP1))
//                srcPoints.add(srcP1);
//            if (!srcPoints.contains(srcP2))
//                srcPoints.add(srcP2);
//
//            DkVector vector = mapVector(dir, width, height);
//            PointF dstP1 = vector.getPoint1();
//            PointF dstP2 = vector.getPoint2();
//
//            if (!dstPoints.contains(dstP1))
//                dstPoints.add(dstP1);
//            if (!dstPoints.contains(dstP2))
//                dstPoints.add(dstP2);
//
//
////            PointF p = cropPoints.get(sideIdx);
////            srcPoints.add(p);
////
////            PointF dstPoint = mapPoint(dir, width, height);
////            dstPoints.add(dstPoint);
//
//            dir++;
//            dir = dir % mDirVecs.length;
//
//        }
//
//
//        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(srcPoints);
//        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(dstPoints);
//
////        Mat h = Calib3d.findHomography(srcPointsMat, dstPointsMat);
//
//
//        for (int i = 0; i < srcPoints.size(); i++) {
//            Log.d(getClass().getName(), "cropPoint: " + srcPoints.get(i));
//            Log.d(getClass().getName(), "destPoints: " + dstPoints.get(i));
//        }
//
//        Mat h = Imgproc.getAffineTransform(srcPointsMat, dstPointsMat);
//        Log.d(getClass().getName(), "h: " + h);
//
//        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        Mat mat = new Mat();
//        Utils.bitmapToMat(bmp32, mat);
//        Mat result = new Mat(height, width, mat.type());
//
//        Log.d(getClass().getName(), "starint warp");
//        Imgproc.warpAffine(mat, result, h, result.size());
//
//        Log.d(getClass().getName(), "warp done");
//        Log.d(getClass().getName(), "mat size: " + result.size());
//
//        String fileName = Helper.getMediaStorageUserSubDir(getResources().getString(R.string.app_name)).toString() + "/test3.png";
//        Imgcodecs.imwrite(fileName, result);
//
//        Log.d(getClass().getName(), "saved img to: " + fileName);
//
//
//    }

    private int getDirection(DkVector vector) {

        double minAngle = 360;
        int minAngleIdx = -1;
        int idx = 0;
        for (int i = 0; i < mDirVecs.length; i++) {

            DkVector dirVec = mDirVecs[i];
            double angle = vector.angle(dirVec);
            if (angle < minAngle)
                minAngleIdx = idx;

            idx++;
        }

        return minAngleIdx;

    }

//    private PointF mapPoint(int direction, int width, int height) {
//
//        switch (direction) {
//            case LEFT:
//                return new PointF(0, 0);
//            case BOTTOM:
//                return new PointF(width, 0);
//            case RIGHT:
//                return new PointF(width, height);
//            case UP:
//                return new PointF(0, height);
//
//            default:
//                return null;
//
//        }
//
//    }
//
//
//    private DkVector mapVector(int direction, int width, int height) {
//
//        switch (direction) {
//            case LEFT:
//                return new DkVector(new PointF(0, 0), new PointF(width, 0));
//            case BOTTOM:
//                return new DkVector(new PointF(width, 0), new PointF(width, height));
//            case RIGHT:
//                return new DkVector(new PointF(width, height), new PointF(0, height));
//            case UP:
//                return new DkVector(new PointF(0, height), new PointF(0, 0));
//            default:
//                return null;
//        }
//
//    }
//
//
//    private void homographyMapping(Bitmap bitmap, ArrayList<PointF> cropPoints) {
//
//
//        // Determine the longest side (reject it, because it is the diagonal)
//        int longestSideIdx = getLargestSideIdx(cropPoints, -1);
//
//        int width = -1;
//        int height = -1;
//        for (int i = 0; i < cropPoints.size(); i++) {
//
//            if (i == longestSideIdx)
//                continue;
//
//            PointF p1 = cropPoints.get(i);
//            PointF p2 = cropPoints.get((i + 1) % cropPoints.size());
//            DkVector v = new DkVector(p1, p2);
//            if (width == -1)
//                width = (int) Math.round(v.length());
//            else if (height == -1)
//                height = (int) Math.round(v.length());
//
//        }
//
//        ArrayList<PointF> dstPoints = new ArrayList<>();
//        dstPoints.add(new PointF(0, 0));
//        dstPoints.add(new PointF(height, 0));
//        dstPoints.add(new PointF(0, width));
//
////        dstPoints.add(new PointF(height, 0));
//        cropPoints.remove(3);
//
////        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(cropPoints);
//
////        ArrayList<PointF> pointsOrdered = new ArrayList<>();
////        pointsOrdered.add(cropPoints.get(1));
////        pointsOrdered.add(cropPoints.get(2));
////        pointsOrdered.add(cropPoints.get(0));
////        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(pointsOrdered);
//
////        ArrayList<PointF> pointsOrdered = (ArrayList<PointF>) cropPoints.clone();
////        PointF p = pointsOrdered.remove(0);
////        pointsOrdered.add(p);
////        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(pointsOrdered);
//
//        ArrayList<PointF> pointsOrdered = (ArrayList<PointF>) cropPoints.clone();
//        MatOfPoint2f srcPointsMat = convertToOpenCVPoints(pointsOrdered);
//
//        MatOfPoint2f dstPointsMat = convertToOpenCVPoints(dstPoints);
//
////        Mat h = Calib3d.findHomography(srcPointsMat, dstPointsMat);
//
//
//        for (int i = 0; i < pointsOrdered.size(); i++) {
//            Log.d(getClass().getName(), "cropPoint: " + pointsOrdered.get(i));
//            Log.d(getClass().getName(), "destPoints: " + dstPoints.get(i));
//        }
//
//        Mat h = Imgproc.getAffineTransform(srcPointsMat, dstPointsMat);
//        Log.d(getClass().getName(), "h: " + h);
//
//        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        Mat mat = new Mat();
//        Utils.bitmapToMat(bmp32, mat);
//        Mat result = new Mat(height, width, mat.type());
//
//        Log.d(getClass().getName(), "starint warp");
//        Imgproc.warpAffine(mat, result, h, result.size());
//
//        Log.d(getClass().getName(), "warp done");
//        Log.d(getClass().getName(), "mat size: " + result.size());
//
//        String fileName = Helper.getMediaStorageUserSubDir(getResources().getString(R.string.app_name)).toString() + "/test2.png";
//        Imgcodecs.imwrite(fileName, result);
//
////        Bitmap b = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
////        Utils.matToBitmap(result, b);
////        mImageView.setImageBitmap(b);
//
//        Log.d(getClass().getName(), "saved img to: " + fileName);
//
//    }
//
//    /**
//     * Determines the affine transformation of the crop points. Note that cropPoints is supposed to
//     * have 3 points and counter-clockwise ordered.
//     * @param bitmap
//     * @param cropPoints
//     */
//    private void mapBitmap(Bitmap bitmap, ArrayList<PointF> cropPoints) {
//
//
//        // Determine the longest side (reject it, because it is the diagonal)
//        int longestSideIdx = getLargestSideIdx(cropPoints, -1);
//        // Determine the second longest side:
////        int secondLongestSideIdx = getLargestSideIdx(cropPoints, longestSideIdx);
//
////        PointF p1 = cropPoints.get(secondLongestSideIdx);
////        PointF p2 = cropPoints.get((secondLongestSideIdx + 1) % cropPoints.size());
//
//        ArrayList<DkVector> dstVecs = new ArrayList<>();
//        dstVecs.add(new DkVector(new PointF(0,1), new PointF(0,0)));
//        dstVecs.add(new DkVector(new PointF(0,0), new PointF(1,0)));
//        dstVecs.add(new DkVector(new PointF(1,0), new PointF(1,1)));
//        dstVecs.add(new DkVector(new PointF(1,1), new PointF(0,1)));
//
////        Calib3d.findHomography()
//
//        PointF p1 = cropPoints.get((longestSideIdx + 1) % cropPoints.size());
//        PointF p2 = cropPoints.get((longestSideIdx + 2) % cropPoints.size());
//        DkVector v1 = new DkVector(p1, p2);
//
//        double minAngle = 360;
//        int minAngleIdx = -1;
//        int idx = 0;
//        for (DkVector v2 : dstVecs) {
//
//            double angle = v1.angle(v2);
//            if (angle < minAngle)
//                minAngleIdx = idx;
//
//            idx++;
//        }
//
//
//        // Check if the longest side is more horizontal or vertical -> dest points are defined
//
//
//
//        MatOfPoint2f srcPoints = convertToOpenCVPoints(cropPoints);
//
//        ArrayList<Point> destPointList = new ArrayList<>();
//        destPointList.add(new Point());
//        destPointList.add(new Point());
//        destPointList.add(new Point());
//
//
//
//
//
////        MatOfPoint2f m = new MatOfPoint2f();
////        m.fromList(cropPoints);
//
//
//
//        // Check if the longest side is more horizontal or vertical -> dest points are defined
//
//        // Get the second side: Project it on the orthogonal of the longest side
//
//        // Determine the output size: we use
////        Imgproc.getAffineTransform()
//
////        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
////        Mat mat = new Mat();
////        Utils.bitmapToMat(bmp32, mat);
//
//    }
//
//    private DkVector getMappedVector(int idx, DkVector vec) {
//
//        /*
//        dstVecs.add(new DkVector(new PointF(0,1), new PointF(0,0)));
//        dstVecs.add(new DkVector(new PointF(0,0), new PointF(1,0)));
//        dstVecs.add(new DkVector(new PointF(1,0), new PointF(1,1)));
//        dstVecs.add(new DkVector(new PointF(1,1), new PointF(0,1)));
//         */
//
//        int length = (int) Math.round(vec.length());
//
//        switch (idx) {
//            case 0:
//                return new DkVector(new PointF(0, length), new PointF(0, 0));
//            case 1:
//                return new DkVector(new PointF(0, 0), new PointF(length, 0));
//        }
//
//        return null;
//
//    }

    private MatOfPoint2f convertToOpenCVPoints(ArrayList<PointF> points) {

        ArrayList<Point> openCVPoints = new ArrayList<>();

        for (PointF point : points) {
            openCVPoints.add(new Point(point.x, point.y));
        }

        MatOfPoint2f result = new MatOfPoint2f();
        result.fromList(openCVPoints);

        return result;

    }

//    private int getLargestSideIdx(ArrayList<PointF> points, int omitSideIdx) {
//
//        double maxDist = 0;
//        int maxSideIdx = -1;
//
//        for (int i = 0; i < points.size(); i++) {
//
//            if (i == omitSideIdx)
//                continue;
//
//            PointF p1 = points.get(i);
//            PointF p2 = points.get((i + 1) % points.size());
//            DkVector v = new DkVector(p1, p2);
//            double dist = v.length();
//
//            if (dist > maxDist) {
//                maxDist = dist;
//                maxSideIdx = i;
//            }
//        }
//
//        return maxSideIdx;
//
//    }
}
