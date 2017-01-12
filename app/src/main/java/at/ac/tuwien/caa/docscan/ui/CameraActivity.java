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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.CameraPaintLayout;
import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.DebugViewFragment;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.PaintView;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.transkribus.TranskribusActivity;

/**
 * The main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */

public class CameraActivity extends BaseActivity implements TaskTimer.TimerCallbacks,
        NativeWrapper.CVCallback, CameraPreview.CameraPreviewCallback, CVResult.CVResultCallback,
        MediaScannerConnection.MediaScannerConnectionClient, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "CameraActivity";
    private static final String FLASH_MODE_KEY = "flashMode"; // used for saving the current flash status
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 0;
    @SuppressWarnings("deprecation")
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mGalleryButton;
    private TaskTimer mTaskTimer;
    private CameraPreview mCameraPreview;
    private PaintView mPaintView;
    private TextView mCounterView;
    private boolean mShowCounter = true;
    private CVResult mCVResult;
    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
    private static boolean mIsDebugViewEnabled;
    private static Context mContext;
    private int mCameraOrientation;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MediaScannerConnection mMediaScannerConnection;
    private boolean mIsPictureSafe;
    private boolean mIsSaving = false;
    // TODO: remove this variable when it is not needed anymore.
    private boolean mCheckPageSegChanges = false;
    private TextView mTextView;
    private MenuItem mFlashMenuItem;
    private Drawable mManualShootDrawable, mAutoShootDrawable, mFlashOffDrawable,
            mFlashOnDrawable, mFlashAutoDrawable, mFlashTorchDrawable;
    private boolean mIsSeriesMode = false;
    private boolean mIsSeriesModePaused = false;
    private long mStartTime;
    private boolean mIsWaitingForCapture = false;
    // We hold here a reference to the popupmenu and the list, because we are not shure what is first initialized:
    private List<String> mFlashModes;
    private PopupMenu mFlashPopupMenu;
    private byte[] mPictureData;
    private Drawable mGalleryButtonDrawable;
    private ProgressBar mProgressBar;
    private final static int SERIES_POS = 1;
    private boolean mIsFrameChanged = true;

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

//        MovementDetector.getInstance(this.getApplicationContext()).stop();

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

        // Resume camera access:
        if (mCameraPreview != null)
            mCameraPreview.resume();

        // Resume drawing thread:
        if (mPaintView != null)
            mPaintView.resume();

//        MovementDetector.getInstance(this.getApplicationContext()).start();

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

        setContentView(R.layout.activity_main);

        mCVResult = new CVResult(this);

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        if (mPaintView != null)
            mPaintView.setCVResult(mCVResult);

        mCounterView = (TextView) findViewById(R.id.counter_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupNavigationDrawer();

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);
        mTextView = (TextView) findViewById(R.id.instruction_view);

        mIsDebugViewEnabled = (mDebugViewFragment == null);
        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;

        mDebugViewFragment = new DebugViewFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
        mIsDebugViewEnabled = true;

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        initDrawables();
        initPictureCallback();
        initButtons();

//        MovementDetector.getInstance(this.getApplicationContext()).addListener(new MovementDetector.Listener() {
//
//            @Override
//            public void onMotionDetected(SensorEvent event, float acceleration) {
//
//                Log.d(TAG, "Acceleration: " + acceleration);
//                if (mCameraPreview != null)
//                    mCameraPreview.cancelAutoFocus();
//
//            }
//        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);

        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);

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
                else
                    Log.d(TAG, "permission not given");

                break;
        }
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
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

        File mediaStorageDir = getMediaStorageDir(getResources().getString(R.string.app_name));

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
        String fileName = mediaStorageDir.toString() + "/" + files[files.length - 1];
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
//                    I do not know why setData(uri) is not working with Marshmallows, it just opens one image (not the folder), with setData(Uri.fromFile) it is working:

                int currentApiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentApiVersion >= Build.VERSION_CODES.M)
                    intent.setDataAndType(Uri.fromFile(new File(path)), "image/*");
                else
                    intent.setData(uri);
