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
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.CameraPaintLayout;
import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.DebugViewFragment;
import at.ac.tuwien.caa.docscan.camera.GPS;
import at.ac.tuwien.caa.docscan.camera.LocationHandler;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.PaintView;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.ChangeDetector;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.logic.AppState;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.SHOT_TIME;
import static at.ac.tuwien.caa.docscan.logic.Helper.getMediaStorageUserSubDir;
import static at.ac.tuwien.caa.docscan.logic.Settings.SettingEnum.HIDE_SERIES_DIALOG_KEY;
import static at.ac.tuwien.caa.docscan.logic.Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY;
import static at.ac.tuwien.caa.docscan.logic.Settings.SettingEnum.SERIES_MODE_PAUSED_KEY;

/**
 * The main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */

public class CameraActivity extends BaseNavigationActivity implements TaskTimer.TimerCallbacks,
        CameraPreview.CVCallback, CameraPreview.CameraPreviewCallback, CVResult.CVResultCallback,
        MediaScannerConnection.MediaScannerConnectionClient, PopupMenu.OnMenuItemClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "CameraActivity";
    private static final String FLASH_MODE_KEY = "flashMode"; // used for saving the current flash status
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 2;

    @SuppressWarnings("deprecation")
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mGalleryButton;
    private Button mDocumentButton;
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
    private MediaScannerConnection mMediaScannerConnection;
    private boolean mIsPictureSafe;
    private boolean mIsSaving = false;
    private TextView mTextView;
    private Menu mOptionsMenu;;
    private MenuItem mFlashMenuItem;
    private Drawable mManualShootDrawable, mAutoShootDrawable, mFlashOffDrawable,
            mFlashOnDrawable, mFlashAutoDrawable, mFlashTorchDrawable;
    private boolean mIsSeriesMode = false;
    private boolean mHideSeriesDialog;
    private boolean mIsSeriesModePaused = true;
    private long mStartTime;
    // We hold here a reference to the popupmenu and the list, because we are not sure what is first initialized:
    private List<String> mFlashModes;
    private PopupMenu mFlashPopupMenu, mSeriesPopupMenu;
    private byte[] mPictureData;
    private Drawable mGalleryButtonDrawable;
    private ProgressBar mProgressBar;
    private final static int SINGLE_POS = 0;
    private final static int SERIES_POS = 1;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private static Date mLastTimeStamp;
    private int mMaxFrameCnt = 0;
    private DkPolyRect[] mLastDkPolyRects;
    private Toast mToast;

    private boolean mIsQRActive = false;


    /**
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

//         We need this for Android 4:
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Error while initializing OpenCV.");
        } else {

            System.loadLibrary("opencv_java3");
            System.loadLibrary("docscan-native");
        }

    }

    private long mLastTime;
    private boolean mItemSelectedAutomatically = false;


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

        //        Open the log file at app startup:
        if (AppState.isDataLogged())
            DataLog.getInstance().readLog(this);

        initActivity();

        mContext = this;

    }


    /**
     * Stops the camera and the paint view thread.
     */
    @Override
    public void onPause() {

        if (mPaintView != null)
            mPaintView.pause();

        if (mCameraPreview != null)
            mCameraPreview.pause();


        savePreferences();

//        // Save the sync info:
//        SyncInfo.getInstance().saveToDisk(this);

        //        Save the log file:
        if (AppState.isDataLogged())
            DataLog.getInstance().writeLog(this);

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

        // Read the sync information:
//        SyncInfo.getInstance().readFromDisk(this);

        // Resume camera access:
        if (mCameraPreview != null)
            mCameraPreview.resume();

        // Resume drawing thread:
        if (mPaintView != null)
            mPaintView.resume();

        // Here we must update the button text after a activity resume (others are just initialized
//        in onCreate.
        initDocumentButton();

        mHideSeriesDialog = Settings.getInstance().loadKey(this, HIDE_SERIES_DIALOG_KEY);

        showControlsLayout(!mIsQRActive);

		checkProviderInstaller();

//        MovementDetector.getInstance(this.getApplicationContext()).start();

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
//
//        if (mDocumentMenuItem != null)
//            mDocumentMenuItem.setVisible(showControls);
        if (mFlashMenuItem != null)
            mFlashMenuItem.setVisible(showControls);

        // Show/hide the overflow button:
        if (mOptionsMenu != null)
            mOptionsMenu.setGroupVisible(R.id.overflow_menu_group, showControls);

        RelativeLayout l = (RelativeLayout) findViewById(R.id.controls_layout);
        if (l != null)
            l.setVisibility(controlsVisibility);

        RelativeLayout qrLayout = (RelativeLayout) findViewById(R.id.qr_controls_layout);
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

            // Indicates that Google Play services is out of date, disabled, etc.
            // Prompt the user to install/update/enable Google Play services.
            GooglePlayServicesUtil.showErrorNotification(
                    e.getConnectionStatusCode(), mContext);

        } catch (GooglePlayServicesNotAvailableException e) {
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

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        if (mPaintView != null)
            mPaintView.setCVResult(mCVResult);

        mCounterView = (TextView) findViewById(R.id.counter_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupToolbar();
        setupNavigationDrawer();
        setupDebugView();

        // Show the debug view: (TODO: This should be not the case for releases)
//        showDebugView();

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

//        initCameraControlLayout();

        initDrawables();
        initPictureCallback();
        initButtons();

        requestLocation();



    }

    private void loadPreferences() {

        // Concerning series mode:
        mIsSeriesMode = Settings.getInstance().loadKey(this, SERIES_MODE_ACTIVE_KEY);
        mIsSeriesModePaused = Settings.getInstance().loadKey(this, SERIES_MODE_PAUSED_KEY);

        UserHandler.loadSeriesName(this);

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
        inflater.inflate(R.menu.actionbar_menu, menu);

        mOptionsMenu = menu;

        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);
//        mDocumentMenuItem = menu.findItem(R.id.document_item);

        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
        if (mFlashModes != null)
            mFlashMenuItem.setVisible(true);

        return true;

    }

    @SuppressWarnings("deprecation")
    private void initDrawables() {

        mAutoShootDrawable = getResources().getDrawable(R.drawable.auto_shoot);
        mManualShootDrawable = getResources().getDrawable(R.drawable.manual_auto);

        mFlashAutoDrawable = getResources().getDrawable(R.drawable.ic_flash_auto);
        mFlashOffDrawable = getResources().getDrawable(R.drawable.ic_flash_off);
        mFlashOnDrawable = getResources().getDrawable(R.drawable.ic_flash_on);
        mFlashTorchDrawable = getResources().getDrawable(R.drawable.ic_torch);

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
     * Connects the document button with its OnClickListener.
     */
    private void initDocumentButton() {

        mDocumentButton = (Button) findViewById(R.id.document_button);
        String documentName = User.getInstance().getDocumentName();

        if (documentName != null && mDocumentButton != null) {
            mDocumentButton.setText(getString(R.string.camera_series_button_prefix) + " " + documentName);
            mDocumentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startDocumentActivity();
                }
            });
        }

    }

    /**
     * Start the document activity via an intent.
     */
    private void startDocumentActivity() {

        Intent intent = new Intent(getApplicationContext(), SeriesGeneralActivity.class);
        startActivity(intent);

    }

    // ================= start: methods for opening the gallery =================

    /**
     * Connects the gallery button with its OnClickListener.
     */
    private void initGalleryCallback() {

        mGalleryButton = (ImageButton) findViewById(R.id.gallery_button);
        mProgressBar = (ProgressBar) findViewById(R.id.saving_progressbar);

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

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.M)
            requestFileOpen();
        else
            startScan();

    }



    /**
     * Request to read the external storage. This method is used to enable file saving in Android >= marshmallow
     * (Android 6), since in this version external file opening is not allowed without user permission.
     */
    @TargetApi(16)
    private void requestFileOpen() {

        // Check permission

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        } else
            startScan();

    }

    /**
     * Starts the MediaScanner.
     */
    private void startScan() {

        if (mMediaScannerConnection != null)
            mMediaScannerConnection.disconnect();

        mMediaScannerConnection = new MediaScannerConnection(this, this);
        mMediaScannerConnection.connect();

    }

    /**
     * Tells the MediaScanner where the directory of DocScan pictures is and tells it to scan the
     * most recent file.
     */
    @Override
    public void onMediaScannerConnected() {

        File mediaStorageDir = getMediaStorageUserSubDir(getResources().getString(R.string.app_name));

        if (mediaStorageDir == null) {
            showNoFileFoundDialog();
            return;
        }

        String[] files = mediaStorageDir.list();

        if (files == null) {
            showNoFileFoundDialog();
            return;
        } else if (files.length == 0) {
            showNoFileFoundDialog();
            return;
        }

        //	    Opens the most recent image:
        Arrays.sort(files);
        String fileName = mediaStorageDir.toString() + File.separator + files[files.length - 1];
        mMediaScannerConnection.scanFile(fileName, null);

    }

    /**
     * Starts an intent with the last saved picture as data. This event is then handled by a user
     * defined app (like the image gallery app).
     *
     * @param path Path
     * @param uri Uri
     */
    @Override
    public void onScanCompleted(String path, Uri uri) {

        try {


            if (uri != null) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);

                startActivity(intent);

            }


        } finally {
            mMediaScannerConnection.disconnect();
            mMediaScannerConnection = null;
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

                Log.d(TAG, "taking picture");

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

                Log.d(TAG, "took picture");

            }
        };

    }

    /**
     * Setup a listener for photo shoot button.
     */
    private void initPhotoButton() {

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        photoButton.setOnClickListener(

                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsSeriesMode) {
                            mIsSeriesModePaused = !mIsSeriesModePaused;
                            showShootModeToast();
                            updateMode();

                            // Show the SeriesGeneralActivity just if the user started the series mode and the hide
                            // dialog setting is not true:
                            if (mIsSeriesMode && !mIsSeriesModePaused &&  !mHideSeriesDialog)
                                startDocumentActivity();
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
        loadThumbnail();
        initShootModeSpinner();
        updatePhotoButtonIcon();
        initCancelQRButton();

    }

    private void initCancelQRButton() {

        ImageButton button = (ImageButton) findViewById(R.id.cancel_qr_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsQRActive = false;
                mCameraPreview.startQrMode(false);
                showControlsLayout(!mIsQRActive);
            }
        });

    }

    private void initShootModeSpinner() {
        
        // TODO: define the text and the icons in an enum, to ensure that they have the same order.
        // Spinner for shoot mode:
        Spinner shootModeSpinner = (Spinner) findViewById(R.id.shoot_mode_spinner);
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

        mCameraPreview.setAwaitFrameChanges(mIsSeriesMode);

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

        Spinner shootModeSpinner = (Spinner) findViewById(R.id.shoot_mode_spinner);
        if (mIsSeriesMode)
            shootModeSpinner.setSelection(SERIES_POS);
        else
            shootModeSpinner.setSelection(SINGLE_POS);

    }

    private void updatePhotoButtonIcon() {

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);
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

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused) {
                drawable = R.drawable.ic_play_arrow_24dp;
                displaySeriesModePaused(); // shows a text in the text view and removes any CVResults shown.
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
        }

        if (mCameraPreview != null)
            mCameraPreview.pauseImageProcessing(mIsSeriesModePaused);

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

        mCameraPreview.storeMat(mIsSeriesMode);
        if (mCameraPreview.getCamera() != null) {
            mCameraPreview.getCamera().takePicture(shutterCallback, null, mPictureCallback);
//            if (mCameraPreview.isFrameSteady())
//                mCameraPreview.getCamera().takePicture(shutterCallback, null, mPictureCallback);
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

        Uri uri = getOutputMediaFile(getResources().getString(R.string.app_name));
        FileSaver fileSaver = new FileSaver(data);
        fileSaver.execute(uri);

    }

    private void showSaveErrorDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.picture_save_error_text).setTitle(R.string.picture_save_error_title);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ChangeDetector.resetNewFrameDetector();
                mIsPictureSafe = true;
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Returns the URI of a new file containing a time stamp.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the filename.
     */
    private static Uri getOutputMediaFile(String appName) {

        File mediaStorageDir = getMediaStorageUserSubDir(appName);
        if (mediaStorageDir == null)
            return null;

        // Create a media file name
        mLastTimeStamp = new Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(mLastTimeStamp);
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                mContext.getString(R.string.img_prefix) + timeStamp + mContext.getString(R.string.img_extension));

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

        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        // Tell the camera that the orientation changed, so it can adapt the preview orientation:
        if (mCameraPreview != null)
            mCameraPreview.displayRotated();

       // Change the layout dynamically: Remove the current camera_controls_layout and add a new
        // one, which is appropriate for the orientation (portrait or landscape xml's).
        ViewGroup appRoot = (ViewGroup) findViewById(R.id.main_frame_layout);
        if (appRoot == null)
            return;

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
//        initDocumentButton();


