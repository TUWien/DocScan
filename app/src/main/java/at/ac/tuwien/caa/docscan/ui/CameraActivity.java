/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   21. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/
/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   21. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.signature.MediaStoreSignature;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;
import com.google.zxing.Result;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.CameraPaintLayout;
import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.DebugViewFragment;
import at.ac.tuwien.caa.docscan.camera.GPS;
import at.ac.tuwien.caa.docscan.camera.LocationHandler;
import at.ac.tuwien.caa.docscan.camera.cv.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.PaintView;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.cv.thread.preview.IPManager;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentMigrator;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.document.SelectDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.ui.syncui.UploadActivity;
import me.drakeet.support.toast.ToastCompat;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.SHOT_TIME;
import static at.ac.tuwien.caa.docscan.logic.Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY;
import static at.ac.tuwien.caa.docscan.logic.Settings.SettingEnum.SERIES_MODE_PAUSED_KEY;
import static at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity.DOCUMENT_QR_TEXT;

/**
 * The main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */

public class CameraActivity extends BaseNavigationActivity implements TaskTimer.TimerCallbacks,
        CameraPreview.CVCallback, CameraPreview.CameraPreviewCallback, CVResult.CVResultCallback,
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {

    private static final String CLASS_NAME = "CameraActivity";
    private static final String FLASH_MODE_KEY = "flashMode"; // used for saving the current flash status
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
    private static final String KEY_SHOW_EXPOSURE_LOCK_WARNING = "KEY_SHOW_EXPOSURE_LOCK_WARNING";
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 2;
    private static final int CREATE_DOCUMENT_FROM_QR_REQUEST = 0;

    @SuppressWarnings("deprecation")
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mGalleryButton;
    private AppCompatButton mForceShootButton;
    private TaskTimer mTaskTimer;
    private CameraPreview mCameraPreview;
    private PaintView mPaintView;
    private TextView mCounterView;
    private boolean mShowCounter = true;
    private CVResult mCVResult;
    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
    private boolean mIsDebugViewEnabled;
    private static Context mContext;
    private int mCameraOrientation;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsPictureSafe;
    private TextView mTextView;
    private MenuItem mFlashMenuItem, mDocumentMenuItem, mGalleryMenuItem, mUploadMenuItem,
            mWhiteBalanceMenuItem, mLockExposureMenuItem, mUnlockExposureMenuItem;
    private Drawable mFlashOffDrawable, mFlashOnDrawable, mFlashAutoDrawable, mFlashTorchDrawable;
    private boolean mIsSeriesMode = false;
    private boolean mIsSeriesModePaused = true;
    // We hold here a reference to the popupmenu and the list, because we are not sure what is first initialized:
    private List<String> mFlashModes;
    private PopupMenu mFlashPopupMenu, mWhiteBalancePopupMenu;
    private byte[] mPictureData;
//    private Drawable mGalleryButtonDrawable;
    private ProgressBar mProgressBar;
    private final static int SINGLE_POS = 0;
    private final static int SERIES_POS = 1;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private static Date mLastTimeStamp;
    private DkPolyRect[] mLastDkPolyRects;

    private OrientationEventListener mOrientationListener;

    private boolean mIsQRActive = false;


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

    private long mLastTime;
    private boolean mItemSelectedAutomatically = false;
    private int mLastDisplayRotation = - 1;
    private boolean mIsFocusMeasured;


    // ================= start: methods from the Activity lifecycle =================

    /**
     * Creates the camera Activity.
     *
     * @param savedInstanceState saved instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


//        //    just for markus oneplus:
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//            colorStatusBar();


        mContext = this;

        initActivity();



    }

////    just for markus oneplus:
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void colorStatusBar() {
//
//        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
//
//    }


    /**
     * Stops the camera and the paint view thread.
     */
    @Override
    public void onPause() {

        if (mPaintView != null)
            mPaintView.pause();

        if (mCameraPreview != null)
            mCameraPreview.pause();

        if (mOrientationListener != null && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.disable();
        }


        savePreferences();
        DocumentStorage.saveJSON(this);

        mPictureData = null;

        super.onPause();

    }

    /**
     * Stops the camera.
     */
    @Override
    public void onStop() {

        super.onStop();

        mCameraPreview.stop();

    }


    /**
     * Called after the Activity resumes. Resumes the camera and the the paint view thread.
     */
    @Override
    public void onResume() {


        super.onResume();

        Log.d(getClass().getName(), "onResume");

//        Stop receiving orientation change events:
        if (mOrientationListener != null && mOrientationListener.canDetectOrientation())
            mOrientationListener.enable();
        else
            mOrientationListener.disable();


        // Read the sync information:
//        SyncInfo.getInstance().readFromDisk(this);

//        ImageProcessLogger.getInstance().readFromDisk(this);

        ImageProcessor.initContext(this);

        mIsPictureSafe = true;

        // Resume camera access:
        if (mCameraPreview != null)
            mCameraPreview.resume();

        // Resume drawing thread:
        if (mPaintView != null)
            mPaintView.resume();

        // Read user settings:

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        boolean showFocusValues = sharedPref.getBoolean(getResources().getString(R.string.key_show_focus_values), false);
        mPaintView.drawFocusText(showFocusValues);

        boolean showGrid = sharedPref.getBoolean(getResources().getString(R.string.key_show_grid), false);
        mPaintView.drawGrid(showGrid);

        boolean useFastPageDetection = sharedPref.getBoolean(getResources().getString(R.string.key_fast_segmentation), true);
        NativeWrapper.setUseLab(!useFastPageDetection);

        mIsFocusMeasured = sharedPref.getBoolean(getResources().getString(R.string.key_focus_measure), true);
        mCVResult.setMeasureFocus(mIsFocusMeasured);
        IPManager.getInstance().setIsFocusMeasured(mIsFocusMeasured);


        boolean isDebugViewShown = sharedPref.getBoolean(getResources().getString(R.string.key_show_debug_view), false);
        showDebugView(isDebugViewShown);
        // update the title of the toolbar:
//        getSupportActionBar().setTitle(User.getInstance().getDocumentName());

        if (DocumentStorage.getInstance(this).getTitle() != null)
            getSupportActionBar().setTitle(DocumentStorage.getInstance(this).getTitle());

        showControlsLayout(!mIsQRActive);

		checkProviderInstaller();

        updateThumbnail();

        TextView lockExposureTextView = findViewById(R.id.lock_exposure_text_view);
        if (lockExposureTextView != null)
            lockExposureTextView.setVisibility(View.INVISIBLE);

    }

    private void showControlsLayout(boolean showControls) {


        int controlsVisibility, qrCodeVisibility;

        if (showControls) {
            controlsVisibility = View.VISIBLE;
            qrCodeVisibility = View.INVISIBLE;

        }
        else {
            controlsVisibility = View.INVISIBLE;
            qrCodeVisibility = View.VISIBLE;
        }

        // Show/hide the menu buttons:
//        Weak devices might have no flash, so check if mFlashModes is null:
        if ((mFlashModes != null) && (mFlashMenuItem != null))
            mFlashMenuItem.setVisible(showControls);
        if (mDocumentMenuItem != null)
            mDocumentMenuItem.setVisible(showControls);
        if (mGalleryMenuItem != null)
            mGalleryMenuItem.setVisible(showControls);
        if (mUploadMenuItem != null)
            mUploadMenuItem.setVisible(showControls);

//        Show/hide the gallery button:


//        Deleted the overflow button, so I had to comment this out:
//        // Show/hide the overflow button:
//        if (mOptionsMenu != null)
//            mOptionsMenu.setGroupVisible(R.id.overflow_menu_group, showControls);

        RelativeLayout l = findViewById(R.id.controls_layout);
        if (l != null)
            l.setVisibility(controlsVisibility);

        RelativeLayout qrLayout = findViewById(R.id.qr_controls_layout);
        if (qrLayout != null)
            qrLayout.setVisibility(qrCodeVisibility);

    }

    /**
     * Checks if the ProviderInstaller is up-to-date. This is necessary to fix the SSL
     * SSLHandshakeException on Android 4 devices.
     * as it is stated here: https://stackoverflow.com/questions/31269425/how-do-i-tell-the-tls-version-in-android-volley
     * Could not reproduce that this is really necessary, since TLSSocketFactory already did the trick.
     * (Google Play Services installed on testing devices were not too old.)
     */
    private void checkProviderInstaller() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            Crashlytics.logException(e);
            // Indicates that Google Play services is out of date, disabled, etc.
            // Prompt the user to install/update/enable Google Play services.
            GooglePlayServicesUtil.showErrorNotification(
                    e.getConnectionStatusCode(), mContext);

        } catch (GooglePlayServicesNotAvailableException e) {
            Crashlytics.logException(e);
            // Indicates a non-recoverable error; the ProviderInstaller is not able
            // to install an up-to-date Provider.
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save the current flash mode
//        if (mCameraPreview != null)
//            savedInstanceState.putString(FLASH_MODE_KEY, mCameraPreview.getFlashMode());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    // ================= end: methods from the Activity lifecyle =================

    /**
     * Initializes the activity.
     */

    private void initActivity() {

        setContentView(R.layout.activity_camera);

        loadPreferences();

        mCVResult = new CVResult(this);
        IPManager.getInstance().setCVResult(mCVResult);

        mCVResult.setSeriesMode(mIsSeriesMode);

        mCameraPreview = findViewById(R.id.camera_view);

        mPaintView = findViewById(R.id.paint_view);
        if (mPaintView != null)
            mPaintView.setCVResult(mCVResult);

        mCounterView = findViewById(R.id.counter_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeriesPopup(null);
            }
        });
        setSupportActionBar(toolbar);
        setupToolbar();
        setupNavigationDrawer();
        setupDebugView();

        initOrientationListener();

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();


