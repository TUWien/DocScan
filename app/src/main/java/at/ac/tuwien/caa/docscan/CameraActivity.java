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

package at.ac.tuwien.caa.docscan;

import android.Manifest;
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
import android.hardware.display.DisplayManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.CVResult;
import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * This is the main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */

public class CameraActivity extends AppCompatActivity implements TaskTimer.TimerCallbacks,
        NativeWrapper.CVCallback, CameraPreview.CameraPreviewCallback, CVResult.CVResultCallback,
        MediaScannerConnection.MediaScannerConnectionClient, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = "CameraActivity";
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
    private static final String CAMERA_PAINT_FRAGMENT = "CameraPaintFragment";
    private static String IMG_FILENAME_PREFIX = "IMG_";
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 0;
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mGalleryButton;

    /**
     * Id of the camera to access. 0 is the first camera.
     */
    private static final int CAMERA_ID = 0;

    private TaskTimer mTaskTimer;
    private CameraPreview mCameraPreview;
    private PaintView mPaintView;
    private TextView mCounterView;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private CameraHandlerThread mThread = null;
    private CVResult mCVResult;
    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
//    private CameraPaintFragment mCameraFragment;
    private static boolean mIsDebugViewEnabled;
    private int mDisplayRotation;
    private static Context mContext;
    private int mCameraOrientation;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MediaScannerConnection mMediaScannerConnection;
    private boolean mIsPictureSafe;
    private TextView mTextView;
    private MenuItem mModeMenuItem, mFlashMenuItem;
    private Drawable mManualShootDrawable, mAutoShootDrawable, mFlashOffDrawable, mFlashOnDrawable, mFlashAutoDrawable;
    private boolean mIsAutoMode = false;
    private long mStartTime;
    private boolean mIsWaitingForCapture = false;
    // We hold here a reference to the popupmenu and the list, because we are not shure what is first initialized:
    private List<String> mFlashModes;
    private PopupMenu mFlashPopupMenu;
    private boolean mIsFlashModeInit = false;
    private byte[] mPictureData;

    // TODO: use here values.ints
    private final int DOCUMENT_STEADY_TIME = 3000;

    /**
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

        boolean init = OpenCVLoader.initDebug();
//         It seems like we need this for Android 4:
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        } else {

            System.loadLibrary("opencv_java3");
            System.loadLibrary("docscan-native");
        }

    }


    // ================= start: methods from the Activity lifecyle =================

    /**
     * Creates the camera Activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        openCameraThread();

//        if (mCamera == null || mCameraInfo == null) {
//            // Camera is not available, display error message
//            Toast.makeText(this, "Camera is not available.", Toast.LENGTH_SHORT).show();
//            setContentView(R.layout.camera_unavailable_view);
//        } else {

            initActivity();

//        }

        mContext = this;

    }


    /**
     * Stops the camera and the paint view thread.
     */
    @Override
    public void onPause() {

        super.onPause();
        // Stop camera access

        if (mPaintView != null)
            mPaintView.pause();

        mCameraPreview.pause();
        releaseCamera();


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
        // Basically this method just calls initCamera, but inside an own thread.
        openCameraThread();

        if (mCameraPreview != null) {
            // This should only be called if the Activity is resumed after it has been paused:
            mCameraPreview.setCamera(mCamera, mCameraInfo, mDisplayRotation);
            mCameraPreview.resume();
//            mCameraPreview.resume();
        }

//        // Resume drawing thread:
        if (mPaintView != null)
            mPaintView.resume();

    }


    // ================= end: methods from the Activity lifecyle =================

    /**
     * Initializes the activity: camera preview, draw view, debug view, navigation drawer and timer
     * are initialized in this method.
     */

    private void initActivity() {

        setContentView(R.layout.activity_main);

        mCVResult = new CVResult(this);

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);
        mCameraPreview.setCamera(mCamera, mCameraInfo, mDisplayRotation);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        mPaintView.setCVResult(mCVResult);

        mCounterView = (TextView) findViewById(R.id.counter_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupNavigationDrawer();

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);
        mTextView = (TextView) findViewById(R.id.instruction_view);