//        getLayoutInflater().inflate(R.layout.camera_controls_layout, appRoot);
//        View view = findViewById(R.id.camera_controls_layout);
//        view.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));
//
//        // Initialize the newly created buttons:
//        initButtons();
//        initDocumentButton();

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
            if (getOrientation() == Surface.ROTATION_0 || getOrientation() == Surface.ROTATION_180)
                dim = new Point(size.x, size.y - v.getHeight());
//                return size.y - v.getHeight();
            else if (getOrientation() == Surface.ROTATION_90 || getOrientation() == Surface.ROTATION_270)
                dim = new Point(size.x - v.getWidth(), size.y);
//                return size.x - v.getWidth();
        }

        return dim;


    }

    private void showDebugView() {

        // Create the debug view - if it is not already created:
        if (mDebugViewFragment == null) {
            mDebugViewFragment = new DebugViewFragment();
        }

        getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();

        mIsDebugViewEnabled = true;

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

//            Show / hide the debug view
            case R.id.debug_view_item:

                // Create the debug view - if it is not already created:
                if (mDebugViewFragment == null) {
                    mDebugViewFragment = new DebugViewFragment();
                }

                // Show the debug view:
                if (getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT) == null) {
                    mIsDebugViewEnabled = true;
                    item.setTitle(R.string.hide_debug_view_text);
                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
                }
                // Hide the debug view:
                else {
                    mIsDebugViewEnabled = false;
                    item.setTitle(R.string.show_debug_view_text);
                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
                }

                return true;

            // Switch between the two page segmentation methods:
            case R.id.use_lab_item:

                if (NativeWrapper.useLab()) {
                    NativeWrapper.setUseLab(false);
                    item.setTitle(R.string.precise_page_seg_text);
                }
                else {
                    NativeWrapper.setUseLab(true);
                    item.setTitle(R.string.fast_page_seg_text);
                }

                return true;

            // Focus measurement:
            case R.id.show_fm_values_item:

                if (mPaintView.isFocusTextVisible()) {
                    item.setTitle(R.string.show_fm_values_text);
                    mPaintView.drawFocusText(false);
                } else {
                    item.setTitle(R.string.hide_fm_values_text);
                    mPaintView.drawFocusText(true);
                }

                break;

            // Guide lines:
            case R.id.show_guide_item:

                if (mPaintView.areGuideLinesDrawn()) {
                    mPaintView.drawGuideLines(false);
                    item.setTitle(R.string.show_guide_text);
                } else {
                    mPaintView.drawGuideLines(true);
                    item.setTitle(R.string.hide_guide_text);
                }

                break;

