package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import androidx.exifinterface.media.ExifInterface;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bumptech.glide.signature.MediaStoreSignature;
import com.crashlytics.android.Crashlytics;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.crop.CropView;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;

import static at.ac.tuwien.caa.docscan.ui.MapViewActivity.KEY_MAP_VIEW_ACTIVITY_FINISHED;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends AppCompatActivity {

    private CropView mCropView;
    private String mFileName;
//    used to restore previous state - in case the user cancels cropping:
    private ArrayList<PointF> mOriginalPoints;
    private boolean mIsFocused;
//    used to restore previous state - in case the user cancels cropping:
    private int mOriginalOrientation;

    private static final String CLASS_NAME = "CropViewActivity";

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

        Log.d(CLASS_NAME, "onCreate: ");

        setContentView(R.layout.activity_crop_view);

        initToolbar();

        mCropView = findViewById(R.id.crop_view);

        initCropView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                try {
                    PageDetector.savePointsToExif(mFileName, mOriginalPoints, mIsFocused);
                    Helper.saveExifOrientation(new File(mFileName), mOriginalOrientation);
                } catch (IOException e) {
                    Crashlytics.logException(e);
                    Log.d(CLASS_NAME, "onOptionsItemSelected: " + e.toString());
                    e.printStackTrace();
                }
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);

    }

    private void initToolbar() {

        Toolbar mToolbar = findViewById(R.id.main_toolbar);
        mToolbar.setTitle(getString(R.string.crop_view_title));

//        Enable back navigation in action bar:
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);

    }


    @Override
    protected void onResume() {

        super.onResume();

        Log.d(CLASS_NAME, "onResume:");

//        Determine if the MapViewActivity has just closed and the image has been cropped (and
//        mapped). In this case we do not need the CropViewActivity, but we want to get back to its
//        calling Activity (might be CameraActivity or GalleryActivity).
        boolean hasMapViewActivityFinished = getIntent().getBooleanExtra(
                KEY_MAP_VIEW_ACTIVITY_FINISHED, false);
        if (hasMapViewActivityFinished) {
            finish();
        }

    }

    private void initCropView() {

        Log.d(CLASS_NAME, "initCropView: ");

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mFileName = b.getString(getString(R.string.key_crop_view_activity_file_name), null);

            Log.d(CLASS_NAME, "initCropView: mFileName: " + mFileName);

            if (mFileName != null) {

                Log.d(CLASS_NAME, "initCropView: mFileName not null");

                loadBitmap();

                PageDetector.PageFocusResult result = PageDetector.getNormedCropPoints(mFileName);
                ArrayList<PointF> points = result.getPoints();
                mIsFocused = result.isFocused();

                if (points == null)
                    Log.d(CLASS_NAME, "initCropView: points are null");
                mCropView.setPoints(points);
                Log.d(CLASS_NAME, "initCropView:  points set");

                try {
                    mOriginalOrientation = Helper.getExifOrientation(new File(mFileName));
//                Store the original states in case the user cancels cropping:
                    mOriginalPoints = points;

                } catch (IOException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }

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


    public void startMapView(MenuItem item) {

        startMapView();

    }

    public void applyChanges(MenuItem item) {

        ArrayList<PointF> cropPoints = mCropView.getCropPoints();
        try {
            PageDetector.savePointsToExif(mFileName, cropPoints, mIsFocused);
            GalleryActivity.fileCropped();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void startMapView() {

        ArrayList<PointF> cropPoints = mCropView.getCropPoints();

        try {
            PageDetector.savePointsToExif(mFileName, cropPoints, mIsFocused);

            Intent intent = new Intent(getApplicationContext(), MapViewActivity.class);
            intent.putExtra(getString(R.string.key_crop_view_activity_file_name), mFileName);

            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadBitmap() {

//        Load image with Glide:
        File file = new File(mFileName);

        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION));

            GlideApp.with(this)
                    .load(file)
                    .signature(new MediaStoreSignature("", file.lastModified(), orientation))
                    .into(mCropView);

            Log.d(CLASS_NAME, "loadBitmap: bitmap loaded");

        } catch (IOException e) {
            GlideApp.with(this)
                    .load(file)
                    .into(mCropView);

            Log.d(CLASS_NAME, "loadBitmap: could not load bitmap");
        }

    }

    private void rotateCropView() {

        mCropView.rotate90Degrees();
    }


    private void rotateExif(File file) {

        if (Helper.rotateExif(file))
            rotateCropView();

    }

}