//        mCameraFragment = (CameraPaintFragment) getSupportFragmentManager().findFragmentByTag(CAMERA_PAINT_FRAGMENT);
//
//        // create the fragment and data the first time
//        if (mCameraFragment == null) {
//            // add the fragment
//            mCameraFragment = new CameraPaintFragment();
//            getSupportFragmentManager().beginTransaction().add(mCameraFragment, CAMERA_PAINT_FRAGMENT).commit();
//            // load the data from the web
////            dataFragment.setData(loadMyData());
//        }


        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        initGalleryCallback();
        initPictureCallback();
        initDrawables();

        loadThumbnail();


    }

    /**
     * Returns a boolean indicating if the screen is turned on. This method is necessary to prevent a
     * resume of the Activity if the display is turned off and the app is in landscape mode, because
     * this causes a resume of the app in portrait mode.
     *
     * @return boolean indicating if the screen is on
     */
    // Taken from: http://stackoverflow.com/questions/2474367/how-can-i-tell-if-the-screen-is-on-in-android
    @SuppressWarnings("deprecation")
    // suppressed because the not deprecated function is called if API level >= 20
    public static boolean isScreenOn() {

        // If API level >= 20
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    return true;
                }
            }
            return false;
        }
        // TODO: not tested on API level < 20
        else {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            return powerManager.isScreenOn();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        mModeMenuItem = menu.findItem(R.id.shoot_mode_item);

        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);

        return true;
    }

    private void initDrawables() {

        mAutoShootDrawable = getResources().getDrawable(R.drawable.auto_shoot);
        mManualShootDrawable = getResources().getDrawable(R.drawable.manual_auto);
        mFlashAutoDrawable = getResources().getDrawable(R.drawable.ic_flash_auto);
        mFlashOffDrawable = getResources().getDrawable(R.drawable.ic_flash_off);
        mFlashOnDrawable = getResources().getDrawable(R.drawable.ic_flash_on);

    }


    /**
     * Returns a Camera object.
     *
     * @param cameraId ID of the camera
     * @return Camera object
     */
    private Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Toast.makeText(this, "Camera " + cameraId + " is not available: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }

    }

    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
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
     * @param path
     * @param uri
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
    private void initPictureCallback() {

        // Callback for picture saving:

        mPictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                // resume the camera again (this is necessary on the Nexus 5X, but not on the Samsung S5)
                mCamera.startPreview();
                requestPictureSave(data);

            }
        };

    }

    private void setupPhotoShootButtonCallback() {

        // Listener for photo shoot button:

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);

        photoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsPictureSafe) {
                            // get an image from the camera
                            takePicture();
                        }
                    }
                });

    }


