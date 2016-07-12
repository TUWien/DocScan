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
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
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

/**
 * Created by fabian on 16.06.2016.
 */

// TODO: check out this thread: http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes/19154438#19154438

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, OverlayView.SizeUpdate
{

    public static final String DEBUG_TAG = "[Camera View]";

    class CameraFrameThread extends Thread {

        private CameraView mCameraView;
        private boolean mIsRunning;
        private boolean mIsFocusMeasured = true;
        private boolean mIsPageSegmented = true;


        public CameraFrameThread(CameraView cameraView) {

            mCameraView = cameraView;

        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }

        @Override
        public void run() {

            byte[] frame;


            while (mIsRunning) {
                // TODO: I am not sure if this access should be synchronized, but I guess:
                //synchronized (mCameraView) {
                    frame = mCameraView.getFrame();
                //}
                if (frame != null) {

                    // 1.5 since YUV
                    Mat yuv = new Mat((int)(mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
                    yuv.put(0, 0, frame);

                    Mat rgbMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
                    Imgproc.cvtColor(yuv, rgbMat, Imgproc.COLOR_YUV2RGB_NV21);

                    if (mIsFocusMeasured) {
                        Patch[] patches = NativeWrapper.getFocusMeasures(rgbMat);
                        mCVCallback.onFocusMeasured(patches);
                    }

                    if (mIsPageSegmented) {
                        DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(rgbMat);
                        mCVCallback.onPageSegmented(polyRects);
                        if (polyRects.length > 0)
                            if (polyRects[0] != null)
                                Log.d(TAG, "rects: " + polyRects[0].getPoints());
                    }

                }

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

    private Camera mCamera = null;
    private int mFrameWidth;
    private int mFrameHeight;
    private byte[] mFrame;
    private CameraFrameThread mCameraFrameThread;
    private NativeWrapper.CVCallback mCVCallback;
    private SurfaceHolder mHolder;
    private int mSurfaceWidth, mSurfaceHeight;
    private boolean mIsSurfaceReady, mIsPermissionGiven;

    private static String TAG = "CameraView";

    public CameraView(Context context, AttributeSet attrs) {


        super(context, attrs);

        mCVCallback = (NativeWrapper.CVCallback) context;

        mHolder = getHolder();
        mHolder.addCallback(this);

        mIsSurfaceReady = false;
        mIsPermissionGiven = false;

        mCameraFrameThread = null;

    }

    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {
        mFrame = pixels;

    }

    public void onPause()
    {

        mCamera.stopPreview();
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

//

    }

    private void openCameraThread() {

        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }

    }


    private Camera.Size getBestFittingSize(List<Camera.Size> cameraSizes, int width, int height) {

        Camera.Size bestSize = null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)height / width;

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

        mCameraFrameThread = new CameraFrameThread(this);

        mCameraFrameThread.setRunning(true);

        // TODO: check why the thread is already started - if the app is restarted. The thread should be dead!
        if (mCameraFrameThread.getState() == Thread.State.NEW)
            mCameraFrameThread.start();


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

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mHolder = holder;

    }


    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {

        if (mCameraFrameThread != null) {

            boolean retry = true;
            mCameraFrameThread.setRunning(false);
            while (retry) {
                try {
                    mCameraFrameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }


        }

        if (mCamera != null) {

            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }
    }

    public int getFrameWidth() {

        return mFrameWidth;

    }

    public int getFrameHeight() {

        return mFrameHeight;

    }

    protected byte[] getFrame() {

        return mFrame;

    }



}