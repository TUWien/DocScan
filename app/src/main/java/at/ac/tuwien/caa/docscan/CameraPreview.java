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
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 21.07.2016.
 */
public class CameraPreview  extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {{}

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

    private Mat mFrameMat;

    private int mFrameWidth;
    private int mFrameHeight;

    private long mLastTime;
    private static long FRAME_TIME_DIFF = 300;

    private static final int FOCUS_HALF_AREA = 1000;

    private boolean isCameraInitialized, mIsCameraPreviewStarted;

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        isCameraInitialized = false;
        mIsCameraPreviewStarted = false;

    }


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

    public void setCamera(Camera camera, Camera.CameraInfo cameraInfo, int displayOrientation) {

        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDisplayOrientation = displayOrientation;
        mCamera.setPreviewCallback(this);

    }


    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
//        try {
//            mCamera.setPreviewDisplay(holder);
//            mCamera.startPreview();
//            Log.d(TAG, "Camera preview started.");
//        } catch (IOException e) {
//            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
//        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }



    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


        // Check that the screen is on, because surfaceChanged is also called after the screen is turned off in landscape mode.
        // (Because the activity is resumed in such cases)
        if (mCamera != null && CameraActivity.isScreenOn())
            initCamera(width, height);

    }

    public void resume() {


        if (!isCameraInitialized)
            initCamera(mSurfaceWidth, mSurfaceHeight);

    }

    public void pause() {

        isCameraInitialized = false;

    }



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


//        Camera.Parameters params = mCamera.getParameters();

//        params.setFocusAreas(focusAreas);
//
//        mCamera.setParameters(params);

//        if (mCamera != null) {

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
//                    mCamera.startPreview();
                }
            }

        });

        return true; //processed

    }

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

//            mCamera.autoFocus(this);
            mIsCameraPreviewStarted = true;
            Log.d(TAG, "Camera preview started.");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        // Tell the dependent Activity that the frame dimension (might have) change:
        mDimensionChangeCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);


    }


    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {

        if (CameraActivity.isDebugViewEnabled()) {

            // Take care that in this case the timer is first stopped (contrary to the other calls):
            mTimerCallbacks.onTimerStopped(TaskTimer.CAMERA_FRAME_ID);
            mTimerCallbacks.onTimerStarted(TaskTimer.CAMERA_FRAME_ID);

        }

        Log.d(TAG, "frame received.");
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

                yuv.release();

                // Measure the time if required:
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.MAT_CONVERSION_ID);


                mLastTime = currentTime;
                this.notify();


            }

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

    /**
     * Calculate the correct orientation for a {@link Camera} preview that is displayed on screen.
     *
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

    // Scales the camera view so that the preview has original width to height ratio:
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

    public interface DimensionChangeCallback {

        void onMeasuredDimensionChange(int width, int height);
        void onFrameDimensionChange(int width, int height, int cameraOrientation);

    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }


    // ================= start: CVThread and subclasses =================


    public abstract class CVThread extends Thread {

        protected CameraPreview mCameraView;
        protected boolean mIsRunning = true;
//        protected NativeWrapper.CVCallback mCVCallback;

        protected abstract void execute();

        public CVThread() {

        }

        public CVThread(CameraPreview cameraView) {


            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mCameraView = cameraView;

        }

        @Override
        public void run() {

            synchronized (mCameraView) {

                while (mIsRunning) {


//                    if (mFrameMat != null && mCVCallback != null) {

                        try {

                            mCameraView.wait();

                            execute();

                        } catch (InterruptedException e) {

                        }

//                    }
                }

            }
        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }

    }

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
//
//                if (MainActivity.isDebugViewEnabled())
//                    mTimerCallbacks.onTimerStopped(TaskTimer.FOCUS_MEASURE_ID);
//
//
                if (CameraActivity.isDebugViewEnabled())
                    mTimerCallbacks.onTimerStopped(TaskTimer.FOCUS_MEASURE_ID);

                mCVCallback.onFocusMeasured(patches);


            }
        }
    }


    // ================= end: CVThread and subclasses =================




}