//        initCameraControlLayout();

        initDrawables();
        initPictureCallback();
        initButtons();

        requestLocation();

//        Check app version:
        checkAppVersion();

    }


    private void checkAppVersion() {

        // Load the last version number saved:
        int lastInstalledVersion = Settings.getInstance().loadIntKey(this, Settings.SettingEnum.INSTALLED_VERSION_KEY);
        int currentVersion = BuildConfig.VERSION_CODE;

        // Save the current version:
        Settings.getInstance().saveIntKey(this, Settings.SettingEnum.INSTALLED_VERSION_KEY, currentVersion);

        if (lastInstalledVersion <= 35 && lastInstalledVersion != -1)
            DocumentMigrator.migrate(this);

    }

    private void showDocumentHint() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);


        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.camera_document_hint_title)
                .setPositiveButton("OK", null)
                .setMessage("moving on...");

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();


    }



    private void showLockedExposureDialog() {

        final SharedPreferences sharedPref = android.support.v7.preference.PreferenceManager.
                getDefaultSharedPreferences(this);
        boolean showDialog = sharedPref.getBoolean(KEY_SHOW_EXPOSURE_LOCK_WARNING, true);

        if (!showDialog)
            return;

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View eulaLayout = adbInflater.inflate(R.layout.locked_exposure_dialog, null);

        final CheckBox checkBox = eulaLayout.findViewById(R.id.skip);
        alertDialog.setView(eulaLayout);
        alertDialog.setTitle(R.string.camera_lock_exposure_title);
        alertDialog.setMessage(R.string.camera_lock_exposure_msg);

        alertDialog.setPositiveButton(getString(R.string.button_ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (checkBox.isChecked()) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(KEY_SHOW_EXPOSURE_LOCK_WARNING, false);
                    editor.commit();
                }
            }
        });

        alertDialog.show();

    }

    private void loadPreferences() {


        // Concerning series mode:
        mIsSeriesMode = Settings.getInstance().loadBooleanKey(this, SERIES_MODE_ACTIVE_KEY);
        mIsSeriesModePaused = Settings.getInstance().loadBooleanKey(this, SERIES_MODE_PAUSED_KEY);

//        UserHandler.loadSeriesName(this);

        showShootModeToast();
        updateMode();
        updateShootModeSpinner();

    }

    private void savePreferences() {

//        // Concerning series mode:
//        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//
//        editor.putBoolean(getString(R.string.series_mode_key), mIsSeriesMode);
//        editor.putBoolean(getString(R.string.series_mode_paused_key), mIsSeriesModePaused);
//
//        editor.commit();

        Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY, mIsSeriesMode);
        Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_PAUSED_KEY, mIsSeriesModePaused);

    }

    /**
     * This function accesses the hardware buttons (like volume buttons). We need this access,
     * because shutter remotes emulate such a key press over bluetooth.
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mIsPictureSafe)
                // get an image from the camera
                takePicture();
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
        }

        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_menu, menu);

        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);
        mDocumentMenuItem = menu.findItem(R.id.document_item);
        mGalleryMenuItem = menu.findItem(R.id.gallery_item);
        mUploadMenuItem = menu.findItem(R.id.upload_item);
        mWhiteBalanceMenuItem = menu.findItem(R.id.white_balance_item);
        mLockExposureMenuItem = menu.findItem(R.id.lock_exposure_item);
        mUnlockExposureMenuItem = menu.findItem(R.id.unlock_exposure_item);

        inflater.inflate(R.menu.white_balance_menu, mWhiteBalanceMenuItem.getSubMenu());

        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
        if (mFlashModes != null && !mIsSeriesMode)
            mFlashMenuItem.setVisible(true);

        return true;

    }

    @SuppressWarnings("deprecation")
    private void initDrawables() {

//        We need to use AppCompatResources for drawables from vector files for pre lollipop devices:
        mFlashAutoDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_auto_white_24dp);
        mFlashOffDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_off_white_24dp);
        mFlashOnDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_on_white_24dp);
        mFlashTorchDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_lightbulb_outline_white_24dp);

    }

    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode Request code
     * @param permissions Permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
//        initCamera();
        switch (requestCode) {

            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (isPermissionGiven && mPictureData != null)
                    savePicture(mPictureData);
                break;
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (isPermissionGiven)
                    startLocationAccess();
                break;
        }
    }



    /**
     * Start the CreateDocumentActivity via an intent.
     * @param qrText
     */
    private void startCreateSeriesActivity(String qrText) {

        stopQRMode();

        Intent intent = new Intent(getApplicationContext(), CreateDocumentActivity.class);
        intent.putExtra(DOCUMENT_QR_TEXT, qrText);
        startActivityForResult(intent, CREATE_DOCUMENT_FROM_QR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == CREATE_DOCUMENT_FROM_QR_REQUEST) {
//            Take care that the image processing is resumed, regardless of the intent result:
            resumeFromQRMode();
        }
    }



    // ================= start: methods for opening the gallery =================

    /**
     * Connects the gallery button with its OnClickListener.
     */
    private void initGalleryCallback() {

        mGalleryButton = findViewById(R.id.gallery_button);
        mProgressBar = findViewById(R.id.saving_progressbar);

        mGalleryButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openGallery();
                    }
                });

    }

    /**
     * Opens the MediaScanner (if permission is given) to scan for saved pictures.
     */
    private void openGallery() {

        Document document = DocumentStorage.getInstance(this).getActiveDocument();
        if (document != null && document.getPages() != null && !document.getPages().isEmpty()) {
            Intent intent = new Intent(getApplicationContext(), PageSlideActivity.class);
            intent.putExtra(mContext.getString(R.string.key_document_file_name),
                    document.getTitle());
            intent.putExtra(mContext.getString(R.string.key_page_position),
                    document.getPages().size()-1);
            mContext.startActivity(intent);
        }

    }


    /**
     * Shows a dialog saying that no saved picture has been found.
     */
    private void showNoFileFoundDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_file_found_msg).setTitle(R.string.no_file_found_title);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }


    // ================= end: methods for opening the gallery =================

    // ================= start: methods for accessing the location =================
    @TargetApi(16)
    private void requestLocation() {

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            startLocationAccess();
        }

    }

    private void startLocationAccess() {

//        This can be used to let the user enable GPS:
//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        startActivity(intent);
        LocationHandler.getInstance(this);

    }

    // ================= start: methods for saving pictures =================

    /**
     * Callback called after an image has been taken by the camera.
     */
    @SuppressWarnings("deprecation")
    private void initPictureCallback() {

        mIsPictureSafe = true;

        // Callback for picture saving:
        mPictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                Log.d(CLASS_NAME, "taking picture");

                mTimerCallbacks.onTimerStopped(SHOT_TIME);
                mTimerCallbacks.onTimerStarted(SHOT_TIME);
                mTimerCallbacks.onTimerStopped(FLIP_SHOT_TIME);

                // resume the camera again (this is necessary on the Nexus 5X, but not on the Samsung S5)
                if (mCameraPreview.getCamera() != null) {
                    mCameraPreview.getCamera().startPreview();
                    mCameraPreview.startAutoFocus();
                }



//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                requestPictureSave(data);

                Log.d(CLASS_NAME, "took picture");

            }
        };

    }

    /**
     * Setup a listener for photo shoot button.
     */
    private void initPhotoButton() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        photoButton.setOnClickListener(

                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsSeriesMode) {
                            mIsSeriesModePaused = !mIsSeriesModePaused;
                            if (mIsSeriesMode && mForceShootButton != null)
                                mForceShootButton.setVisibility(View.INVISIBLE);
                            showShootModeToast();
                            updateMode();

//                            // Show the SeriesGeneralActivity just if the user started the series mode and the hide
//                            // dialog setting is not true:
//                            if (mIsSeriesMode && !mIsSeriesModePaused &&  !mHideSeriesDialog)
//                                startDocumentActivity();
                        }
                        else if (mIsPictureSafe) {
                            // get an image from the camera
                            takePicture();
                        }
                    }
                });

    }

    /**
     * Initializes the buttons that are used in the camera_controls_layout. These layouts are
     * recreated on orientation changes, so we need to assign the callbacks again.
     */
    private void initButtons() {

        initPhotoButton();
        initGalleryCallback();
        initShootModeSpinner();
        updatePhotoButtonIcon();
        initCancelQRButton();
        initForceShootButton();

    }



    private void initForceShootButton() {

        mForceShootButton = findViewById(R.id.force_shoot_button);
        mForceShootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

    }

    private void initCancelQRButton() {

        ImageButton button = findViewById(R.id.cancel_qr_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeFromQRMode();
            }
        });

    }

    private void resumeFromQRMode() {
        mIsQRActive = false;
        mCameraPreview.startQrMode(false);
        IPManager.getInstance().setProcessFrame(true);
        IPManager.getInstance().setIsPaused(false);
        showControlsLayout(!mIsQRActive);
    }

    private void initShootModeSpinner() {

        // TODO: define the text and the icons in an enum, to ensure that they have the same order.
        // Spinner for shoot mode:
        Spinner shootModeSpinner = findViewById(R.id.shoot_mode_spinner);
        String[] shootModeText = getResources().getStringArray(R.array.shoot_mode_array);
        Integer[] shootModeIcons = new Integer[]{R.drawable.ic_photo_vector, R.drawable.ic_burst_mode_vector};
        shootModeSpinner.setAdapter(new ShootModeAdapter(this, R.layout.spinner_row, shootModeText, shootModeIcons));
        shootModeSpinner.setOnItemSelectedListener(this);

//        Used to prevent firing the onItemSelected method:
        mItemSelectedAutomatically = true;
        if (mIsSeriesMode)
            shootModeSpinner.setSelection(SERIES_POS);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if ((position == SERIES_POS) && !mIsSeriesMode) {
            mIsSeriesMode = true;
            mIsSeriesModePaused = false;
            mCameraPreview.startAutoFocus();
        }
        else if (position != SERIES_POS && mIsSeriesMode) {
            mIsSeriesMode = false;
            mPaintView.drawMovementIndicator(false); // This is necessary to prevent a drawing of the movement indicator
        }

        // Show a toast and update the mode, but just if he selected the spinner manually:
        if (!mItemSelectedAutomatically) {
            showShootModeToast();
            updateMode();

        }

        mItemSelectedAutomatically = false;



    }


    private void showShootModeToast() {

        int msg;
        if (mIsSeriesMode) {
            if (mIsSeriesModePaused)
                msg = R.string.toast_series_paused;
            else
                msg = R.string.toast_series_started;
        }
        else
            msg = R.string.toast_single;

        showToastText(msg);
    }

    private void updateShootModeSpinner() {

        Spinner shootModeSpinner = findViewById(R.id.shoot_mode_spinner);
        if (mIsSeriesMode)
            shootModeSpinner.setSelection(SERIES_POS);
        else
            shootModeSpinner.setSelection(SINGLE_POS);

    }

    private void updatePhotoButtonIcon() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused) {
                drawable = R.drawable.ic_play_arrow_24dp;
//                displaySeriesModePaused(); // shows a text in the text view and removes any CVResults shown.
            }
            else {
                drawable = R.drawable.ic_pause_24dp;
//                setTextViewText(R.string.instruction_series_started);
            }
        }
        else {
//            showToastText(R.string.toast_single);
            drawable = R.drawable.ic_photo_camera;
//            mIsSeriesModePaused = false;
        }

