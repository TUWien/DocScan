package at.ac.tuwien.caa.docscan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 21.07.2016.
 */
public class CameraActivity extends AppCompatActivity implements TaskTimer.TimerCallbacks, NativeWrapper.CVCallback, CameraPreview.DimensionChangeCallback {

    private static final String TAG = "CameraActivity";
    private static String IMG_FILENAME_PREFIX = "IMG_";
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private Camera.PictureCallback mPictureCallback;

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
    private boolean mIsDebugViewEnabled;
    private int mDisplayRotation;
    private static Context mContext;
    private int mCameraOrientation;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

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


    @Override
    public void onPause() {

        super.onPause();
        // Stop camera access

        if (mPaintView != null)
            mPaintView.pause();

        mCameraPreview.pause();
        releaseCamera();



    }

    @Override
    public void onStop() {

        super.onStop();

        mCameraPreview.stop();

    }


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

    private void initActivity() {

        setContentView(R.layout.activity_main);

        mCVResult = new CVResult();

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);
        mCameraPreview.setCamera(mCamera, mCameraInfo, mDisplayRotation);

        mPaintView = (PaintView) findViewById(R.id.paint_view);
//        // TODO: the reference to the camera preview class is just needed to lock the mPaintView, think about a nicer solution:
//        mPaintView.setCameraPreview(mCameraPreview);
        mPaintView.setCVResult(mCVResult);

//        mCVResult.setDisplayRotation(mDisplayRotation);

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        initPictureCallback();


    }

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
        // TODO: not tested!
        else {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            return powerManager.isScreenOn();
        }


    }


    /** A safe way to get an instance of the Camera object. */
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



    private void initPictureCallback() {

        // Callback for picture saving:

        mPictureCallback = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutputMediaFile(getResources().getString(R.string.app_name));

                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions");
                    return;
                }

                try {

                    FileOutputStream fos = new FileOutputStream(pictureFile);

                    Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                    image = rotate(image, mCameraOrientation);
                    image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();

//                    Finally tell the MediaScannerConnection that a new file has been saved.
//                    This is necessary, since the Android system will not detect the image in time,
//                    and hence it is not visible in the gallery for example.
                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{pictureFile.toString()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {

                                public void onScanCompleted(String path, Uri uri) {


                                }
                            });

                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }


            }
        };

        // Listener for photo shoot button:

        ImageButton photoButton = (ImageButton) findViewById(R.id.photo_button);

        photoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        requestFileSave();
                    }
                });

    }

    // ================= start: methods for file handling =================


    // This method is used to enable file saving in marshmallow (Android 6), since in this version file saving is not allowed without user permission:
    private void requestFileSave() {

        // Check Permissions Now
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
        else
            takePicture();

    }

    private void takePicture() {

        mCamera.takePicture(null, null, mPictureCallback);


    }



    private static File getOutputMediaFile(String appName){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.


        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appName);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + IMG_FILENAME_PREFIX + timeStamp + ".jpg");


        return mediaFile;
    }


    // ================= end: methods for file handling =================



    // Taken from http://stackoverflow.com/questions/15808719/controlling-the-camera-to-take-pictures-in-portrait-doesnt-rotate-the-final-ima:
    private Bitmap rotate(Bitmap bitmap, int degree) {

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);

    }


    // ================= start: CALLBACKS invoking TaskTimer =================

    @Override
    public void onTimerStarted(int senderId) {

        if (mTaskTimer == null)
            return;

        mTaskTimer.startTaskTimer(senderId);


    }

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

    // Do this in an own thread, so that the onPreviewFrame method is not called on the UI thread
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

    private void initCamera() {

        // Open an instance of the first camera and retrieve its info.
        mCamera = getCameraInstance(CAMERA_ID);
        mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, mCameraInfo);

        // Get the rotation of the screen to adjust the preview image accordingly.
        mDisplayRotation = getWindowManager().getDefaultDisplay().getRotation();


    }


    // ================= start: CALLBACKS called from native files =================

    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (mCVResult != null)
            mCVResult.setPatches(patches);

    }

    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        if (mCVResult != null)
            mCVResult.setDKPolyRects(dkPolyRects);


    }

    // ================= end: CALLBACKS called from native files =================



    // =================  start: CameraPreview.DimensionChange CALLBACK =================

    @Override
    public void onMeasuredDimensionChange(int width, int height) {

        mCVResult.setViewDimensions(width, height);

    }

    @Override
    public void onFrameDimensionChange(int width, int height, int cameraOrientation) {

        mCameraOrientation = cameraOrientation;
        mCVResult.setFrameDimensions(width, height, cameraOrientation);

    }

    // =================  end: CameraPreview.DimensionChange CALLBACK =================



    // Taken from: http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes/19154438#19154438
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



}