//

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

                // resume the camera again (this is necessary on the Nexus 5X, but not on the Samsung S5)
                mCameraPreview.getCamera().startPreview();
                requestPictureSave(data);

            }
        };

    }

    /**
     * Setup a listener for photo shoot button.
     */
    private void setupPhotoShootButtonCallback() {

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        photoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsSeriesMode) {
                            mIsSeriesModePaused = !mIsSeriesModePaused;
                            updateShootButton();
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

        setupPhotoShootButtonCallback();
        initGalleryCallback();
        loadThumbnail();
        initShootModeSpinner();
        updateShootButton();

    }

    private void initShootModeSpinner() {
        
        // TODO: define the text and the icons in an enum, to ensure that they have the same order.
        // Spinner for shoot mode:
        Spinner shootModeSpinner = (Spinner) findViewById(R.id.shoot_mode_spinner);
        String[] shootModeText = getResources().getStringArray(R.array.shoot_mode_array);
        Integer[] shootModeIcons = new Integer[]{R.drawable.ic_photo_vector, R.drawable.ic_burst_mode_vector};
        shootModeSpinner.setAdapter(new ShootModeAdapter(this, R.layout.spinner_row, shootModeText, shootModeIcons));
        shootModeSpinner.setOnItemSelectedListener(this);

        if (mIsSeriesMode)
            shootModeSpinner.setSelection(SERIES_POS);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if ((position == SERIES_POS) && !mIsSeriesMode) {
            mIsSeriesMode = true;
            mIsSeriesModePaused = false;
        }
        else if (position != SERIES_POS && mIsSeriesMode)
            mIsSeriesMode = false;

        updateShootButton();

    }


    private void updateShootButton() {

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused)
                drawable = R.drawable.ic_play_arrow_24dp;
            else
                drawable = R.drawable.ic_pause_24dp;
        }
        else
            drawable = R.drawable.ic_photo_camera;

        photoButton.setImageResource(drawable);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void storeFrame() {

    }

    /**
     * Tells the camera to take a picture.
     */
    private void takePicture() {

        if (mCheckPageSegChanges)
            mCVResult.storePageState();

        mCameraPreview.storeMat();

        mIsPictureSafe = false;
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                mPaintView.showFlicker();
            }
        };
        mCameraPreview.getCamera().takePicture(shutterCallback, null, mPictureCallback);

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

    /**
     * Returns the URI of a new file containing a time stamp.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the filename.
     */
    private static Uri getOutputMediaFile(String appName) {

        File mediaStorageDir = getMediaStorageDir(appName);
        if (mediaStorageDir == null)
            return null;

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return Uri.fromFile(mediaFile);
    }

    /**
     * Returns the path to the directory in which the images are saved.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    private static File getMediaStorageDir(String appName) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {

                return null;
            }
        }

        return mediaStorageDir;
    }


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

        // Initialize the newly created buttons:
        initButtons();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set the item text for the debug view in the naviation drawer:
        if (mDrawer == null)
            return;

        Menu menu = mDrawer.getMenu();
        if (menu == null)
            return;

        MenuItem item = menu.findItem(R.id.action_show_debug_view);
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

            case R.id.action_show_debug_view:

                // Create the debug view - if it is not already created:
                if (mDebugViewFragment == null) {
                    mDebugViewFragment = new DebugViewFragment();
                }

                // Show the debug view:
                if (getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT) == null) {
                    mIsDebugViewEnabled = true;
                    menuItem.setTitle(R.string.hide_debug_view_text);
                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
                }
                // Hide the debug view:
                else {
                    mIsDebugViewEnabled = false;
                    menuItem.setTitle(R.string.show_debug_view_text);
                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
                }

                break;

            // Focus measurement:
            case R.id.action_show_fm_values:

                if (mPaintView.isFocusTextVisible()) {
                    menuItem.setTitle(R.string.show_fm_values_text);
                    mPaintView.drawFocusText(false);
                } else {
                    menuItem.setTitle(R.string.hide_fm_values_text);
                    mPaintView.drawFocusText(true);
                }

                break;

            // Guide lines:
            case R.id.action_show_guide:

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
     * @param senderId ID of the sender, as defined in TaskTimer.
     */
    @Override
    public void onTimerStarted(int senderId) {

        if (mTaskTimer == null)
            return;

        mTaskTimer.startTaskTimer(senderId);


    }

    /**
     * Called after a task is executed. This is used to measure the time of the task execution.
     *
     * @param senderId ID of the sender, as defined in TaskTimer.
     */
    @Override
    public void onTimerStopped(final int senderId) {

        if (mTaskTimer == null)
            return;

        final long timePast = mTaskTimer.getTaskTime(senderId);

        // Normally the timer callback should just be called if the debug view is visible:
        if (mIsDebugViewEnabled) {

            if (mDebugViewFragment != null) {
                // The update of the UI elements must be done from the UI thread:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDebugViewFragment.setTimeText(senderId, timePast);
                    }
                });
            }

        }


    }


    /**
     * Returns true if the debug view is visible. Mainly used before TaskTimer events are triggered.
     *
     * @return boolean
     */
    public static boolean isDebugViewEnabled() {

        return mIsDebugViewEnabled;

    }

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
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        if (mCVResult != null) {
//            mCVResult.setPatches(null);
            mCVResult.setDKPolyRects(dkPolyRects);
        }


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
    public void onMovement() {

        setTextViewText(R.string.instruction_movement);

    }

    @Override
    public void onNoFrameDifference() {

        setTextViewText(R.string.instruction_no_changes);

    }

    private void setTextViewText(int msg) {

        final String msgText = getResources().getString(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    mTextView.setText(msgText);
            }
        });
    }

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

    }

    /**
     * Called after the the status of the CVResult object is changed.
     * @param state state of the CVResult
     */
    @Override
    public void onStatusChange(final int state) {

        // Check if we listen just for page segmentation changes:

        if (state == CVResult.DOCUMENT_STATE_NO_PAGE_CHANGES) {
                // Do nothing at this point.
            mCameraPreview.startFocusMeasurement(false);
            mCameraPreview.startIllumination(false);
        }
        // Check if we need the focus measurement at this point:
        else if (state == CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED) {
            mCameraPreview.startFocusMeasurement(true);
        }
        // Check if we need the illumination measurement at this point:
        else if (state == CVResult.DOCUMENT_STATE_NO_ILLUMINATION_MEASURED) {

            if (mCVResult.getDKPolyRects() == null)
                return;
            if (mCVResult.getDKPolyRects().length == 0)
                return;

            mCameraPreview.setIlluminationRect(mCVResult.getDKPolyRects()[0]);
            mCameraPreview.startIllumination(true);
            mCVResult.setIsIlluminationComputed(true);

        }

        else if (state != CVResult.DOCUMENT_STATE_OK && state != CVResult.DOCUMENT_STATE_BAD_ILLUMINATION) {
            mCameraPreview.startIllumination(false);
            mCVResult.setIsIlluminationComputed(false);
//            if (state != CVResult.DOCUMENT_STATE_UNSHARP) {
//                mCameraPreview.startFocusMeasurement(false);
//                mCVResult.setPatches(null);
//            }
        }

        final String msg;

//        mCameraPreview.startFocusMeasurement(true);

        if (!mIsPictureSafe) {
            msg = getResources().getString(R.string.taking_picture_text);
        }

        else if (!mIsSeriesMode || (mIsSeriesMode && mIsSeriesModePaused) || state != CVResult.DOCUMENT_STATE_OK) {
            mIsWaitingForCapture = false;
            msg = getInstructionMessage(state);
        }

        else if (mShowCounter){
            if (!mIsWaitingForCapture) {
                mStartTime = System.currentTimeMillis();
                mIsWaitingForCapture = true;
                msg = "";

            }
            else {
                // Count down:
                long timePast = System.currentTimeMillis() - mStartTime;
                int steadyTime = getResources().getInteger(R.integer.counter_time);
                if (timePast < steadyTime) {
                    final long timeLeft = steadyTime - timePast;
                    msg = getResources().getString(R.string.dont_move_text);

                    final int counter = Math.round(timeLeft / 1000);
//                    if (counter > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCounterView.setVisibility(View.VISIBLE);
                                mCounterView.setText(String.format(Locale.ENGLISH, "%d", counter));
                            }
                        });

                }
                else {
                    // Take the picture:
                    msg = getResources().getString(R.string.taking_picture_text);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCounterView.setVisibility(View.INVISIBLE);
                        }
                    });
                    mIsWaitingForCapture = false;
                    if (mIsPictureSafe)
                        takePicture();
                }

            }
        }
        else {
            msg = getResources().getString(R.string.taking_picture_text);
            mIsWaitingForCapture = false;
            if (mIsPictureSafe)
                takePicture();
        }