//        if (mCameraPreview != null)
//            mCameraPreview.pauseImageProcessing(mIsSeriesModePaused);

        photoButton.setImageResource(drawable);


    }

    private void updateMode() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused) {
                drawable = R.drawable.ic_play_arrow_24dp;
                displaySeriesModePaused(); // shows a text in the text view and removes any CVResults shown.
                if (mForceShootButton != null)
                    mForceShootButton.setVisibility(View.INVISIBLE);
            }
            else {
                drawable = R.drawable.ic_pause_24dp;
                setTextViewText(R.string.instruction_series_started);
            }
        }
        else {
//            showToastText(R.string.toast_single);
            drawable = R.drawable.ic_photo_camera;
            mIsSeriesModePaused = false;
            if (mForceShootButton != null)
                mForceShootButton.setVisibility(View.INVISIBLE);
        }

//        Hide the flash button in series mode:
        if (mFlashMenuItem != null)
            mFlashMenuItem.setVisible(!mIsSeriesMode);

        if (mCVResult != null)
            mCVResult.setSeriesMode(mIsSeriesMode);

//        if (mCameraPreview != null)
//            mCameraPreview.pauseImageProcessing(mIsSeriesModePaused, mIsFocusMeasured);

        IPManager.getInstance().setIsSeriesMode(mIsSeriesMode);
        IPManager.getInstance().setIsPaused(mIsSeriesModePaused);