//    /**
//     * This method is used to enable file saving in marshmallow (Android 6), since in this version
//     * file saving is not allowed without user permission.
//     */
//    private void requestFileSave() {
//
//        // Check Permissions Now
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // ask for permission:
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
//        }
//        else if (mPictureData != null)
//            savePicture(mPictureData);
//
//    }

    /**
     * Tells the camera to take a picture.
     */
    private void takePicture() {

        mIsPictureSafe = false;
        mPaintView.showFlicker();
        mCamera.takePicture(null, null, mPictureCallback);

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

    private void savePicture(byte[] data) {

        // Save the image in an own thread (AsyncTask):
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

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + IMG_FILENAME_PREFIX + timeStamp + ".jpg");

        Uri uri = Uri.fromFile(mediaFile);

        return uri;
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        mCameraPreview.setCamera(mCamera, mCameraInfo, displayRotation);

        ViewGroup appRoot = (ViewGroup) findViewById(R.id.main_layout);
        View f = findViewById(R.id.camera_controls_layout);
        appRoot.removeView(f);

//        LinearLayout l = (LinearLayout) findViewById(R.id.main_layout);
//        l.setOrientation(LinearLayout.HORIZONTAL);

        getLayoutInflater().inflate(R.layout.camera_controls_layout, appRoot);

        setupPhotoShootButtonCallback();
        initGalleryCallback();
        loadThumbnail();

//        View subview = getLayoutInflater().inflate(R.layout.activity_main, appRoot);

//
//        // remove old switcher, shutter and shutter icon
//        View cameraControlsView = findViewById(R.id.camera_controls_layout);
////        if (cameraControlsView != null)
//        appRoot.removeView(cameraControlsView);
////
//        RelativeLayout l = (RelativeLayout) findViewById(R.id.main_layout);
//        l.setOrientation(LinearLayout.HORIZONTAL);

//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        params.addRule(RelativeLayout.RIGHT_OF, R.id.camera_paint_fragment);
////
//
//        RelativeLayout r = (RelativeLayout) findViewById(R.id.camera_controls_layout);
//        r.setLayoutParams(params);

        // create new layout with the current orientation
//        LayoutInflater inflater = getLayoutInflater();
//        inflater.inflate(R.layout.ca, appRoot);

//        l.setLayoutParams(layoutParams);

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
    private void setupNavigationDrawer() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        NavigationView mDrawer = (NavigationView) findViewById(R.id.left_drawer);
        setupDrawerContent(mDrawer);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Menu menu = mDrawer.getMenu();
        MenuItem item = menu.findItem(R.id.action_show_debug_view);

        if (mIsDebugViewEnabled)
            item.setTitle(R.string.hide_debug_view_text);
        else
            item.setTitle(R.string.show_debug_view_text);

    }


    /**
     * Connects the items in the navigation drawer with a listener.
     *
     * @param navigationView
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

            // Text for focus measurement:
            case R.id.action_show_fm_values:

                if (mPaintView.isFocusTextVisible()) {
                    menuItem.setTitle(R.string.show_fm_values_text);
                    mPaintView.drawFocusText(false);
                } else {
                    menuItem.setTitle(R.string.hide_fm_values_text);
                    mPaintView.drawFocusText(true);
                }

                break;

            // Text for focus measurement:
            case R.id.action_show_guide:

                if (mPaintView.areGuideLinesDrawn()) {
                    mPaintView.drawGuideLines(false);
                    menuItem.setTitle(R.string.show_guide_text);
                } else {
                    mPaintView.drawGuideLines(true);
                    menuItem.setTitle(R.string.hide_guide_text);
                }

                break;


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


    /**
     * Initializes the camera in an own thread, so that the Camera.onPreviewFrame method is not
     * called on the UI thread.
     */

    private void openCameraThread() {

        if (mCamera == null) {

            if (mThread == null) {
                mThread = new CameraHandlerThread();
//            mThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            }

            synchronized (mThread) {
                mThread.openCamera();
            }

        }

    }

    /**
     * Initializes the camera.
     */
    private void initCamera() {

        // Open an instance of the first camera and retrieve its info.
        mCamera = getCameraInstance(CAMERA_ID);
        mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, mCameraInfo);

        // Get the rotation of the screen to adjust the preview image accordingly.
        mDisplayRotation = getWindowManager().getDefaultDisplay().getRotation();

        mIsPictureSafe = true;

    }


    // ================= start: CALLBACKS called from native files =================

    /**
     * Called after focus measurement is finished.
     *
     * @param patches
     */
    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (mCVResult != null)
            mCVResult.setPatches(patches);

    }

    /**
     * Called after page segmentation is finished.
     *
     * @param dkPolyRects
     */
    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        if (mCVResult != null)
            mCVResult.setDKPolyRects(dkPolyRects);


    }

    /**
     * Called after page segmentation is finished.
     *
     * @param value
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

//        mLiveViewLayout.setFrameDimension(width, height);

        View container = (View) findViewById(R.id.container_layout);


//        // This is necessary to resize the parent view (holding the camera preview and the paint view):
////        View container = (View) findViewById(R.id.container_layout);
//        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) container.getLayoutParams();
//
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//
//        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
//            p.height = height;
//        else
//            p.width = width;
//
//        container.setLayoutParams(p);
//
//
//        View paintView = (View) findViewById(R.id.paint_view);
//        FrameLayout.LayoutParams pf = (FrameLayout.LayoutParams) paintView.getLayoutParams();
//        pf.height = height;
//        paintView.setLayoutParams(pf);

    }

    @Override
    public void onFlashModesFound(List<String> modes) {

        mFlashModes = modes;
        if (mFlashPopupMenu != null)
            setupFlashPopup();

    }

    public void showFlashPopup(MenuItem item) {

        View menuItemView = findViewById(R.id.flash_mode_item);
        if (menuItemView == null)
            return;

        mFlashPopupMenu = new PopupMenu(this, menuItemView);
        if (mFlashPopupMenu == null)
            return;

        mFlashPopupMenu.setOnMenuItemClickListener(this);
        mFlashPopupMenu.inflate(R.menu.flash_mode_menu);
        mFlashPopupMenu.show();

    }

    private void setupFlashPopup() {

        if (mFlashPopupMenu == null)
            return;

        // TODO: Test this on Moto E:
        if (mFlashModes == null)
            mFlashMenuItem.setVisible(false);

        else {

            if (mFlashModes.size() == 1)
                mFlashMenuItem.setVisible(false);

            if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO))
                mFlashPopupMenu.getMenu().findItem(R.id.flash_auto_item).setVisible(false);

            if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_ON))
                mFlashPopupMenu.getMenu().findItem(R.id.flash_on_item).setVisible(false);

            if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
                mFlashPopupMenu.getMenu().findItem(R.id.flash_off_item).setVisible(false);

        }

    }

    public void showShootPopup(MenuItem item) {

        View menuItemView = findViewById(R.id.shoot_mode_item);
        if (menuItemView == null)
            return;
        PopupMenu popupMenu = new PopupMenu(this, menuItemView);
        if (popupMenu == null)
            return;
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.shoot_mode_menu);
        popupMenu.show();


    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manual_mode_item:
                mModeMenuItem.setIcon(mManualShootDrawable);
                mIsAutoMode = false;
                return true;
            case R.id.auto_mode_item:
                mModeMenuItem.setIcon(mAutoShootDrawable);
                mIsAutoMode = true;
                return true;
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

            default:
                return false;
        }
    }


//    public void showPopup(View v) {
//        PopupMenu popup = new PopupMenu(this, v);
//        MenuInflater inflater = popup.getMenuInflater();
//        inflater.inflate(R.menu.actions, popup.getMenu());
//        popup.show();
//    }

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

    }

    @Override
    public void onStatusChange(final int state) {

        // Check if we need the focus measurement at this point:
        if (state == CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED) {
            mCameraPreview.startFocusMeasurement(true);
        }
        // Check if we need the illumination measurement at this point:
        else if (state == CVResult.DOCUMENT_STATE_NO_ILLUMINATION_MEASURED) {
            mCameraPreview.setIlluminationRect(mCVResult.getDKPolyRects()[0]);
            mCameraPreview.startIllumination(true);
            mCVResult.setIsIlluminationComputed(true);

        } else if (state != CVResult.DOCUMENT_STATE_OK && state != CVResult.DOCUMENT_STATE_BAD_ILLUMINATION) {
            mCameraPreview.startIllumination(false);
            mCVResult.setIsIlluminationComputed(false);
            if (state != CVResult.DOCUMENT_STATE_UNSHARP) {
                mCameraPreview.startFocusMeasurement(false);
                mCVResult.setPatches(null);
            }
        }

        // Check if we need the counter text view:
        if (!mIsAutoMode || state != CVResult.DOCUMENT_STATE_OK) {
            runOnUiThread(new Runnable() {
            @Override
                public void run() {
                    mCounterView.setVisibility(View.INVISIBLE);
                }
            });
        }

        final String msg;

//        mCameraPreview.startFocusMeasurement(true);

        if (!mIsPictureSafe) {
            msg = getResources().getString(R.string.taking_picture_text);
        }

        else if (!mIsAutoMode || state != CVResult.DOCUMENT_STATE_OK) {
            mIsWaitingForCapture = false;
            msg = getInstructionMessage(state);
        }

        else {
            if (!mIsWaitingForCapture) {
                mStartTime = System.currentTimeMillis();
                mIsWaitingForCapture = true;
                msg = "";
                mPaintView.showCounter();

            }
            else {
                // Count down:
                long timePast = System.currentTimeMillis() - mStartTime;
                if (timePast < DOCUMENT_STEADY_TIME) {
                    final long timeLeft = DOCUMENT_STEADY_TIME - timePast;
                    msg = getResources().getString(R.string.dont_move_text);

                    final int counter = Math.round(timeLeft / 1000);
//                    if (counter > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCounterView.setVisibility(View.VISIBLE);
                                mCounterView.setText(Integer.toString(counter));
                            }
                        });
//                    }
//                    else {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mCounterView.setVisibility(View.INVISIBLE);
//                            }
//                        });
//                    }
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                mTextView.setText(msg);
            }
        });



    }

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



    private void loadThumbnail() {

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


        Bitmap thumbNailBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileName.toString()), 200, 200);
        if (thumbNailBitmap == null)
            return;

        // Determine the rotation angle of the image:
        int angle = -1;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            String attr = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            angle = getAngleFromExif(Integer.valueOf(attr));
        } catch (IOException e) {
            return;
        }

        //Rotate the image:
        Matrix mtx = new Matrix();
        mtx.setRotate(angle);
        thumbNailBitmap = Bitmap.createBitmap(thumbNailBitmap, 0, 0, thumbNailBitmap.getWidth(), thumbNailBitmap.getHeight(), mtx, true);

        // Update the gallery button:
        final BitmapDrawable thumbDrawable = new BitmapDrawable(getResources(), thumbNailBitmap);
        if (thumbDrawable == null)
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setGalleryButtonDrawable(thumbDrawable);
            }
        });

    }

    private void setGalleryButtonDrawable(Drawable drawable) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mGalleryButton.setBackground(drawable);
        else
            mGalleryButton.setBackgroundDrawable(drawable);
    }

//    @Override
//    public void onCameraLoaded(Camera camera, Camera.CameraInfo info, int displayRotation) {
//
//        mCamera = camera;
//        mCameraInfo = info;
//        // Get the rotation of the screen to adjust the preview image accordingly.
//        mDisplayRotation = displayRotation;
//
//        mIsPictureSafe = true;
//
//    }


    /**
     * Class extending HandlerThread, used to initialize the camera in an own thread.
     * Taken from: <a href="http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes/19154438#19154438}">stackoverflow</a>
     */
    private class CameraHandlerThread extends HandlerThread {

        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
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

            final File outFile = new File(uris[0].getPath());

            try {

                final RotateAnimation ranim = (RotateAnimation) AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_button_rotate);

                runOnUiThread(new Runnable() {
//
                    @Override
                    public void run() {
                        Drawable drawable = getResources().getDrawable(R.drawable.ic_gallery_busy);
                        setGalleryButtonDrawable(drawable);
                        mGalleryButton.setAnimation(ranim);
                    }

                });

                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(mData);

                fos.close();

                // Set exif orientation (avoid a real rotation of the image):
                // TODO: Test on more devices. Currently tested on: Nexus 5X, Moto E
                // TODO: check if we loose here other exif values
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

                                ranim.cancel();
                                setGalleryButtonDrawable(thumbDrawable);

//                                mGalleryButton.setScaleType(ImageView.ScaleType.FIT_START);
//                                mPaintView.showSpinner(false);
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


//        @Override
//        protected void onPostExecute(Void v) {
//
//            mPaintView.showSpinner(false);
//
//        }


    }




}