package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 21.07.2016.
 */
public class CameraActivity extends AppCompatActivity implements TaskTimer.TimerCallbacks, NativeWrapper.CVCallback {

    private static final String TAG = "CameraActivity";

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

    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
    private boolean mIsDebugViewEnabled;
    private int mDisplayRotation;
    private static Context mContext;

    /**
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

        boolean init = OpenCVLoader.initDebug();

        if (init) {
            System.loadLibrary("docscan-native");
        }

    }

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

    private void initActivity() {

        setContentView(R.layout.activity_main);

        mCameraPreview = (CameraPreview) findViewById(R.id.camera_view);
        mCameraPreview.setCamera(mCamera, mCameraInfo, mDisplayRotation);

//        mPaintView = (PaintView) findViewById(R.id.paint_view);

        // This is used to mea'sure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

    }

    @Override
    public void onPause() {

        super.onPause();
        // Stop camera access
        releaseCamera();

        if (mPaintView != null)
            mPaintView.pause();

    }

    @Override
    public void onStop() {

        super.onStop();

        boolean b = isChangingConfigurations();

        mCameraPreview.stop();

    }

    // Taken from: http://stackoverflow.com/questions/2474367/how-can-i-tell-if-the-screen-is-on-in-android
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

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "On Restart .....");

        boolean isOrientationChanged = isChangingConfigurations();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

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

        if (mPaintView != null)
            mPaintView.setFocusPatches(patches);

    }

    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        if (mPaintView != null)
            mPaintView.setDkPolyRects(dkPolyRects);

    }

    // ================= end: CALLBACKS called from native files =================



    // ================= end: CALLBACKS invoking TaskTimer =================


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