//        IPManager.getInstance().setIsPaused(mIsSeriesModePaused);

        photoButton.setImageResource(drawable);

        // TODO: put this into a method used for restoring generic states:
        if (mIsSeriesMode)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        // Show the SeriesGeneralActivity just if the user started the series mode and the hide
//        // dialog setting is not true:
//        if (mIsSeriesMode && !mIsSeriesModePaused && !mItemSelectedAutomatically && !mHideSeriesDialog) {
//            startDocumentActivity();
//        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    /**
     * Tells the camera to take a picture.
     */
    private void takePicture() {

//        if (!mIsPictureSafe)
//            return;

        mIsPictureSafe = false;
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                mPaintView.showFlicker();
            }
        };

        if (mCameraPreview.getCamera() != null) {
            mCameraPreview.getCamera().takePicture(shutterCallback, null, mPictureCallback);
        }


    }

    private void requestPictureSave(byte[] data) {

        // Check if we have the permission to save images:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPictureData = data;
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else
            savePicture(data);



    }

    /**
     * Save the image in an own thread (AsyncTask):
     * @param data image as a byte stream.
     */
    private void savePicture(byte[] data) {

//        commented, because we are restructuring the document setup:
//        Uri uri = getOutputMediaFile(getResources().getString(R.string.app_name));
        FileSaver fileSaver = new FileSaver(data);
        fileSaver.execute();

    }

    private void showSaveErrorDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.picture_save_error_text).setTitle(R.string.picture_save_error_title);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mIsPictureSafe = true;
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private static Uri getFileName(String appName) {

        File mediaStorageDir = Helper.getMediaStorageDir(appName);
        if (mediaStorageDir == null)
            return null;

        // Create a media file name
        mLastTimeStamp = new Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(mLastTimeStamp);
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(mLastTimeStamp);
        String prefix = mediaStorageDir.getPath() + File.separator +
                mContext.getString(R.string.img_prefix) + timeStamp;
        File mediaFile = new File(prefix  + ".jpg");

//        Check if the file is existing:
        if (mediaFile.exists()) {
//            add a number at the end:
            int idx = 2;
            while (mediaFile.exists()) {
                mediaFile = new File(prefix + "_" + idx + ".jpg");
                idx++;
            }
        }

        return Uri.fromFile(mediaFile);

    }

//    /**
//     * Returns the path to the directory in which the images are saved.
//     *
//     * @param appName name of the app, this is used for gathering the directory string.
//     * @return the path where the images are stored.
//     */
//    public static File getMediaStorageDir(String appName) {
//
//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), appName);
//
//        // Create the storage directory if it does not exist
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//
//                return null;
//            }
//        }
//
//        return mediaStorageDir;
//    }


    // ================= end: methods for saving pictures =================


    // ================= start: methods for navigation drawer =================
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    /**
     * Called after configuration changes -> This includes also orientation change. By handling
     * orientation changes by ourselves, we can prevent a restart of the camera, which results in a
     * speedup.
     * @param newConfig new configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        Log.d(CLASS_NAME, "configuration changed");
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        rotateCameraAndLayout();

    }

    /**
     * Initializes an OrientEventListener that is used to detect orientation changes that are not
     * detected by onConfigurationChanged (e.g. landscape to reverse landscape and portrait to reverse portrait).
     */
    private void initOrientationListener() {

        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {

                int displayRotation = getDisplayRotation();

//                Catch the display rotation changes that are not covered by configuration changes
//                (e.g. landscape to reverse landscape and portrait to reverse portrait):
                if ((mLastDisplayRotation == Surface.ROTATION_0 && displayRotation == Surface.ROTATION_180) ||
                        (mLastDisplayRotation == Surface.ROTATION_180 && displayRotation == Surface.ROTATION_0) ||
                        (mLastDisplayRotation == Surface.ROTATION_90 && displayRotation == Surface.ROTATION_270) ||
                        (mLastDisplayRotation == Surface.ROTATION_270 && displayRotation == Surface.ROTATION_90)) {
                    if (mCameraPreview != null)
                        mCameraPreview.displayRotated();
                }

                mLastDisplayRotation = displayRotation;

            }
        };
    }

    private void rotateCameraAndLayout() {

        // Tell the camera that the orientation changed, so it can adapt the preview orientation:
        if (mCameraPreview != null)
            mCameraPreview.displayRotated();

        // Change the layout dynamically: Remove the current camera_controls_layout and add a new
        // one, which is appropriate for the orientation (portrait or landscape xml's).
        ViewGroup appRoot = findViewById(R.id.main_frame_layout);
        if (appRoot == null)
            return;

//        This is for the HUD rotation:
//        View v = findViewById(R.id.main_frame_layout);
//        v.setScaleY(-1.f);

        View f = findViewById(R.id.camera_controls_layout);
        if (f == null)
            return;

        appRoot.removeView(f);

        getLayoutInflater().inflate(R.layout.camera_controls_layout, appRoot);
        View view = findViewById(R.id.camera_controls_layout);
        view.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));

        showControlsLayout(!mIsQRActive);