//            // Threading:
//            case R.id.threading_item:
//                if (mCameraPreview.isMultiThreading()) {
//                    mCameraPreview.setThreading(false);
//                    item.setTitle(R.string.multi_thread_text);
//                }
//                else {
//                    mCameraPreview.setThreading(true);
//                    item.setTitle(R.string.single_thread_text);
//                }


        }

        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
//            getSupportActionBar().setDisplayShowTitleEnabled(true);
//            getSupportActionBar().setTitle("Ich bin ein �sterreicher");
//
//            getToolbar().setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    showSeriesPopup(null);
//                }
//            });

//            final int abTitleId = getResources().getIdentifier("Ich bin ein �sterreicher", "id", "android");
//            findViewById(abTitleId).setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    showSeriesPopup(null);
//                }
//            });

        }


    }

    private void setupDebugView() {

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);
        mTimerCallbacks = this;
        mTextView = (TextView) findViewById(R.id.instruction_view);

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

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        NavigationView mDrawer = (NavigationView) findViewById(R.id.left_drawer);
        setupDrawerContent(mDrawer);

        // Set the item text for the debug view in the naviation drawer:
        if (mDrawer == null)
            return;

        Menu menu = mDrawer.getMenu();
        if (menu == null)
            return;

        MenuItem item = menu.findItem(R.id.debug_view_item);
        if (item == null)
            return;

        if (mIsDebugViewEnabled)
            item.setTitle(R.string.hide_debug_view_text);
        else
            item.setTitle(R.string.show_debug_view_text);

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

        switch (menuItem.getItemId()) {

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
//                    menuItem.setTitle(R.string.hide_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
//                }
//                // Hide the debug view:
//                else {
//                    mIsDebugViewEnabled = false;
//                    menuItem.setTitle(R.string.show_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
//                }
//
//                break;

            // Focus measurement:
            case R.id.show_fm_values_item:

                if (mPaintView.isFocusTextVisible()) {
                    menuItem.setTitle(R.string.show_fm_values_text);
                    mPaintView.drawFocusText(false);
                } else {
                    menuItem.setTitle(R.string.hide_fm_values_text);
                    mPaintView.drawFocusText(true);
                }

                break;

            // Guide lines:
            case R.id.show_guide_item:

                if (mPaintView.areGuideLinesDrawn()) {
                    mPaintView.drawGuideLines(false);
                    menuItem.setTitle(R.string.show_guide_text);
                } else {
                    mPaintView.drawGuideLines(true);
                    menuItem.setTitle(R.string.hide_guide_text);
                }

                break;

//            // Switch between the two page segmentation methods:
//            case R.id.action_precise_page_seg:
//
//                if (NativeWrapper.useLab()) {
//                    NativeWrapper.setUseLab(false);
//                    menuItem.setTitle(R.string.precise_page_seg_text);
//                }
//                else {
//                    NativeWrapper.setUseLab(true);
//                    menuItem.setTitle(R.string.fast_page_seg_text);
//                }


        }

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


    public static int getOrientation() {

        WindowManager w = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);

        return w.getDefaultDisplay().getRotation();

    }


    // ================= start: CALLBACKS called from native files =================

    /**
     * Called after focus measurement is finished.
     *
     * @param patches Patches array
     */
    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (mCVResult != null)
            mCVResult.setPatches(patches);

    }

    /**
     * Called after page segmentation is finished.
     *
     * @param dkPolyRects Array of polyRects
     */
    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects, int frameCnt) {

        // Check if the result is returned from a thread that is already outdated
        // (an up-to-date result is already computed):
        if (frameCnt <= mMaxFrameCnt) {
            Log.d(TAG, "skipped frame id: " + frameCnt);
            return;
        }

        mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - mLastTime;
        Log.d(TAG, "time difference: " + timeDiff);

        mLastTime = currentTime;

        if (mCVResult != null) {
//            mCVResult.setPatches(null);
            if (!mCVResult.isStable())
                mCVResult.setPatches(new Patch[0]);

            mCVResult.setDKPolyRects(dkPolyRects);
        }

//        if (isRectJumping(dkPolyRects))
//            mCameraPreview.startFocusMeasurement(false);
//        else
//            mCameraPreview.startFocusMeasurement(true);

        mLastDkPolyRects = dkPolyRects;

        mMaxFrameCnt = frameCnt;


        mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);


    }

    boolean isRectJumping(DkPolyRect[] dkPolyRects){

        boolean isJumping = false;

        Log.d(TAG, "jumping?");

        if (dkPolyRects != null && mLastDkPolyRects != null) {
            Log.d(TAG, "check 1");
            if (dkPolyRects.length == 1 && mLastDkPolyRects.length == 1) {
                Log.d(TAG, "check 2");
                PointF distVec = mLastDkPolyRects[0].getLargestDistVector(dkPolyRects[0]);
                PointF normedPoint = mCVResult.normPoint(distVec);

                if (normedPoint.length() >= .05) {
                    isJumping = true;
                }

                Log.d(TAG, "distance: " + normedPoint.length());
            }
        }

        return isJumping;
    }

    /**
     * Called after page segmentation is finished.
     *
     * @param value illumination value
     */
    @Override
    public void onIluminationComputed(double value) {

        if (mCVResult != null)
            mCVResult.setIllumination(value);


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
    public void onFocusTouch(PointF point) {

        Log.d(TAG, "onFocusTouch");

        if (mPaintView != null)
            mPaintView.drawFocusTouch(point);

    }

    @Override
    public void onFocusTouchSuccess() {

        if (mPaintView != null)
            mPaintView.drawFocusTouchSuccess();
    }

    @Override
    public void onMovement(boolean moved) {

        // This happens if the user has just switched to single mode and the event occurs later than the touch event.
        // TODO: implement a proper handling of the events.
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

        if (!mIsPictureSafe)
            return;

        //  Tell the user that a picture will be taken:
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                mTextView.setText(getResources().getString(R.string.taking_picture_text));
            }
        });

        takePicture();


    }

    @Override
    public void onQRCode(Result result) {

        final String text;
        if (result == null)
            text = getString(R.string.instruction_searching_qr);
        else
            text = result.toString();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
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

        final String msgText = getResources().getString(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTextView != null)
                    mTextView.setText(msgText);
            }
        });
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

    public void showFlashPopup(MenuItem item) {

        View menuItemView = findViewById(R.id.flash_mode_item);
        if (menuItemView == null)
            return;

        // Create the menu for the first time:
        if (mFlashPopupMenu == null) {
            mFlashPopupMenu = new PopupMenu(this, menuItemView);
            mFlashPopupMenu.setOnMenuItemClickListener(this);
            mFlashPopupMenu.inflate(R.menu.flash_mode_menu);

            setupFlashUI();
        }



        mFlashPopupMenu.show();

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
                startActivity(new Intent(getApplicationContext(), CreateSeriesActivity.class));
                return true;

            case R.id.series_use_existing_item:
                startActivity(new Intent(getApplicationContext(), DocumentActivity.class));
                return true;

            case R.id.series_qr_item:
                mIsQRActive = true;
                showControlsLayout(false);
                mCameraPreview.startQrMode(true);
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

        CameraPaintLayout l = (CameraPaintLayout) findViewById(R.id.camera_paint_layout);
        if (l != null)
            l.setFrameDimensions(width, height);

        View v = findViewById(R.id.camera_controls_layout);
        if ((v != null) && (!mCameraPreview.isPreviewFitting()))
            v.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));

    }

    /**
     * Called after the the status of the CVResult object is changed.
     * @param state state of the CVResult
     */
    @Override
    public void onStatusChange(final int state) {

        if (mIsSeriesModePaused) {
            displaySeriesModePaused();
            return;
        }

        // Check if we need the focus measurement at this point:
        if (state == CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED) {
            //            if (mCVResult.isStable())
            mCameraPreview.startFocusMeasurement(true);
            //            else
            //                mCameraPreview.startFocusMeasurement(false);
        }
        // TODO: I do not know why this is happening, once the CameraActivity is resumed:
        else if (state > CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED && !mCameraPreview.isFocusMeasured()) {
            //            if (mCVResult.isStable())
            mCameraPreview.startFocusMeasurement(true);
            //            else
            //                mCameraPreview.startFocusMeasurement(false);
        } else if (state < CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED) {
            mCameraPreview.startFocusMeasurement(false);
        }


        final String msg;

        if (!mIsPictureSafe) {
            msg = getResources().getString(R.string.taking_picture_text);
        }

        else if (!mIsSeriesMode || state != CVResult.DOCUMENT_STATE_OK) {
            msg = getInstructionMessage(state);
        }

        else {

            if (mIsSeriesMode) {
                mCameraPreview.verifyCapture();
                return;
            }
            else {
                msg = getResources().getString(R.string.taking_picture_text);
                if (mIsPictureSafe)
                    takePicture();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                mTextView.setText(msg);
            }
        });



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

        if (mToast != null)
            mToast.cancel();

        mToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        mToast.show();
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
    private void loadThumbnail() {

        // Check if a thumbnail is already existing (this should occur on orientation changes):
        if (mGalleryButtonDrawable != null)
            setGalleryButtonDrawable(mGalleryButtonDrawable);

        // Load the most recent image from the folder:
        else
            loadThumbnailFromDisk();

    }

    private void loadThumbnailFromDisk() {
        File mediaStorageDir = Helper.getMediaStorageUserSubDir(getResources().getString(R.string.app_name));
        if (mediaStorageDir == null)
            return;

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };

        File[] files = mediaStorageDir.listFiles(filesFilter);

        if (files == null)
            return;
        else if (files.length == 0)
            return;

        // Determine the most recent image:
        Arrays.sort(files);
        String fileName = mediaStorageDir.toString() + "/" + files[files.length - 1].getName();

        ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
        thumbnailLoader.execute(fileName);
    }

    @SuppressWarnings("deprecation")
    private void setGalleryButtonDrawable(Drawable drawable) {

        mGalleryButton.setVisibility(View.VISIBLE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//            mGalleryButton.setBackground(drawable);
//        else
//            mGalleryButton.setBackgroundDrawable(drawable);
//
        mGalleryButton.setImageDrawable(drawable);
//        mGalleryButton.setAdjustViewBounds(true);
//        mGalleryButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // Keep a reference to the drawable, because we need it if the orientation is changed:
        mGalleryButtonDrawable = drawable;

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

    private int getAngleFromExif(int orientation) {

        switch (orientation) {

            case 1:
                return 0;
            case 6:
                return 90;
            case 3:
                return 180;
            case 8:
                return 270;

        }

        return -1;

    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.CAMERA;
    }


    /**
     * Class responsible for loading thumbnails from images. This is time intense and hence it is
     * done in an own thread (AsyncTask).
     */
    private class ThumbnailLoader extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... fileNames) {

            String fileName = fileNames[0];

            Bitmap thumbNailBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileName), 50, 50);
            if (thumbNailBitmap == null)
                return null;

            // Determine the rotation angle of the image:
            int angle = -1;
            try {
                ExifInterface exif = new ExifInterface(fileName);
                String attr = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                angle = getAngleFromExif(Integer.valueOf(attr));
            } catch (IOException e) {
                return null;
            }

            //Rotate the image:
            Matrix mtx = new Matrix();
            mtx.setRotate(angle);
            thumbNailBitmap = Bitmap.createBitmap(thumbNailBitmap, 0, 0, thumbNailBitmap.getWidth(), thumbNailBitmap.getHeight(), mtx, true);

            // Update the gallery button:
            final BitmapDrawable thumbDrawable = new BitmapDrawable(getResources(), thumbNailBitmap);
            if (thumbDrawable == null)
                return null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setGalleryButtonDrawable(thumbDrawable);
                }
            });

            return null;

        }

    }

    /**
     * Class used to save pictures in an own thread (AsyncTask).
     */
    private class FileSaver extends AsyncTask<Uri, Void, Void> {

        private byte[] mData;

        public FileSaver(byte[] data) {

            mData = data;

        }

        @Override
        protected Void doInBackground(Uri... uris) {

            mIsSaving = true;

            final File outFile = new File(uris[0].getPath());

            if (outFile == null)
                return null;

            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGalleryButton.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
                });

                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(mData);

                fos.close();

                // Save exif information (especially the orientation):
                saveExif(outFile);

                // Set the thumbnail on the gallery button, this must be done one the UI thread:
                updateThumbnail(outFile);

