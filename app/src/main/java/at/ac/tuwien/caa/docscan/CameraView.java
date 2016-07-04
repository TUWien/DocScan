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
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 16.06.2016.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, OverlayView.SizeUpdate
{

    public static final String DEBUG_TAG = "[Camera View]";

    class CameraFrameThread extends Thread {

        private CameraView mCameraView;
        private boolean mIsRunning;


        public CameraFrameThread(CameraView cameraView) {

            mCameraView = cameraView;

        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }

//        public void setSurfaceSize(int width, int height) {
//
//            mSurfaceWidth = width;
//            mSurfaceHeight = height;
//
//        }

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

//                    LinkedList<Mat> ch = new LinkedList<Mat>();
//                    Core.split(rgbMat, ch);
//
//                    Core.MinMaxLocResult mm = Core.minMaxLoc(ch.getFirst());
//
//                    //Log.d(DEBUG_TAG, "buffer size: " + frame.length + " expected: " + mFrameWidth*mFrameHeight);
//                    //Log.d(DEBUG_TAG, "size: " + mFrameWidth + " x " + mFrameHeight);
//                    Log.d(DEBUG_TAG, "range: [" + mm.minVal + " " + mm.maxVal + "]");

                    Patch[] patches = NativeWrapper.getFocusMeasures(rgbMat);
                    mCVCallback.onFocusMeasured(patches);

                }

//                }

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

    private static String TAG = "CameraView";

    public CameraView(Context context, AttributeSet attrs) {


        super(context, attrs);

        mCVCallback = (NativeWrapper.CVCallback) context;

        mHolder = getHolder();
        mHolder.addCallback(this);

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

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> cameraSizes = params.getSupportedPreviewSizes();

        Camera.Size bestSize = getBestFittingSize(cameraSizes, width, height);

        mFrameWidth = bestSize.width;
        mFrameHeight = bestSize.height;

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        params.setPreviewSize(mFrameWidth, mFrameHeight);

        mCamera.setParameters(params);

        MainActivity.setCameraDisplayOrientation(mCamera);

        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
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

//    public void setMeasuredSize(int width, height) {
//
//        setMeasuredDimension(width, height);
//
//    }

//    // Scales the camera view so that the preview has original width to height ratio:
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        if (0 == mFrameHeight || 0 == mFrameWidth) {
//            setMeasuredDimension(width, height);
//        } else {
//
//
//            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!
//            // Portrait mode:
//            if (width < height) {
//                double resizeFac = (double) width / mFrameHeight;
//                int scaledHeight = (int) Math.round(mFrameWidth * resizeFac);
//                if (scaledHeight > height)
//                    scaledHeight = height;
//                setMeasuredDimension(width, scaledHeight);
//            }
//            // Landscape mode:
//            else {
//                double resizeFac = (double) height / mFrameHeight;
//                int scaledWidth = (int) Math.round(mFrameWidth * resizeFac);
//                if (scaledWidth > width)
//                    scaledWidth = width;
//                setMeasuredDimension(scaledWidth, height);
//            }
//
//        }
//    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        mCamera = Camera.open();
        try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mHolder = arg0;
            mCamera.setPreviewCallback(this);
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }

        mCameraFrameThread = new CameraFrameThread(this);

        mCameraFrameThread.setRunning(true);
        Thread.State state = mCameraFrameThread.getState();
        // TODO: check why the thread is already started - if the app is restarted. The thread should be dead!
        if (mCameraFrameThread.getState() == Thread.State.NEW)
            mCameraFrameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {

        boolean retry = true;
        mCameraFrameThread.setRunning(false);
        while (retry) {
            try {
                mCameraFrameThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

        Thread.State state = mCameraFrameThread.getState();

        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
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