//        // Initialize the newly created buttons:
        initButtons();
        updateThumbnail();

    }


    public static Point getPreviewDimension() {

//        Taken from: http://stackoverflow.com/questions/1016896/get-screen-dimensions-in-pixels
        View v = ((Activity) mContext).findViewById(R.id.camera_controls_layout);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Point dim = null;

        if (v != null) {
            if (getDisplayRotation() == Surface.ROTATION_0 || getDisplayRotation() == Surface.ROTATION_180)
                dim = new Point(size.x, size.y - v.getHeight());
//                return size.y - v.getHeight();
            else if (getDisplayRotation() == Surface.ROTATION_90 || getDisplayRotation() == Surface.ROTATION_270)
                dim = new Point(size.x - v.getWidth(), size.y);
//                return size.x - v.getWidth();
        }

        return dim;


    }

    private void showDebugView(boolean showDebugView) {


        if (showDebugView && !mIsDebugViewEnabled) {
            // Create the debug view - if it is not already created:
            if (mDebugViewFragment == null) {
                mDebugViewFragment = new DebugViewFragment();
            }

            getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
        }
        else if (!showDebugView && mIsDebugViewEnabled && mDebugViewFragment != null) {

            getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();

        }

        mIsDebugViewEnabled = showDebugView;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {

            case R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

//        Deleted the overflow button, so I had to comment this out:

////            Show / hide the debug view
//            case R.id.debug_view_item:
//
//                // Create the debug view - if it is not already created:
//                if (mDebugViewFragment == null) {
//                    mDebugViewFragment = new DebugViewFragment();
//                }
//
//                // Show the debug view:
//                if (getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT) == null) {
//                    mIsDebugViewEnabled = true;
//                    item.setTitle(R.string.hide_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
//                }
//                // Hide the debug view:
//                else {
//                    mIsDebugViewEnabled = false;
//                    item.setTitle(R.string.show_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
//                }
//
//                return true;
//
//            // Switch between the two page segmentation methods:
//            case R.id.use_lab_item:
//
//                if (NativeWrapper.useLab()) {
//                    NativeWrapper.setUseLab(false);
//                    item.setTitle(R.string.precise_page_seg_text);
//                }
//                else {
//                    NativeWrapper.setUseLab(true);
//                    item.setTitle(R.string.fast_page_seg_text);
//                }
//
//                return true;
//
//            // Focus measurement:
//            case R.id.show_fm_values_item:
//
//                if (mPaintView.isFocusTextVisible()) {
//                    item.setTitle(R.string.show_fm_values_text);
//                    mPaintView.drawFocusText(false);
//                } else {
//                    item.setTitle(R.string.hide_fm_values_text);
//                    mPaintView.drawFocusText(true);
//                }
//
//                break;
//
//            // Guide lines:
//            case R.id.show_guide_item:
//
//                if (mPaintView.areGuideLinesDrawn()) {
//                    mPaintView.drawGuideLines(false);
//                    item.setTitle(R.string.show_guide_text);
//                } else {
//                    mPaintView.drawGuideLines(true);
//                    item.setTitle(R.string.hide_guide_text);
//                }
//
//                break;
//
////            // Threading:
////            case R.id.threading_item:
////                if (mCameraPreview.isMultiThreading()) {
////                    mCameraPreview.setThreading(false);
////                    item.setTitle(R.string.multi_thread_text);
////                }
////                else {
////                    mCameraPreview.setThreading(true);
////                    item.setTitle(R.string.single_thread_text);
////                }
//
//
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }


    }

    private void setupDebugView() {

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);
        mTimerCallbacks = this;
        mTextView = findViewById(R.id.instruction_view);

        mIsDebugViewEnabled = (mDebugViewFragment == null);
        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;

    }

    /**
     * Initializes the navigation drawer, when the app is started.
     */
    @SuppressWarnings("deprecation")
    private void setupNavigationDrawer() {

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        NavigationView mDrawer = findViewById(R.id.left_drawer);
        setupDrawerContent(mDrawer);

        // Set the item text for the debug view in the naviation drawer:
        if (mDrawer == null)
            return;

        Menu menu = mDrawer.getMenu();
        if (menu == null)
            return;

        //        Deleted the overflow button, so I had to comment this out:
//        MenuItem item = menu.findItem(R.id.debug_view_item);
//        if (item == null)
//            return;
//
//        if (mIsDebugViewEnabled)
//            item.setTitle(R.string.hide_debug_view_text);
//        else
//            item.setTitle(R.string.show_debug_view_text);

    }


    /**
     * Connects the items in the navigation drawer with a listener.
     *
     * @param navigationView NavigationView
     */
    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(

                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    /**
     * Called after an item is selected in the navigation drawer.
     *
     * @param menuItem ID of the selected item.
     */
    private void selectDrawerItem(MenuItem menuItem) {

        mDrawerLayout.closeDrawers();

    }

    // ================= end: methods for navigation drawer =================


    // ================= start: CALLBACKS invoking TaskTimer =================

    /**
     * Called before a task is executed. This is used to measure the time of the task execution.
     *
     * @param type TaskType of the sender, as defined in TaskTimer.
     */
    @Override
    public void onTimerStarted(TaskTimer.TaskType type) {

        // Do nothing if the debug view is not visible:
        if (!mIsDebugViewEnabled)
            return;

        if (mTaskTimer == null)
            return;

        mTaskTimer.startTaskTimer(type);


    }

    /**
     * Called after a task is executed. This is used to measure the time of the task execution.
     *
     * @param type
     */
    @Override
    public void onTimerStopped(final TaskTimer.TaskType type) {

        if (!mIsDebugViewEnabled)
            return;

        if (mTaskTimer == null)
            return;

        final long timePast = mTaskTimer.getTaskTime(type);

        // Normally the timer callback should just be called if the debug view is visible:
        if (mIsDebugViewEnabled) {

            if (mDebugViewFragment != null) {
                // The update of the UI elements must be done from the UI thread:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDebugViewFragment.setTimeText(type, timePast);
                    }
                });
            }
        }
    }


//    /**
//     * Returns true if the debug view is visible. Mainly used before TaskTimer events are triggered.
//     *
//     * @return boolean
//     */
//    public static boolean isDebugViewEnabled() {
//
//        return mIsDebugViewEnabled;
//
//    }

    // ================= stop: CALLBACKS invoking TaskTimer =================


    public static int getDisplayRotation() {

        WindowManager w = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);

        return w.getDefaultDisplay().getRotation();

    }


    // ================= start: CALLBACKS called from native files =================

    @Override
    public void onMovement(boolean moved) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }

        // This happens if the user has just switched to single mode and the event occurs later than the touch event.
        if (!mIsSeriesMode) {
            mPaintView.drawMovementIndicator(false);
            return;
        }
        else if (mIsSeriesModePaused) {
            displaySeriesModePaused();
            return;
        }

        mPaintView.drawMovementIndicator(moved);

        if (moved) {
            mCVResult.clearResults();
            setTextViewText(R.string.instruction_movement);
        }
        else {
            // This forces an update of the textview if it is still showing the R.string.instruction_movement text
            if (mTextView.getText() == getResources().getString(R.string.instruction_movement))
                setTextViewText(R.string.instruction_none);
        }

    }

    /**
     * Called after focus measurement is finished.
     *
     * @param patches Patches array
     */
    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }

        if (mCVResult != null && patches != null && patches.length > 0)
            mCVResult.setPatches(patches);

//        CVManager.getInstance().setNextTask(TASK_TYPE_MOVE);

//        if (mIsSeriesMode)
//            CVManager.getInstance().setNextTask(TASK_TYPE_MOVE);
//        else
//            CVManager.getInstance().setNextTask(TASK_TYPE_PAGE);

    }

    /**
     * Called after page segmentation is finished.
     *
     * @param polyRects Array of polyRects
     */
    @Override
    public void onPageSegmented(DkPolyRect[] polyRects) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }
        mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - mLastTime;

        mLastTime = currentTime;

        if (mCVResult != null) {
//            mCVResult.setPatches(null);
//            if (!mCVResult.isStable())
//                mCVResult.setPatches(new Patch[0]);

            mCVResult.setDKPolyRects(polyRects);
        }