//        setTextViewText(msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                mTextView.setText(msg);
            }
        });



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

            case CVResult.DOCUMENT_STATE_NO_PAGE_CHANGES:
                return getResources().getString(R.string.instruction_no_changes);

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
        else {

            File mediaStorageDir = getMediaStorageDir(getResources().getString(R.string.app_name));
            if (mediaStorageDir == null)
                return;

            String[] files = mediaStorageDir.list();

            if (files == null)
                return;
            else if (files.length == 0)
                return;

            // Determine the most recent image:
            Arrays.sort(files);
            String fileName = mediaStorageDir.toString() + "/" + files[files.length - 1];

            ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
            thumbnailLoader.execute(fileName);

        }

    }

    @SuppressWarnings("deprecation")
    private void setGalleryButtonDrawable(Drawable drawable) {

        mGalleryButton.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mGalleryButton.setBackground(drawable);
        else
            mGalleryButton.setBackgroundDrawable(drawable);

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

    private void startTranskribusLoginActivity() {

        Intent intent = new Intent(this, TranskribusActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();

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

            Bitmap thumbNailBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileName), 200, 200);
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
                final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());

                if (exif != null) {
                    int orientation = getExifOrientation();
                    String exifOrientation = Integer.toString(orientation);

                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);
                    exif.saveAttributes();
                }

//    Set the thumbnail on the gallery button, this must be done one the UI thread:

                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outFile.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {


                        Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(outFile.toString()), 200, 200);

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

                mIsPictureSafe = true;

            }
            catch (FileNotFoundException e) {
                Log.d(TAG, "Could not find file: " + outFile);
            }
            catch (IOException e) {
                Log.d(TAG, "Could not save file: " + outFile);
            }


            return null;


        }

        protected void onPostExecute(Void v) {

            mIsSaving = false;

        }



    }




}