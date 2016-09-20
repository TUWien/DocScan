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
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * This is the main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */

public class CameraActivity extends AppCompatActivity implements TaskTimer.TimerCallbacks, NativeWrapper.CVCallback, CameraPreview.DimensionChangeCallback, MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "CameraActivity";
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
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
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private CameraHandlerThread mThread = null;
    private CVResult mCVResult;
    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
    private static boolean mIsDebugViewEnabled;
    private int mDisplayRotation;
    private static Context mContext;
    private int mCameraOrientation;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MediaScannerConnection mMediaScannerConnection;
    private boolean mIsPictureSafe;

    /**
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

        boolean init = OpenCVLoader.initDebug();

        if (init) {
            System.loadLibrary("docscan-native");
        }

    }

    // ================= start: methods from the Activity lifecyle =================

    /**
     * Creates the camera Activity.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        openCameraThread();

        if (mCamera == null || mCameraInfo == null) {
            // Camera is not available, display error message
            Toast.makeText(this, "Camera is not available.", Toast.LENGTH_SHORT).show();
            setContentView(R.layout.camera_unavailable_view);
        } else {

            initActivity();

        }

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

        mCVResult = new CVResult();

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);
        mCameraPreview.setCamera(mCamera, mCameraInfo, mDisplayRotation);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
        mPaintView.setCVResult(mCVResult);

        setupNavigationDrawer();

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);

        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;


        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        initGalleryCallback();
        initPictureCallback();


    }

    /**
     * Returns a boolean indicating if the screen is turned on. This method is necessary to prevent a
     * resume of the Activity if the display is turned off and the app is in landscape mode, because
     * this causes a resume of the app in portrait mode.
     * @return boolean indicating if the screen is on
     */
    // Taken from: http://stackoverflow.com/questions/2474367/how-can-i-tell-if-the-screen-is-on-in-android
    @SuppressWarnings("deprecation") // suppressed because the not deprecated function is called if API level >= 20
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

    /**
     * Returns a Camera object.
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
                if (isPermissionGiven)
                    takePicture();

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
     * Request to read the external storage. This method is used to enable file saving in marshmallow
     * (Android 6), since in this version external file opening is not allowed without user permission.
     */
    private void requestFileOpen() {

        // Check Permissions Now
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        }
        else
            startScan();

    }

    /**
     * Starts the MediaScanner.
     */
    private void startScan() {


        if(mMediaScannerConnection != null)
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
        }
        else if (files.length == 0) {
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




        }
        finally {
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

                // Save the image in an own thread (AsyncTask):
                Uri uri = getOutputMediaFile(getResources().getString(R.string.app_name));
                FileSaver fileSaver = new FileSaver(data);
                fileSaver.execute(uri);


            }
        };

        // Listener for photo shoot button:

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);

        photoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsPictureSafe) {
                            // get an image from the camera
                            requestFileSave();
                        }
                    }
                });

    }


    /**
     * This method is used to enable file saving in marshmallow (Android 6), since in this version
     * file saving is not allowed without user permission.
     */
    private void requestFileSave() {

        // Check Permissions Now
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
        else
            takePicture();

    }

    /**
     * Tells the camera to take a picture.
     */
    private void takePicture() {

        mIsPictureSafe = false;
        mCamera.takePicture(null, null, mPictureCallback);


    }

    /**
     * Returns the URI of a new file containing a time stamp.
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the filename.
     */
    private static Uri getOutputMediaFile(String appName){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.


        File mediaStorageDir = getMediaStorageDir(appName);

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + IMG_FILENAME_PREFIX + timeStamp + ".jpg");

        Uri uri = Uri.fromFile(mediaFile);

        return uri;
    }

    /**
     * Returns the path to the directory in which the images are saved.
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    private static File getMediaStorageDir(String appName) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){

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
     * @param menuItem ID of the selected item.
     */
    private void selectDrawerItem(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

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
                }
                else {
                    menuItem.setTitle(R.string.hide_fm_values_text);
                    mPaintView.drawFocusText(true);
                }

                break;

        }

        mDrawerLayout.closeDrawers();

    }

    // ================= end: methods for navigation drawer =================



    // ================= start: CALLBACKS invoking TaskTimer =================

    /**
     * Called before a task is executed. This is used to measure the time of the task execution.
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
     * @param patches
     */
    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (mCVResult != null)
            mCVResult.setPatches(patches);

    }

    /**
     * Called after page segmentation is finished.
     * @param dkPolyRects
     */
    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        if (mCVResult != null)
            mCVResult.setDKPolyRects(dkPolyRects);


    }

    // ================= end: CALLBACKS called from native files =================



    // =================  start: CameraPreview.DimensionChange CALLBACK =================

    /**
     * Called after the dimension of the camera view is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     * @param width width of the view
     * @param height height of the view
     */
    @Override
    public void onMeasuredDimensionChange(int width, int height) {

        mCVResult.setViewDimensions(width, height);

    }

    /**
     * Called after the dimension of the camera frame is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     * @param width width of the frame
     * @param height height of the frame
     * @param cameraOrientation orientation of the camera
     */
    @Override
    public void onFrameDimensionChange(int width, int height, int cameraOrientation) {

        mCameraOrientation = cameraOrientation;
        mCVResult.setFrameDimensions(width, height, cameraOrientation);

    }

    // =================  end: CameraPreview.DimensionChange CALLBACK =================





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

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Drawable drawable = getResources().getDrawable(R.drawable.ic_gallery_busy);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                            mGalleryButton.setBackground(drawable);
                        else
                            mGalleryButton.setBackgroundDrawable(drawable);

                        mGalleryButton.setScaleType(ImageView.ScaleType.FIT_START);
                    }
                });

                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(mData);

                fos.close();

                // Set exif orientation (avoid a real rotation of the image):
                // TODO: Test on more devices. Currently tested on: Nexus 5X, Moto E
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
//

                        if (exif != null) {

                            byte[] thumbData = exif.getThumbnail();
                            Bitmap thumbnail = BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length);

                            // Rotate the image according to the camera orientation - unfortunately it seems that the exif.getThumbnail is not using the orientation tag.

                            Matrix mtx = new Matrix();
                            mtx.setRotate(mCameraOrientation);

                            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), mtx, true);

                            final BitmapDrawable thumbDrawable = new BitmapDrawable(getResources(), thumbnail);

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                                        mGalleryButton.setBackground(thumbDrawable);
                                    else
                                        mGalleryButton.setBackgroundDrawable(thumbDrawable);

                                    mGalleryButton.setScaleType(ImageView.ScaleType.FIT_START);
                                }

                            });

                        }

                    }
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


    }




}