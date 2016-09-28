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

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Class for showing the camera preview. This class is extending SurfaceView and making use of the
 * Camera API. The received frames are converted to OpenCV Mat's in a fixed time interval. The Mat
 * is used by two thread classes (inner classes): FocusMeasurementThread and PageSegmentationThread.
 */
public class CameraPreview  extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    private static final int MAX_MAT_WIDTH = 1000;
    private static final int MAX_MAT_HEIGHT = 1000;
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mDisplayOrientation;
    private int mSurfaceWidth, mSurfaceHeight;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private NativeWrapper.CVCallback mCVCallback;
    private DimensionChangeCallback mDimensionChangeCallback;

    private PageSegmentationThread mPageSegmentationThread;
    private FocusMeasurementThread mFocusMeasurementThread;

    // Mat used by mPageSegmentationThread and mFocusMeasurementThread:
    private Mat mFrameMat;
    private int mMatWidth, mMatHeight;

    private int mFrameWidth;
    private int mFrameHeight;

    private long mLastTime;

    // Used for generating mFrameMat at a 'fixed' frequency:
    private static long FRAME_TIME_DIFF = 300;

    // Used for the size of the auto focus area:
    private static final int FOCUS_HALF_AREA = 1000;

    private boolean isCameraInitialized;


//    public CameraPreview(Context context) {
//
//        super(context);
//
//        // Install a SurfaceHolder.Callback so we get notified when the
//        // underlying surface is created and destroyed.
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//
//        isCameraInitialized = false;
//
//
//    }

    /**
     * Creates the CameraPreview and the callbacks required to send events to the activity.
     * @param context
     * @param attrs
     */
    public CameraPreview(Context context, AttributeSet attrs) {

        super(context, attrs);

        mHolder = getHolder();
        mHolder.addCallback(this);

        mCVCallback = (NativeWrapper.CVCallback) context;
        mDimensionChangeCallback = (DimensionChangeCallback) context;

        // used for debugging:
        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;

    }


    public void stop() {

        isCameraInitialized = false;

    }

    /**
     * Initializes the camera.
     * @param camera camera object
     * @param cameraInfo camera information
     * @param displayOrientation orientation of the display in degrees
     */
    public void setCamera(Camera camera, Camera.CameraInfo cameraInfo, int displayOrientation) {

        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDisplayOrientation = displayOrientation;
        mCamera.setPreviewCallback(this);

    }


    /**
     * Called after the surface is created. Here nothing is done, because surfaceChanged is called
     * afterwards.
     * @param holder
     */
    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    /**
     * Called once the surface is changed. This happens after orientation changes or if the activity
     * is started (or resumed).
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


        // Check that the screen is on, because surfaceChanged is also called after the screen is turned off in landscape mode.
        // (Because the activity is resumed in such cases)
        if (mCamera != null && CameraActivity.isScreenOn())
            initCamera(width, height);

    }

    /**
     * Called after the activity is resumed.
     */
    public void resume() {


        if (!isCameraInitialized)
            initCamera(mSurfaceWidth, mSurfaceHeight);

    }


    public void pause() {

        isCameraInitialized = false;

    }


    /**
     * Called after the user touches the view in order to make an auto focus. The auto focus region
     * is set device independent by using Camera.Area.
     * @see <a href="https://developer.android.com/reference/android/hardware/Camera.Area.html">Camera.Area</a>
     * @param event a touch event
     * @return a boolean indicating if the event has been processed
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {


        if (mCamera == null)
            return true;

        // We wait until the finger is up again:
        if (event.getAction() != MotionEvent.ACTION_UP)
            return true;


        float touchX = event.getX();
        float touchY = event.getY();
        PointF touchScreen = new PointF(touchX, touchY);

        // The camera field of view is normalized so that -1000,-1000 is top left and 1000, 1000 is
        // bottom right. Not that multiple areas are possible, but currently only one is used.

        float focusRectHalfSize = .2f;

        // Normalize the coordinates of the touch event:
        float centerX = getMeasuredWidth() / 2;
        float centerY = getMeasuredHeight() / 2;
        PointF centerScreen = new PointF(centerX, centerY);


        Point upperLeft = transformPoint(centerScreen, touchScreen, -focusRectHalfSize, -focusRectHalfSize);
        Point lowerRight = transformPoint(centerScreen, touchScreen, focusRectHalfSize, focusRectHalfSize);

        Rect focusRect = new Rect(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);

        Camera.Area focusArea = new Camera.Area(focusRect, 750);
        List<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(focusArea);

        mCamera.cancelAutoFocus();

        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        if (parameters.getMaxNumFocusAreas() > 0) {

            parameters.setFocusAreas(focusAreas);
//            mCamera.setParameters(parameters);
        }

        mCamera.setParameters(parameters);
//        mCamera.startPreview();

        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {

                camera.cancelAutoFocus();

                if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) {
//
//
////                        mCamera.stopPreview();
                    Camera.Parameters parameters = camera.getParameters();
                    mCamera.autoFocus(null);
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//                    if (parameters.getMaxNumFocusAreas() > 0) {
//                        parameters.setFocusAreas(null);
//                    }
                    mCamera.setParameters(parameters);

                }
            }

        });

        return true; //processed

    }

    /**
     * Transforms screen coordinates to the camera field of view
     * @param centerScreen
     * @param touchScreen
     * @param offSetX
     * @param offSetY
     * @return
     */
    private Point transformPoint(PointF centerScreen, PointF touchScreen, float offSetX, float offSetY) {

        // Translate the point:
        PointF normalizedPoint = new PointF(touchScreen.x, touchScreen.y);
//        normalizedPoint.offset(offSetX, offSetY);

        normalizedPoint.offset(-centerScreen.x, -centerScreen.y);

        // Scale the point between -1 and 1:
        normalizedPoint.x = normalizedPoint.x / centerScreen.x;
        normalizedPoint.y = normalizedPoint.y / centerScreen.y;

        normalizedPoint.offset(offSetX, offSetY);

        // Scale the point between -1000 and 1000:
        normalizedPoint.x = normalizedPoint.x * FOCUS_HALF_AREA;
        normalizedPoint.y = normalizedPoint.y * FOCUS_HALF_AREA;

        // Clamp the values if necessary:
        if (normalizedPoint.x < -FOCUS_HALF_AREA)
            normalizedPoint.x = -FOCUS_HALF_AREA;
        else if (normalizedPoint.x > FOCUS_HALF_AREA)
            normalizedPoint.x = FOCUS_HALF_AREA;

        if (normalizedPoint.y < -FOCUS_HALF_AREA)
            normalizedPoint.y = -FOCUS_HALF_AREA;
        else if (normalizedPoint.y > FOCUS_HALF_AREA)
            normalizedPoint.y = FOCUS_HALF_AREA;

        Point transformedPoint = new Point(Math.round(normalizedPoint.x), Math.round(normalizedPoint.y));

        return transformedPoint;

    }


    /**
     * Initializes the camera with the preview size and the orientation. Starts the page segmentation
     * and the focus measurement threads.
     * @param width width of the surface
     * @param height height of the surface
     */
    private void initCamera(int width, int height) {

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist");
            return;
        }

        isCameraInitialized = true;

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Log.d(TAG, "Preview stopped.");
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        mSurfaceWidth = width;
        mSurfaceHeight = height;

        int orientation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(orientation);


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

        mPageSegmentationThread = new PageSegmentationThread(this);
        if (mPageSegmentationThread.getState() == Thread.State.NEW)
            mPageSegmentationThread.start();

        mFocusMeasurementThread = new FocusMeasurementThread(this);
        if (mFocusMeasurementThread.getState() == Thread.State.NEW)
            mFocusMeasurementThread.start();


        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();

            Log.d(TAG, "Camera preview started.");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        // Determine the size of the Mat used for page segmentation and focus measure:
        if (mFrameWidth > MAX_MAT_WIDTH)
            mMatWidth = MAX_MAT_WIDTH;
        else
            mMatWidth = mFrameWidth;

        if (mFrameHeight > MAX_MAT_HEIGHT)
            mMatHeight = MAX_MAT_HEIGHT;
        else
            mMatHeight = mFrameHeight;

        // Tell the dependent Activity that the frame dimension (might have) change:
//        mDimensionChangeCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);
        mDimensionChangeCallback.onFrameDimensionChange(mMatWidth, mMatHeight, orientation);


    }

    /**
     * Called after the preview received a new frame (as byte array).
     * @param pixels byte array containgin the frame.
     * @param camera camera
     */
    @Override
    public void onPreviewFrame(byte[] pixels, Camera camera)
    {

        if (CameraActivity.isDebugViewEnabled()) {

            // Take care that in this case the timer is first stopped (contrary to the other calls):
            mTimerCallbacks.onTimerStopped(TaskTimer.CAMERA_FRAME_ID);
            mTimerCallbacks.onTimerStarted(TaskTimer.CAMERA_FRAME_ID);

        }

//        Log.d(TAG, "frame received.");
        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

            // TODO: check if the threads are still in their execution state and wait until they have finished.
            synchronized (this) {

                // Measure the time if required:
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStarted(TaskTimer.MAT_CONVERSION_ID);


                // 1.5 since YUV
                Mat yuv = new Mat((int)(mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
                yuv.put(0, 0, pixels);


                mFrameMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
                Imgproc.cvtColor(yuv, mFrameMat, Imgproc.COLOR_YUV2RGB_NV21);

                Size s = new Size(mMatWidth, mMatHeight);
                Imgproc.resize(mFrameMat, mFrameMat, s);

                yuv.release();

                // Measure the time if required:
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.MAT_CONVERSION_ID);


                mLastTime = currentTime;
                this.notify();


            }

        }



    }


    /**
     * Returns the preview size that fits best into the surface view.
     * @param cameraSizes possible frame sizes (camera dependent)
     * @param width width of the surface view
     * @param height height of the surface view
     * @return best fitting size
     */
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

    /**
     * Calculate the correct orientation for a {@link Camera} preview that is displayed on screen.
     * Implementation is based on the sample code provided in
     * {@link Camera#setDisplayOrientation(int)}.
     */
    public static int calculatePreviewOrientation(Camera.CameraInfo info, int rotation) {

        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    /**
     * Scales the camera view so that the preview has original width to height ratio, this is
     * necessary to avoid a stretching of the camera preview. If this function is not used, the
     * size of the camera preview frames is equal to the available space (defined by the view).
     *
     * @see <a href="https://developer.android.com/reference/android/view/View.html#onMeasure(int,%20int)>onMeasure</a>
     *
     * @param widthMeasureSpec  horizontal space requirements as imposed by the parent. The requirements are encoded with View.MeasureSpec
     * @param heightMeasureSpec vertical space requirements as imposed by the parent. The requirements are encoded with View.MeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int frameWidth = mFrameWidth;
        int frameHeight = mFrameHeight;

        if (0 == frameHeight || 0 == frameWidth) {
//            setChildMeasuredDimension(width, height);
            setMeasuredDimension(width, height);
        } else {


            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!
            // Portrait mode:
            if (width < height) {

                double resizeFac = (double) width / frameHeight;
                int scaledHeight = (int) Math.round(frameWidth * resizeFac);
                if (scaledHeight > height)
                    scaledHeight = height;
                setMeasuredDimension(width, scaledHeight);

            }
            // Landscape mode:
            else {
                double resizeFac = (double) height / frameHeight;
                int scaledWidth = (int) Math.round(frameWidth * resizeFac);
                if (scaledWidth > width)
                    scaledWidth = width;
                setMeasuredDimension(scaledWidth, height);

            }

        }

        // Finally tell the dependent Activity the dimension has changed:
        mDimensionChangeCallback.onMeasuredDimensionChange(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    /**
     * Interfaces used to tell the activity that a dimension is changed. This is used to enable
     * a conversion between frame and screen coordinates (necessary for drawing in PaintView).
     */
    public interface DimensionChangeCallback {

        void onMeasuredDimensionChange(int width, int height);
        void onFrameDimensionChange(int width, int height, int cameraOrientation);

    }



    // ================= start: CVThread and subclasses =================


    /**
     * Abstract class extending a thread, concerned with computer vision tasks. Note that this class
     * and its child are connected to the CameraPreview, so that the tasks are only executed in case
     * of updates of the mFrameMat.
     */
    public abstract class CVThread extends Thread {

        protected CameraPreview mCameraView;
        protected boolean mIsRunning = true;

        protected abstract void execute();

        public CVThread() {

        }

        public CVThread(CameraPreview cameraView) {


            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mCameraView = cameraView;

        }

        /**
         * The main loop of the thread.
         */
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

    /**
     * Class responsible for calling the native method for page detection.
     */
    public class PageSegmentationThread extends CVThread {

        public PageSegmentationThread(CameraPreview cameraView) {

            super(cameraView);

        }

        protected void execute() {

            if (mIsRunning) {

                // Measure the time if required:
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStarted(TaskTimer.PAGE_SEGMENTATION_ID);



                    DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mFrameMat);

                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.PAGE_SEGMENTATION_ID);

                    mCVCallback.onPageSegmented(polyRects);





            }
        }
    }

    /**
     * Class responsible for calling the native method for focus measurement.
     */
    class FocusMeasurementThread extends CVThread {

        public FocusMeasurementThread(CameraPreview cameraView) {
            super(cameraView);
        }


        protected void execute() {

            if (mIsRunning) {

//                // Measure the time if required:
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStarted(TaskTimer.FOCUS_MEASURE_ID);

                Patch[] patches = NativeWrapper.getFocusMeasures(mFrameMat);

                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.FOCUS_MEASURE_ID);

                mCVCallback.onFocusMeasured(patches);


            }
        }
    }


    // ================= end: CVThread and subclasses =================




}