//                // Add the file to the sync list:
//                addToSyncList(mContext, outFile);

                mIsPictureSafe = true;

            }

            catch (Exception e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mGalleryButton.setVisibility(View.VISIBLE);
                        mIsPictureSafe = false;
                        showSaveErrorDialog();
                    }

                });


//                Log.d(TAG, "Could not save file: " + outFile);
            }


            return null;


        }

        private void updateThumbnail(final File outFile) {
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outFile.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {

                public void onScanCompleted(String path, Uri uri) {

//                        Before ThumbnailUtils.extractThumbnail was used which causes OOM's:
//                        Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(outFile.toString()), 200, 200);

//                        Instead use this method to avoid loading large images into memory:
                    Bitmap resized = decodeFile(outFile, 200, 200);

                    Matrix mtx = new Matrix();
                    mtx.setRotate(mCameraOrientation);

                    resized = Bitmap.createBitmap(resized, 0, 0, resized.getWidth(), resized.getHeight(), mtx, true);
                    final BitmapDrawable thumbDrawable = new BitmapDrawable(getResources(), resized);

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            setGalleryButtonDrawable(thumbDrawable);
                        }

                    });

                }

//                    }
            });
        }

        private void addToSyncList(Context context, File outFile) {

            SyncInfo.getInstance().addFile(context, outFile);

        }

        private void saveExif(File outFile) throws IOException {
            final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
            if (exif != null) {
                // Save the orientation of the image:
                int orientation = getExifOrientation();
                String exifOrientation = Integer.toString(orientation);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);

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
//
                //                    Log the shot:
                if (AppState.isDataLogged()) {
                    GPS gps = new GPS(location);
                    DataLog.getInstance().logShot(outFile.getAbsolutePath(), gps, mLastTimeStamp, mIsSeriesMode);
                }

                exif.saveAttributes();

            }
        }


        protected void onPostExecute(Void v) {

            mIsSaving = false;
            // Release the memory. Note this is essential, because otherwise allocated memory will increase.
            mData = null;

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

        } catch (FileNotFoundException e) {}
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