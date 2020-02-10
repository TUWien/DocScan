package at.ac.tuwien.caa.docscan.ui;

import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.exifinterface.media.ExifInterface;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.button.MaterialButton;

//import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.crop.CropView;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity;

import static at.ac.tuwien.caa.docscan.ui.MapViewActivity.KEY_MAP_VIEW_ACTIVITY_FINISHED;
import static at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.KEY_DOCUMENT_NAME;
import static at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.KEY_FILE_NAME;
import static at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.KEY_OPEN_GALLERY;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends AppCompatActivity {

    private static final String KEY_SKIP_CROPPING_INFO_DIALOG = "KEY_SKIP_CROPPING_INFO_DIALOG";

    private CropView mCropView;
    private String mFileName;
    private String mDocumentTitle;
//    used to restore previous state - in case the user cancels cropping:
    private ArrayList<PointF> mOriginalPoints;
    private boolean mIsFocused;
//    used to restore previous state - in case the user cancels cropping:
    private int mOriginalOrientation;
    private float mImageHeightWidthRatio;

    private static final String CLASS_NAME = "CropViewActivity";

//    /**
//     * Static initialization of the OpenCV and docscan-native modules.
//     */
//    static {
//
//        Log.d(CLASS_NAME, "initializing OpenCV");
//
////         We need this for Android 4:
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(CLASS_NAME, "Error while initializing OpenCV.");
//        } else {
//
//            System.loadLibrary("opencv_java3");
//            System.loadLibrary("docscan-native");
//
//            Log.d(CLASS_NAME, "OpenCV initialized");
//        }
//
//    }


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
    public void onBackPressed() {

//        Discard all changes:

        try {
            PageDetector.savePointsToExif(mFileName, mOriginalPoints, mIsFocused);
            Helper.saveExifOrientation(new File(mFileName), mOriginalOrientation);
        } catch (IOException e) {
            Crashlytics.logException(e);
            Log.d(CLASS_NAME, "onOptionsItemSelected: " + e.toString());
            e.printStackTrace();
        }

        super.onBackPressed();
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
        mToolbar.setTitle("");

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
            mDocumentTitle = b.getString(getString(R.string.key_crop_view_activity_document_name), null);

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

        if (Helper.rotateExif(new File(mFileName))) {
            float toAngle = (mCropView.getRotation() + 90) % 360;
            rotateCropView(toAngle, item);
        }

    }

    private void calcImageRatio(File file) throws FileNotFoundException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(file), null, options);

        try {
            int exifOrientation = Helper.getExifOrientation(file);
//            this should not happen:
            if (exifOrientation == -1)
                mImageHeightWidthRatio = options.outHeight / (float) options.outWidth;
            else {
                int exifAngle = Helper.getAngleFromExif(exifOrientation);
                if (exifAngle == 0 || exifAngle == 180)
                    mImageHeightWidthRatio = options.outHeight / (float) options.outWidth;
                else
                    mImageHeightWidthRatio = options.outWidth / (float) options.outHeight;
            }
        } catch (IOException e) {
            mImageHeightWidthRatio = options.outHeight / (float) options.outWidth;
        }
//        Helper.getAngleFromExif()


//        int outHeight = mCropView.getMeasuredHeight();
//        int outWidth = mCropView.getMeasuredWidth();
//        int cropViewWidth = mCropView.getMeasuredWidth();
//
////        Unfortunately the image view height is always the height of the activity. I found no way
////        to adjust the view bounds for the CropView. So we have to reconstruct the 'real' view
////        height based on the image ratio.
//        int cropViewHeight;
//
////            If the original image orientation was in landscape mode we have to flip the ratio:
//        if (outWidth > outHeight) {
//            cropViewHeight = Math.round(cropViewWidth / mImageHeightWidthRatio);
//        }
//        else {
//            cropViewHeight = Math.round(cropViewWidth * mImageHeightWidthRatio);
//        }
//
//        float heightWidthRatio = outHeight / (float) cropViewWidth;
//        float widthHeightRatio = outWidth / (float) cropViewHeight;
////        The scale factor depends on the ratio of the original image:
////            portrait:
//        if (mImageHeightWidthRatio > 1)
//            mScaleFactor = Math.min(heightWidthRatio, widthHeightRatio);
////            landscape
//        else
//            mScaleFactor = Math.max(heightWidthRatio, widthHeightRatio);


    }

