package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bumptech.glide.signature.MediaStoreSignature;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.threads.Cropper;
import at.ac.tuwien.caa.docscan.crop.CropView;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends BaseNoNavigationActivity {


    private CropView mCropView;
    private String mFileName;

    private static final String CLASS_NAME = "CropViewActivity";


//    TODO: remove this:
    /**
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

        Log.d(CLASS_NAME, "initializing OpenCV");

//         We need this for Android 4:
        if (!OpenCVLoader.initDebug()) {
            Log.d(CLASS_NAME, "Error while initializing OpenCV.");
        } else {

            System.loadLibrary("opencv_java3");
            System.loadLibrary("docscan-native");

            Log.d(CLASS_NAME, "OpenCV initialized");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_view);

        super.initToolbarTitle(R.string.crop_view_title);

//        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
//        mCropView = findViewById(R.id.crop_view);
//        initCropInfo(cropInfo);

        mCropView = findViewById(R.id.crop_view);

        initCropView();
    }

    private void initCropView() {

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mFileName = b.getString(getString(R.string.key_crop_view_activity_file_name), null);

            if (mFileName != null) {
                loadBitmap();
                ArrayList<PointF> points = Cropper.getNormedCropPoints(mFileName);
                mCropView.setPoints(points);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.crop_menu, menu);

        return true;

    }


    public void rotateCropView(MenuItem item) {

        rotateExif(new File(mFileName));


    }


    public void applyChanges(MenuItem item) {

        startMapView();

    }

    private void startMapView() {

        ArrayList<PointF> cropPoints = mCropView.getCropPoints();

        try {
            Cropper.savePointsToExif(mFileName, cropPoints);

            Intent intent = new Intent(getApplicationContext(), MapViewActivity.class);
            intent.putExtra(getString(R.string.key_crop_view_activity_file_name), mFileName);

            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    private void initCropInfo(CropInfo cropInfo) {

    private void loadBitmap() {

//        Load image with Glide:
//        mFileName = cropInfo.getFileName();
        File file = new File(mFileName);

        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION));

            GlideApp.with(this)
                    .load(file)
                    .signature(new MediaStoreSignature("", file != null ? file.lastModified() : 0L, orientation))
//                    .listener(imgLoadListener)
                    .into(mCropView);

        } catch (IOException e) {
            GlideApp.with(this)
                    .load(file)
//                    .listener(imgLoadListener)
                    .into(mCropView);
        }

    }

    private void rotateCropView() {

        mCropView.rotate90Degrees();
    }

//    private RequestListener imgLoadListener = new RequestListener() {
//
//        @Override
//        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
//            return false;
//        }
//
//        @Override
//        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
//
//            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
//            Mat m = new Mat();
//            Utils.bitmapToMat(bitmap, m);
//
//
//            Mat mg = new Mat();
//            Imgproc.cvtColor(m, mg, Imgproc.COLOR_RGBA2RGB);
//
////            TODO: put this into AsyncTask:
//            DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);
//
//            if (polyRects.length > 0 && polyRects[0] != null) {
//                ArrayList<PointF> cropPoints = normPoints(polyRects[0], bitmap.getWidth(), bitmap.getHeight());
//                mCropView.setPoints(cropPoints);
//            }
//            else {
//                mCropView.setDefaultPoints();
//            }
//
//            return false;
//        }
//
//    };

    private ArrayList<PointF> normPoints(DkPolyRect rect, int width, int height) {

        ArrayList<PointF> normedPoints = new ArrayList<>();

        for (PointF point : rect.getPoints()) {
            PointF normedPoint = new PointF();
            normedPoint.x = point.x / width;
            normedPoint.y = point.y / height;
            normedPoints.add(normedPoint);
        }

        return normedPoints;

    }

    private void rotateExif(File file) {

        try {
            if (Helper.rotateExif(file))
                rotateCropView();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    private void rotateExif(File outFile) throws IOException {
//
//        final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
//        if (exif != null) {
//
//            // Save the orientation of the image:
////            int orientation = getExifOrientation();
//            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
//            String newOrientation = null;
//            switch (orientation) {
//                case "1":
//                    newOrientation = "6"; // 90 degrees
//                    break;
//                case "6":
//                    newOrientation = "3"; // 180 degrees
//                    break;
//                case "3":
//                    newOrientation = "8"; // 270 degrees
//                    break;
//                case "8":
//                    newOrientation = "1"; // 0 degrees
//                    break;
//                default:
//            }
//
//            if (newOrientation != null)
//                exif.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation);
//
//            exif.saveAttributes();
//
//            rotateCropView();
//
//        }
//    }

}
