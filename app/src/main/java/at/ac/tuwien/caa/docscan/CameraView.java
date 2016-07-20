/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   16. June 2016
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

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, OverlayView.SizeUpdate
{



    private Camera mCamera = null;
    private int mFrameWidth;
    private int mFrameHeight;
    private byte[] mFrame;

    private NativeWrapper.CVCallback mCVCallback;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private SurfaceHolder mHolder;
    private int mSurfaceWidth, mSurfaceHeight;
    private boolean mIsSurfaceReady, mIsPermissionGiven;

    private long mLastTime;

    private static long FRAME_TIME_DIFF = 300;

    private CameraView mCameraView;
    private Mat mFrameMat;
    private PageSegmentationThread mPageSegmentationThread;
    private FocusMeasurementThread mFocusMeasurementThread;


    private static String TAG = "CameraView";

    public CameraView(Context context, AttributeSet attrs) {


        super(context, attrs);

        mCVCallback = (NativeWrapper.CVCallback) context;
        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;

        mHolder = getHolder();
        mHolder.addCallback(this);

        mIsSurfaceReady = false;
        mIsPermissionGiven = false;


    }

    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {

        if (MainActivity.isDebugViewEnabled()) {

            // Take care that in this case the timer is first stopped (contrary to the other calls):
            mTimerCallbacks.onTimerStopped(TaskTimer.CAMERA_FRAME_ID);
            mTimerCallbacks.onTimerStarted(TaskTimer.CAMERA_FRAME_ID);

        }


        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

            synchronized (mCameraView) {

                // 1.5 since YUV
                Mat yuv = new Mat((int)(mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
                yuv.put(0, 0, pixels);


                mFrameMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
                Imgproc.cvtColor(yuv, mFrameMat, Imgproc.COLOR_YUV2RGB_NV21);

                mLastTime = currentTime;
                mCameraView.notify();

            }

        }

    }

    public void resume() {

        if (mIsPermissionGiven && mCamera == null) {

            if (mIsSurfaceReady)
                openCameraThread();

        }

    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mHolder = holder;

        mIsSurfaceReady = true;

        if (mIsPermissionGiven && mCamera == null) {

            openCameraThread();

        }

    }

    // Do this in an own thread, so that the onPreviewFrame method is not called on the UI thread
    private void openCameraThread() {

        if (mThread == null) {
            mThread = new CameraHandlerThread();
//            mThread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        }

        synchronized (mThread) {
            mThread.openCamera();
        }

    }


    private Camera.Size getBestFittingSize(List<Camera.Size> cameraSizes, int width, int height) {

        Camera.Size bestSize = null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        double minDiff = Double.MAX_VALUE;

        int targetHeight;
        if (width < height)
            targetHeight = height;
        else
            targetHeight = width;

        for (Camera.Size size : cameraSizes) {
            double ratio;
            if (width < height)
                ratio = (double) size.width / size.height;
            else
                ratio = (double) size.height / size.width;

            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                bestSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (bestSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : cameraSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    bestSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return bestSize;

    }

    public void setMeasuredSize(int width, int height) {

        setMeasuredDimension(width, height);

    }

    public void giveCameraPermission() {

        mIsPermissionGiven = true;

        if (mIsSurfaceReady && mCamera == null)
            openCameraThread();
    }

    private void initCamera() {

        Log.d(TAG, "initializing camera...");

        if (mCamera == null)
            mCamera = Camera.open();

        try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);

        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }


        mCameraView = this;

        Log.d(TAG, "initializing threads...");

        // TODO: check why the thread is already started - if the app is restarted. The thread should be dead!
        mPageSegmentationThread = new PageSegmentationThread(mCameraView);
        if (mPageSegmentationThread.getState() == Thread.State.NEW)
            mPageSegmentationThread.start();

        mFocusMeasurementThread = new FocusMeasurementThread(mCameraView);
        if (mFocusMeasurementThread.getState() == Thread.State.NEW)
            mFocusMeasurementThread.start();


        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> cameraSizes = params.getSupportedPreviewSizes();

        Camera.Size bestSize = getBestFittingSize(cameraSizes, mSurfaceWidth, mSurfaceHeight);

        mFrameWidth = bestSize.width;
        mFrameHeight = bestSize.height;

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        params.setPreviewSize(mFrameWidth, mFrameHeight);

        mCamera.setParameters(params);

        // Determine the preview orientation. Note that the frame size in onPreviewFrame is not changed by this!
        int cameraDisplayOrientation = MainActivity.getCameraDisplayOrientation();
        mCamera.setDisplayOrientation(cameraDisplayOrientation);
        mCamera.getParameters().setRotation(cameraDisplayOrientation);

        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        Log.d(TAG, "camera started");

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mHolder = holder;

    }

    public void pause() {

        // Stop the threads safely:
        stopThread(mPageSegmentationThread);
        stopThread(mFocusMeasurementThread);

        if (mCamera != null) {

            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }

        mIsSurfaceReady = false;

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {


    }

    private void stopThread(CVThread thread) {

        if (thread != null) {

            boolean retry = true;

            thread.setRunning(false);
            while (retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }

        }

    }

    public int getFrameWidth() {

        return mFrameWidth;

    }

    public int getFrameHeight() {

        return mFrameHeight;

    }


    public abstract class CVThread extends Thread {

        private CameraView mCameraView;
        protected boolean mIsRunning = true;

        protected abstract void execute();

        public CVThread() {

        }

        public CVThread(CameraView cameraView) {



            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mCameraView = cameraView;

        }

        @Override
        public void run() {

            synchronized (mCameraView) {

                while (mIsRunning) {


                        try {

                            mCameraView.wait();

                            execute();

                        } catch (InterruptedException e) {

                        }

                    }

                }
        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }
    }

    public class PageSegmentationThread extends CVThread {

        public PageSegmentationThread(CameraView cameraView) {
            super(cameraView);
        }

        protected void execute() {

            if (mIsRunning) {

                // Measure the time if required:
                if (MainActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStarted(TaskTimer.PAGE_SEGMENTATION_ID);

                DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mFrameMat);

                if (MainActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.PAGE_SEGMENTATION_ID);

                mCVCallback.onPageSegmented(polyRects);


            }
        }
    }

    private class FocusMeasurementThread extends CVThread {

        public FocusMeasurementThread(CameraView cameraView) {
            super(cameraView);
        }


        protected void execute() {

            if (mIsRunning) {

                // Measure the time if required:
                if (MainActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStarted(TaskTimer.FOCUS_MEASURE_ID);

                Patch[] patches = NativeWrapper.getFocusMeasures(mFrameMat);

                if (MainActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.FOCUS_MEASURE_ID);



                if (MainActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.FOCUS_MEASURE_ID);

                mCVCallback.onFocusMeasured(patches);


            }
        }
    }



    // Taken from: http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes/19154438#19154438
    private CameraHandlerThread mThread = null;
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