//        if (isRectJumping(dkPolyRects))
//            mCameraPreview.startFocusMeasurement(false);
//        else
//            mCameraPreview.startFocusMeasurement(true);

        mLastDkPolyRects = polyRects;

        mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);

    }


    boolean isRectJumping(DkPolyRect[] dkPolyRects){

        boolean isJumping = false;

        Log.d(CLASS_NAME, "jumping?");

        if (dkPolyRects != null && mLastDkPolyRects != null) {
            Log.d(CLASS_NAME, "check 1");
            if (dkPolyRects.length == 1 && mLastDkPolyRects.length == 1) {
                Log.d(CLASS_NAME, "check 2");
                PointF distVec = mLastDkPolyRects[0].getLargestDistVector(dkPolyRects[0]);
                PointF normedPoint = mCVResult.normPoint(distVec);

                if (normedPoint.length() >= .05) {
                    isJumping = true;
                }

                Log.d(CLASS_NAME, "distance: " + normedPoint.length());
            }
        }

        return isJumping;
    }


    // ================= end: CALLBACKS called from native files =================


    // =================  start: CameraPreview.CameraPreviewCallback CALLBACK =================

    /**
     * Called after the dimension of the camera view is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     *
     * @param width  width of the view
     * @param height height of the view
     */
    @Override
    public void onMeasuredDimensionChange(int width, int height) {

        mCVResult.setViewDimensions(width, height);

    }

    @Override
    public void onFlashModesFound(List<String> modes) {

        mFlashModes = modes;

        if (mFlashPopupMenu != null) // Menu is not created yet
            setupFlashUI();

        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
        if (mFlashModes != null && mFlashMenuItem != null)
            mFlashMenuItem.setVisible(true);

    }

    @Override
    public void onExposureLockFound(boolean isSupported) {

        if (mUnlockExposureMenuItem != null && mUnlockExposureMenuItem.isVisible())
            mUnlockExposureMenuItem.setVisible(false);
        if (mLockExposureMenuItem != null) {
            if (isSupported)
                mLockExposureMenuItem.setVisible(true);
            else
                mLockExposureMenuItem.setVisible(false);
        }


    }


    @Override
    public void onWhiteBalanceFound(List<String> whiteBalances) {

//        if (mWhiteBalancePopupMenu == null) // Menu is not created yet
//            return;
//
////        At least one white balance mode found must be handled by the UI:
//        boolean isWBSupported = false;
//        for (String whiteBalance : whiteBalances)
//            isWBSupported |= enableWBItem(whiteBalance);
//
////        Otherwise hide the WB item:
//        if (!isWBSupported)
//            mWhiteBalanceMenuItem.setVisible(false);
    }

//    /**
//     * Returns true if the whiteBalance is handled in the UI.
//     * @param whiteBalance
//     * @return
//     */
//    private boolean enableWBItem(String whiteBalance) {
//
//        switch (whiteBalance) {
//            case Camera.Parameters.WHITE_BALANCE_AUTO:
//                mWhiteBalancePopupMenu.getMenu().findItem(R.id.white_balance_auto_item)
//                        .setVisible(true);
//                return true;
//            case Camera.Parameters.WHITE_BALANCE_DAYLIGHT:
//                mWhiteBalancePopupMenu.getMenu().findItem(R.id.white_balance_sunny_item)
//                        .setVisible(true);
//                return true;
//            default:
//                return false;
//        }
//
//    }

//    private void enableWBButton(final String whiteBalance) {
//
//        AppCompatButton button = null;
//        switch(whiteBalance) {
//            case Camera.Parameters.WHITE_BALANCE_AUTO:
//                button = findViewById(R.id.wb_auto_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_INCANDESCENT:
//                button = findViewById(R.id.wb_incandescent_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_FLUORESCENT:
//                button = findViewById(R.id.wb_iridescent_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_DAYLIGHT:
//                button = findViewById(R.id.wb_sunny_button);
//        }
//
//        if (button != null) {
//            button.setVisibility(View.VISIBLE);
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (mCameraPreview != null)
//                        mCameraPreview.setWhiteBalance(whiteBalance);
//                }
//            });
//        }
//
//    }

//    private void enableWB

    @Override
    public void onFocusTouch(PointF point) {

        Log.d(CLASS_NAME, "onFocusTouch");

        if (mPaintView != null)
            mPaintView.drawFocusTouch(point);

    }

    @Override
    public void onFocusTouchSuccess() {

        if (mPaintView != null)
            mPaintView.drawFocusTouchSuccess();
    }



    @Override
    public void onWaitingForDoc(boolean waiting) {

        if (mIsSeriesModePaused) {
            displaySeriesModePaused();
            return;
        }

        if (waiting)
            setTextViewText(R.string.instruction_no_changes);

    }

    @Override
    public void onCaptureVerified() {

        if (!mIsPictureSafe) {
            Log.d(CLASS_NAME, "onCaptureVerified: not safe to save picture");
            return;
        }


        mTextView.setText(getResources().getString(R.string.taking_picture_text));

        takePicture();


    }

    @Override
    public void onQRCode(Result result) {

        Log.d(CLASS_NAME, "qr code found: " + result);

        final String text;
        // Tell the user that we are still searching for a QR code:
        if (result == null) {
            text = getString(R.string.instruction_searching_qr);
            setInstructionText(text);

        }
        // Start the CreateDocumentActivity:
        else {
            // Stop searching for QR code:
//            stopQRMode();

            text = result.toString();
            startCreateSeriesActivity(text);

        }




    }

    private void stopQRMode() {
        mIsQRActive = false;
        mCameraPreview.startQrMode(false);
        IPManager.getInstance().setIsPaused(false);
    }

    private void setInstructionText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(text);
            }
        });
    }

//    @Override
//    public void onBarCodeFound(final Barcode barcode) {
//
//        final String txt;
//        if (barcode != null)
//            txt = barcode.rawValue;
//        else
//            txt = "";
//
//        //  Tell the user that a barcode is found
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // This code will always run on the UI thread, therefore is safe to modify UI elements.
//                mTextView.setText(txt);
//            }
//        });
//
//    }

    private void setTextViewText(int msg) {

        String msgText = getResources().getString(msg);
        if (mTextView != null)
            mTextView.setText(msgText);

    }