//    Returns the scaling factor for the ImageView, depending on the current angle:
    private float getScaleFactor(float angle) {

        if (angle == 0 || angle == 180)
            return 1.f;

        int outHeight = mCropView.getMeasuredHeight();
        int outWidth = mCropView.getMeasuredWidth();
        int cropViewWidth = mCropView.getMeasuredWidth();

//        Unfortunately the image view height is always the height of the activity. I found no way
//        to adjust the view bounds for the CropView. So we have to reconstruct the 'real' view
//        height based on the image ratio.
        int cropViewHeight;

//            If the original image orientation was in landscape mode we have to flip the ratio:
        if (outWidth > outHeight) {
            cropViewHeight = Math.round(cropViewWidth / mImageHeightWidthRatio);
        }
        else {
            cropViewHeight = Math.round(cropViewWidth * mImageHeightWidthRatio);
        }

        float heightWidthRatio = outHeight / (float) cropViewWidth;
        float widthHeightRatio = outWidth / (float) cropViewHeight;

        float scaleFactor = Math.min(heightWidthRatio, widthHeightRatio);
        return scaleFactor;

    }

    private void rotateCropView(float angle, final MenuItem item) {

        float scaleFactor = getScaleFactor(angle);
        mCropView.resizeDimensions(scaleFactor);

        mCropView.animate().rotation(angle).scaleX(scaleFactor).scaleY(scaleFactor).
                setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
                item.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                item.setEnabled(true);
                mCropView.invalidate();
//                mCropView.requestLayout();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
//                item.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }


    public void startMapView(MenuItem item) {

        startMapView();

    }

    public void applyChanges(MenuItem item) {

        ArrayList<PointF> cropPoints = mCropView.getCropPoints();
        try {
            PageDetector.savePointsToExif(mFileName, cropPoints, mIsFocused);

            //        Tell the user that the cropping coordingates have changed:
            if (!skipCroppingInfoDialog())
                showCroppingInfo();
//                showCroppingInfoDialog();
//            The user has skipped the dialog before, close the CropViewActivity:
            else
                finish();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean skipCroppingInfoDialog() {

        final SharedPreferences sharedPref = androidx.preference.PreferenceManager.
                getDefaultSharedPreferences(this);
        boolean skipDialog = sharedPref.getBoolean(KEY_SKIP_CROPPING_INFO_DIALOG, false);

        return skipDialog;

    }

    /**
     * Shows a message about cropping coordinates and that the images are not transformed at this
     * point.
     */
    private void showCroppingInfo() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View layout = adbInflater.inflate(R.layout.crop_info_dialog, null);

        final CheckBox checkBox = layout.findViewById(R.id.skip);
        alertDialog.setView(layout);
        alertDialog.setTitle(R.string.crop_view_crop_dialog_title);
        alertDialog.setMessage(R.string.crop_view_crop_dialog_text);

        MaterialButton openDocumentViewButton = layout.findViewById(R.id.open_document_viewer_button);
        openDocumentViewButton.setOnClickListener(view -> {
            final Intent intent = new Intent(getApplicationContext(), DocumentViewerActivity.class);
//            This is used to prevent cycling between the GalleryActivity and the PageSlideActivity.
//            Without this flag the activities would all be added to the back stack.
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(KEY_OPEN_GALLERY, true);
            intent.putExtra(KEY_DOCUMENT_NAME, mDocumentTitle);
            intent.putExtra(KEY_FILE_NAME, mFileName);
            startActivity(intent);
            finish();

        });

        final SharedPreferences sharedPref = androidx.preference.PreferenceManager.
                getDefaultSharedPreferences(this);

        alertDialog.setPositiveButton(getString(R.string.button_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (checkBox.isChecked()) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putBoolean(KEY_SKIP_CROPPING_INFO_DIALOG, true);
                            editor.commit();
//                            Close the CropViewActivity:
                        }

                        finish();
                    }
                });

        alertDialog.show();

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
        final File file = new File(mFileName);

        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION));

            GlideApp.with(this)
                    .load(file)
                    .signature(new MediaStoreSignature("", file.lastModified(), orientation))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            try {
                                calcImageRatio(file);
                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
                            }
                            return false;
                        }
                    })
                    .into(mCropView);

            Log.d(CLASS_NAME, "loadBitmap: bitmap loaded");

        } catch (IOException e) {
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