//    public void showSeriesPopup(MenuItem item) {
//
//        View menuItemView = findViewById(R.id.document_item);
//        if (menuItemView == null)
//            return;
//
//        // Create the menu for the first time:
//        if (mSeriesPopupMenu == null) {
//            mSeriesPopupMenu = new PopupMenu(this, menuItemView);
//            mSeriesPopupMenu.setOnMenuItemClickListener(this);
//            mSeriesPopupMenu.inflate(R.menu.series_menu);
//
//            setupFlashUI();
//        }
//
//        mSeriesPopupMenu.show();
//
//    }

    public void showSeriesPopup(MenuItem item) {

        View menuItemView = findViewById(R.id.document_item);
        if (menuItemView == null)
            return;

        PopupMenu seriesPopupMenu = new PopupMenu(this, menuItemView);
        seriesPopupMenu.setOnMenuItemClickListener(this);
        seriesPopupMenu.inflate(R.menu.series_menu);
        seriesPopupMenu.show();

    }

    public void showFlashPopup(MenuItem item) {

        View menuItemView = findViewById(R.id.flash_mode_item);
        if (menuItemView == null)
            return;

        mFlashPopupMenu = new PopupMenu(this, menuItemView);
        mFlashPopupMenu.setOnMenuItemClickListener(this);
        mFlashPopupMenu.inflate(R.menu.flash_mode_menu);

        setupFlashUI();

        mFlashPopupMenu.show();

    }

    public void startUploadActivity(MenuItem item) {

        Document document = DocumentStorage.getInstance(this).getActiveDocument();
        Intent intent = new Intent(getApplicationContext(), UploadActivity.class);

//        If the document contains already images, select it in the UploadActivity:
        if (document != null && document.getPages() != null && !document.getPages().isEmpty())
            intent.putExtra(getString(R.string.key_document_file_name), document.getTitle());

        startActivity(intent);

    }


    public void startGalleryActivity(MenuItem item) {

        String documentTitle = DocumentStorage.getInstance(this).getTitle();
        if (documentTitle != null) {
            if (DocumentStorage.getInstance(this).getDocument(documentTitle) == null)
                return;

            Intent intent = new Intent(mContext, GalleryActivity.class);
            intent.putExtra(mContext.getString(R.string.key_document_file_name), documentTitle);
            mContext.startActivity(intent);
        }

    }

    public void lockExposure(MenuItem item) {

        if (mCameraPreview != null) {

            showLockedExposureDialog();

            mCameraPreview.lockExposure(true);
            mCameraPreview.lockWhiteBalance(true);

            if (mLockExposureMenuItem != null)
                mLockExposureMenuItem.setVisible(false);
            if (mUnlockExposureMenuItem != null)
                mUnlockExposureMenuItem.setVisible(true);

            final TextView lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setAlpha(0f);
            lockTextView.setVisibility(View.VISIBLE);
            lockTextView.animate()
                    .alpha(1f)
                    .setDuration(1000);
        }

    }

    public void unlockExposure(MenuItem item) {

        if (mCameraPreview != null) {
            mCameraPreview.lockExposure(false);
            mCameraPreview.lockWhiteBalance(false);

            if (mLockExposureMenuItem != null)
                mLockExposureMenuItem.setVisible(true);
            if (mUnlockExposureMenuItem != null)
                mUnlockExposureMenuItem.setVisible(false);

            final TextView lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setVisibility(View.INVISIBLE);

            final TextView unlockTextView = findViewById(R.id.unlock_exposure_text_view);
            unlockTextView.setAlpha(1f);
            unlockTextView.setVisibility(View.VISIBLE);
            unlockTextView.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            unlockTextView.setVisibility(View.INVISIBLE);
                        }
                    });

//            final TextView lockTextView = findViewById(R.id.lock_exposure_text_view);
//            lockTextView.setAlpha(0.f);
//            lockTextView.setVisibility(View.VISIBLE);
//            lockTextView.animate()
//                    .alpha(1f)
//                    .setDuration(500);

        }

    }

    public void whiteBalanceAuto(MenuItem item) {

        if (mCameraPreview != null)
            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

    }

    public void whiteBalanceDaylight(MenuItem item) {

        if (mCameraPreview != null)
            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);

    }

    public void whiteBalanceCloudy(MenuItem item) {

        if (mCameraPreview != null)
            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);

    }

    @SuppressWarnings("deprecation")
    private void setupFlashUI() {


        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO))
            mFlashPopupMenu.getMenu().findItem(R.id.flash_auto_item).setVisible(false);

        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_ON))
            mFlashPopupMenu.getMenu().findItem(R.id.flash_on_item).setVisible(false);

        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
            mFlashPopupMenu.getMenu().findItem(R.id.flash_off_item).setVisible(false);

        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH))
            mFlashPopupMenu.getMenu().findItem(R.id.flash_torch_item).setVisible(false);

    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.flash_auto_item:
                mFlashMenuItem.setIcon(mFlashAutoDrawable);
                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                return true;
            case R.id.flash_off_item:
                mFlashMenuItem.setIcon(mFlashOffDrawable);
                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
                return true;
            case R.id.flash_on_item:
                mFlashMenuItem.setIcon(mFlashOnDrawable);
                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
//                mFlashMode = Camera.Parameters.FLASH_MODE_ON;
                return true;
            case R.id.flash_torch_item:
                mFlashMenuItem.setIcon(mFlashTorchDrawable);
                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                mFlashMode = Camera.Parameters.FLASH_MODE_ON;
                return true;

            case R.id.series_new_item:
                startActivity(new Intent(getApplicationContext(), CreateDocumentActivity.class));
//                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;

            case R.id.series_edit_item:
                String documentName = DocumentStorage.getInstance(this).getTitle();
                if (documentName != null) {
                    Intent intent = new Intent(getApplicationContext(), EditDocumentActivity.class);
                    intent.putExtra(EditDocumentActivity.DOCUMENT_NAME_KEY, documentName);
                    startActivity(intent);
                }
//                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;

            case R.id.series_switch_item:
//                startActivity(new Intent(getApplicationContext(), OpenDocumentActivity.class));
                startActivity(new Intent(getApplicationContext(), SelectDocumentActivity.class));
                return true;

            case R.id.series_qr_item:
                mIsQRActive = true;
                showControlsLayout(false);
                mCVResult.clearResults();
                mCameraPreview.startQrMode(true);
//                String text = getString(R.string.instruction_searching_qr);
//                setInstructionText(text);

                IPManager.getInstance().setIsPaused(true);
                return true;

            default:
                return false;

        }
    }


    /**
     * Called after the dimension of the camera frame is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     *
     * @param width             width of the frame
     * @param height            height of the frame
     * @param cameraOrientation orientation of the camera
     */
    @Override
    public void onFrameDimensionChange(int width, int height, int cameraOrientation) {

        mCameraOrientation = cameraOrientation;
        mCVResult.setFrameDimensions(width, height, cameraOrientation);

        CameraPaintLayout l = findViewById(R.id.camera_paint_layout);
        if (l != null)
            l.setFrameDimensions(width, height);

        if (mCameraPreview == null)
            return;

        View v = findViewById(R.id.camera_controls_layout);
        if ((v != null) && (!mCameraPreview.isPreviewFitting()))
            v.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));

////        Make the actionbar opaque in case the preview does not fit the entire screen
////        Note: this is especially for markus oneplus not tested on other devices yet:
//        if (mCameraPreview.isPreviewFitting() &&
//                (getDisplayRotation() == Surface.ROTATION_0 || getDisplayRotation() == Surface.ROTATION_180)) {
//
//            int actionBarHeight;
//
//            final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
//                    new int[]{android.R.attr.actionBarSize}
//            );
//            actionBarHeight = (int) styledAttributes.getDimension(0, 0);
//            styledAttributes.recycle();
//
//            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) l.getLayoutParams();
//            params.setMargins(0, actionBarHeight, 0, 0);
//        }
//        else {
//            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) l.getLayoutParams();
//            params.setMargins(0, 0, 0, 0);
//        }

    }

    /**
     * Called after the the status of the CVResult object is changed.
     * @param state state of the CVResult
     */
    @Override
    public void onStatusChange(final int state) {

        mTextView.setText(getInstructionMessage(state));
        updateForceShootButton(state);

    }

    private void updateForceShootButton(int state) {
        //        Show or hide the force shoot button:
        if (mIsSeriesMode) {
            if (mForceShootButton != null) {
                if (state == CVResult.DOCUMENT_STATE_UNSHARP ||
                        state == CVResult.DOCUMENT_STATE_ROTATION ||
                        state == CVResult.DOCUMENT_STATE_PERSPECTIVE)
                    mForceShootButton.setVisibility(View.VISIBLE);
                else
                    mForceShootButton.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void displaySeriesModePaused() {
        if (mCVResult != null)
            mCVResult.clearResults();

        if (mPaintView != null)
            mPaintView.clearScreen();

        setTextViewText(R.string.instruction_series_paused);
    }

    private void showToastText(int id) {

        String msg = getResources().getString(id);
        ToastCompat toast = ToastCompat.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();


//        Old version that cause BadTokenExceptions on API level 25. Could not catch it.
//        According to https://github.com/drakeet/ToastCompat this is an API 25 bug
//        try {
//
//            String msg = getResources().getString(id);
//
//            if (mToast != null)
//                mToast.cancel();
//
//            mToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
//            mToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
//            mToast.show();
//
//        }
//        catch (WindowManager.BadTokenException e) {
////            Ignore the bad token exception, according to https://github.com/drakeet/ToastCompat
////            this should only happen on API level 25, but I could not reproduce it with a virtual
////            machine.
//        }
    }

    /**
     * Returns instruction messages, depending on the current state of the CVResult object.
     * @param state state of the CVResult object
     * @return instruction message
     */
    private String getInstructionMessage(int state) {

        switch (state) {

            case CVResult.DOCUMENT_STATE_EMPTY:
                return getResources().getString(R.string.instruction_empty);

            case CVResult.DOCUMENT_STATE_OK:
                return getResources().getString(R.string.instruction_ok);

            case CVResult.DOCUMENT_STATE_SMALL:
                return getResources().getString(R.string.instruction_small);

            case CVResult.DOCUMENT_STATE_PERSPECTIVE:
                return getResources().getString(R.string.instruction_perspective);

            case CVResult.DOCUMENT_STATE_UNSHARP:
                return getResources().getString(R.string.instruction_unsharp);

            case CVResult.DOCUMENT_STATE_BAD_ILLUMINATION:
                return getResources().getString(R.string.instruction_bad_illumination);

            case CVResult.DOCUMENT_STATE_ROTATION:
                return getResources().getString(R.string.instruction_rotation);

            case CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED:
                return getResources().getString(R.string.instruction_no_focus_measured);

            case CVResult.DOCUMENT_STATE_NO_ILLUMINATION_MEASURED:
                return getResources().getString(R.string.instruction_no_illumination_measured);


        }

        return getResources().getString(R.string.instruction_unknown);

    }

    // =================  end: CameraPreview.DimensionChange CALLBACK =================

    /**
     * Shows the last picture taken as a thumbnail on the gallery button.
     */
    private void updateThumbnail() {

        int visibility = loadThumbNail() ? View.VISIBLE : View.INVISIBLE;
        mGalleryButton.setVisibility(visibility);

    }

    private boolean loadThumbNail() {

        String fileName = DocumentStorage.getInstance(this).getLastPageFileInActiveDocument();
        if (fileName != null) {
            //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
            int exifOrientation = -1;
            try {
                exifOrientation = Helper.getExifOrientation(fileName);
            } catch (IOException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            if (exifOrientation != -1) {
                GlideApp.with(mContext)
                        .load(fileName)
                        .signature(new MediaStoreSignature("", 0, exifOrientation))
                        .into(mGalleryButton);
            }
            else {
                GlideApp.with(mContext)
                        .load(fileName)
                        .into(mGalleryButton);
            }

            return true;
        }

        return false;

    }


    private int getExifOrientation() {

        switch (mCameraOrientation) {

            case 0:
                return 1;
            case 90:
                return 6;
            case 180:
                return 3;
            case 270:
                return 8;

        }

        return -1;

    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.CAMERA;
    }




    /**
     * Class used to save pictures in an own thread (AsyncTask).
     */
    private class FileSaver extends AsyncTask<Void, Void, String> {

        private byte[] mData;

        public FileSaver(byte[] data) {

            mData = data;

        }

        @Override
        protected String doInBackground(Void... voids) {

            Uri uri = getFileName(mContext.getString(R.string.app_name));
            Log.d(CLASS_NAME, "FileSaver: uri " + uri);

            final File file = new File(uri.getPath());

            if (file == null)
                return null;

            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGalleryButton.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
                });

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(mData);

                fos.close();

                // Save exif information (especially the orientation):
                saveExif(file);

                boolean fileAdded = DocumentStorage.getInstance(mContext).addToActiveDocument(file);
                if (!fileAdded)
                    DocumentStorage.getInstance(mContext).generateDocument(file, mContext);

                mIsPictureSafe = true;

                return uri.getPath();

            }

            catch (Exception e) {

                Crashlytics.logException(e);

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mGalleryButton.setVisibility(View.VISIBLE);
                        mIsPictureSafe = false;
                        showSaveErrorDialog();
                    }

                });


//                Log.d(CLASS_NAME, "Could not save file: " + outFile);
            }


            return null;


        }

        private void updateThumbnail(final File file) {
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {

                public void onScanCompleted(String path, Uri uri) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mProgressBar.setVisibility(View.INVISIBLE);
                            mGalleryButton.setVisibility(View.VISIBLE);
//                            Check if the activity is closing. This might especially happen on slow
//                            devices, where it takes some time to process the image and the app
//                            is closed, before the thumbnail gets shown.
                            if (!((Activity) mContext).isFinishing())
                                GlideApp.with(mContext)
                                        .load(file.getPath())
                                        .into(mGalleryButton);

                        }
                    });


                }

            });
        }


        private void saveExif(File outFile) throws IOException {

            final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
            if (exif != null) {
                // Save the orientation of the image:
                int orientation = getExifOrientation();
                String exifOrientation = Integer.toString(orientation);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);

//                Save the docscan information:
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, getString(R.string.app_name));

                // Save the GPS coordinates if available:
                Location location = LocationHandler.getInstance(mContext).getLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
//                        Taken from http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android (post by fabien):
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(latitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(latitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(longitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(longitude));
                }

                exif.saveAttributes();

            }
        }



        protected void onPostExecute(String uri) {

            // Release the memory. Note this is essential, because otherwise allocated memory will increase.
            mData = null;

            // Set the thumbnail on the gallery button, this must be done on the UI thread:
            if (uri != null) {
                updateThumbnail(new File(uri));

                //            Start the page detection on the saved image:
                ImageProcessor.pageDetection(new File(uri));
            }
            else
                Log.d(CLASS_NAME, "onPostExecute: could not save file!");

        }


    }


    private static Bitmap decodeFile(File f,int width,int height){
        try {
            //Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            if (options.outWidth > options.outHeight)
                height = (int) (height * (float) width / options.outWidth);
            else
                width = (int) (width * (float) height / options.outHeight);
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, options);

        } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
        }
        return null;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }




}