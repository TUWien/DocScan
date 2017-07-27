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

package at.ac.tuwien.caa.docscan.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.camera.cv.ChangeDetector;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.threads.CVThreadManager;
import at.ac.tuwien.caa.docscan.camera.threads.ChangeCallable;
import at.ac.tuwien.caa.docscan.camera.threads.FocusCallable;
import at.ac.tuwien.caa.docscan.camera.threads.PageCallable;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.CAMERA_FRAME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FOCUS_MEASURE;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.MOVEMENT_CHECK;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.NEW_DOC;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;

/**
 * Class for showing the camera preview. This class is extending SurfaceView and making use of the
 * Camera API. The received frames are converted to OpenCV Mat's in a fixed time interval. The Mat
 * is used by two thread classes (inner classes): FocusMeasurementThread and PageSegmentationThread.
 */
@SuppressWarnings("deprecation")
public class CameraPreview  extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    //// let the C routine decide which resolution they need...
    //private static final int MAX_MAT_WIDTH = 500;
    //private static final int MAX_MAT_HEIGHT = 500;

    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private CVCallback mCVCallback;
    private CameraPreviewCallback mCameraPreviewCallback;

    private PageSegmentationThread mPageSegmentationThread;
    private FocusMeasurementThread mFocusMeasurementThread;
    private IlluminationThread mIlluminationThread;
    private CameraHandlerThread mThread = null;

    // Mat used by mPageSegmentationThread and mFocusMeasurementThread:
    private Mat mFrameMat;
    private int mFrameWidth;
    private int mFrameHeight;
    private boolean mAwaitFrameChanges = false; // this is dependent on the mode: single vs. series
    private boolean mManualFocus = true;
    private boolean mIsImageProcessingPaused = false;
    private boolean mStoreMat = false;
    private boolean mUseThreading = false;

    // This is used to pause the CV tasks for a short time after an image has been taken in series mode.
    // Prevents a shooting within a very short time range:
    private static final int LAST_SHOT_TIME_NOT_INIT = -1;
    private long mLastShotTime = LAST_SHOT_TIME_NOT_INIT;
    private boolean mIsSeriesMode;
    private static final int MIN_TIME_BETWEEN_SHOTS = 2000; // in milli-seconds

    private long mLastTime;
    // Used for generating the mat (for CV tasks) at a fixed frequency:
    private static long FRAME_TIME_DIFF = 1;
    // Used for the size of the auto focus area:
    private static final int FOCUS_HALF_AREA = 1000;

    private static final int MIN_RESOLUTION_AREA = 1000000;

    private boolean isCameraInitialized;
    private DkPolyRect mIlluminationRect;
    private String mFlashMode; // This is used to save the current flash mode, during Activity lifecycle.
    private boolean mIsPreviewFitting = false;

    private CVThreadManager mCVThreadManager;
    private int mFrameCnt = 0;

    private boolean mVerifyCapture = false;
    private long mVerificationTime;
    private static long VERIFICATION_TIME_DIFF = 800;
    private boolean mMeasureFocus = false;

    /**
     * Creates the CameraPreview and the callbacks required to send events to the activity.
     * @param context context
     * @param attrs attributes
     */
    public CameraPreview(Context context, AttributeSet attrs) {

        super(context, attrs);

        mHolder = getHolder();
        mHolder.addCallback(this);

        mCVCallback = (CVCallback) context;
        mCameraPreviewCallback = (CameraPreviewCallback) context;

        // used for debugging:
        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;

        mFlashMode = null;

        mCVThreadManager = CVThreadManager.getsInstance();
        // CVThreadManager stores activity as a weak reference. No need to unregister.
//        mCVThreadManager.setUiThreadCallback(this);

//        bgSubtractor = Video.createBackgroundSubtractorMOG2();

    }

    /**
     * Called after the preview received a new frame (as byte array).
     * @param pixels byte array containgin the frame.
     * @param camera camera
     */
    @Override
    public void onPreviewFrame(byte[] pixels, Camera camera) {

        updateFPS();

        // TODO: handle an overflow:
        mFrameCnt++;

        if (pixels == null)
            return;

        if (mIsImageProcessingPaused)
            return;

        // The verification is done in series mode after an automatic capture is requested:
        if (mAwaitFrameChanges && mVerifyCapture) {
            checkMovementAfterCapture(pixels);
        } else {
            if (mUseThreading)
                performCVTasks(pixels);
            else
                singleThreadCV(pixels);
        }
//        oldSingleThread(pixels);

    }

    private void oldSingleThread(byte[] pixels) {

        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

            synchronized (this) {

                // 1.5 since YUV
                Mat yuv = new Mat((int)(mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
                yuv.put(0, 0, pixels);

                if (mFrameMat != null)
                    mFrameMat.release();

                mFrameMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
                Imgproc.cvtColor(yuv, mFrameMat, Imgproc.COLOR_YUV2RGB_NV21);

                if (mStoreMat) {
                    ChangeDetector.initNewFrameDetector(mFrameMat);
                    mStoreMat = false;
                }

                yuv.release();

                mLastTime = currentTime;

                boolean processFrame = true;

                // This is done in series mode:
                if (mAwaitFrameChanges)
                    processFrame = isFrameSteadyAndNew();

                // Check if there should be short break between two successive shots in series mode:
                boolean paused = pauseBetweenShots(currentTime);
                processFrame &= !paused;

//                If in single mode - or the frame is steady and contains a change, do the document analysis:
                if (processFrame)
                    this.notify();

            }

        }

    }


    private void singleThreadCV(byte[] pixels) {

        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

            synchronized (this) {



                if (mFrameMat != null)
                    mFrameMat.release();

                mFrameMat = byte2Mat(pixels);

                if (mStoreMat) {
                    ChangeDetector.initMovementDetector(mFrameMat);
                    ChangeDetector.initNewFrameDetector(mFrameMat);
                    mStoreMat = false;
                } else if (!ChangeDetector.isMovementDetectorInitialized()) {
                    ChangeDetector.initMovementDetector(mFrameMat);
                }


//                boolean processFrame = true;
//
//                // This is done in series mode:
//                if (mAwaitFrameChanges)
//                    processFrame = isFrameSteadyAndNew();
//
//                // Check if there should be short break between two successive shots in series mode:
//                boolean paused = pauseBetweenShots(currentTime);
//                processFrame &= !paused;

                boolean processFrame = true;
                // In serial mode the document analysis is just performed if no movement occurred:
                if (mAwaitFrameChanges) {
                    if (!mCVThreadManager.isRunning(CVThreadManager.TASK_CHANGE)) {
                        ChangeCallable cCallable = new ChangeCallable(mFrameMat.clone(), mCVCallback, mTimerCallbacks);
                        mCVThreadManager.addCallable(cCallable, CVThreadManager.TASK_CHANGE);
                        processFrame = mCVThreadManager.isFrameSteadyAndNew();
                    }
                }

                //                If in single mode - or the frame is steady and contains a change, do the document analysis:
                if (processFrame)
                    this.notify();

                if (mAwaitFrameChanges)
                    mVerificationTime = System.currentTimeMillis();

            }

            mLastTime = currentTime;
        }
    }

    public boolean isMultiThreading() {
        return mUseThreading;
    }

    public void setThreading(boolean multiThreading) {
        mUseThreading = multiThreading;
    }


    /**
     * This is used to enable a movement and change detector (currently just used in series mode).
     * @param awaitFrameChanges
     */
    public void setAwaitFrameChanges(boolean awaitFrameChanges) {

        mAwaitFrameChanges = awaitFrameChanges;

    }

    public void verifyCapture() {
        mVerifyCapture = true;
    }

    private void checkMovementAfterCapture(byte[] pixels) {


        long timePassed = System.currentTimeMillis() - mVerificationTime;
        if (timePassed <= VERIFICATION_TIME_DIFF) {

            Mat mat = byte2Mat(pixels);

            ChangeCallable cCallable = new ChangeCallable(mat.clone(), mCVCallback, mTimerCallbacks);
            mCVThreadManager.addCallable(cCallable, CVThreadManager.TASK_CHANGE);
            boolean isSteady = mCVThreadManager.isFrameSteadyAndNew();
            if (!isSteady)
                mVerifyCapture = false;
            return;

        }
        else {
            mVerifyCapture = false;
            mCVCallback.onCaptureVerified();
            return;
        }

    }

    private void performCVTasks(byte[] pixels) {

        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

//        if (true) {
            Mat mat = byte2Mat(pixels);

            if (mStoreMat) {
                ChangeDetector.initMovementDetector(mat);
                ChangeDetector.initNewFrameDetector(mat);
                mStoreMat = false;
            }
            else if (!ChangeDetector.isMovementDetectorInitialized()) {
                ChangeDetector.initMovementDetector(mat);
            }

            boolean processFrame = true;
            // In serial mode the document analysis is just performed if no movement occurred:
            if (mAwaitFrameChanges) {
                if (!mCVThreadManager.isRunning(CVThreadManager.TASK_CHANGE)) {
                    ChangeCallable cCallable = new ChangeCallable(mat.clone(), mCVCallback, mTimerCallbacks);
                    mCVThreadManager.addCallable(cCallable, CVThreadManager.TASK_CHANGE);
                    processFrame = mCVThreadManager.isFrameSteadyAndNew();
                }
            }

            if (processFrame) {

                if (!mCVThreadManager.isRunning(CVThreadManager.TASK_PAGE)) {
                    PageCallable pCallable = new PageCallable(mat.clone(), mCVCallback, mTimerCallbacks, mFrameCnt);
                    mCVThreadManager.addCallable(pCallable, CVThreadManager.TASK_PAGE);
                }

                if (mMeasureFocus && !mCVThreadManager.isRunning(CVThreadManager.TASK_FOCUS)) {
                    FocusCallable fCallable = new FocusCallable(mat.clone(), mCVCallback, mTimerCallbacks);
                    mCVThreadManager.addCallable(fCallable, CVThreadManager.TASK_FOCUS);
                }

                if (mAwaitFrameChanges)
                    mVerificationTime = System.currentTimeMillis();

            }

            mat.release();

            mLastTime = currentTime;

        }


    }

    public void stop() {

        isCameraInitialized = false;

    }

    public boolean isFrameSame(Mat frame1, Mat frame2) {

        Mat tmp1 = new Mat(frame1.rows(), frame1.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame1, tmp1, Imgproc.COLOR_RGB2GRAY);

        Mat tmp2 = new Mat(frame2.rows(), frame2.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame2, tmp2, Imgproc.COLOR_RGB2GRAY);

        Mat subtractResult = new Mat(frame2.rows(), frame2.cols(), CvType.CV_8UC1);
        Core.absdiff(frame1, frame2, subtractResult);
        Imgproc.threshold(subtractResult, subtractResult, 50, 1, Imgproc.THRESH_BINARY);
        Scalar sumDiff = Core.sumElems(subtractResult);
        double diffRatio = sumDiff.val[0] / (frame1.cols() * frame2.rows());


        return diffRatio < .05;

    }

    /**
     * Called after the surface is created.
     * @param holder Holder
     */
    public void surfaceCreated(SurfaceHolder holder) {

        initPreview();

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Called once the surface is changed. This happens after orientation changes or if the activity
     * is started (or resumed).
     * @param holder holder
     * @param format format
     * @param width width
     * @param height height
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


        // Check that the screen is on, because surfaceChanged is also called after the screen is turned off in landscape mode.
        // (Because the activity is resumed in such cases)
//        if (CameraActivity.isScreenOn())
//            initCamera(width, height);

    }

    /**
     * Called after the activity is resumed.
     */
    public void resume() {

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist");
            return;
        }

        if (!isCameraInitialized)
            openCameraThread();

        initPreview();

    }

    @SuppressWarnings("deprecation")
    public Camera getCamera() {
        return mCamera;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void pause() {

        releaseCamera();
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
    @SuppressWarnings("deprecation")
    public boolean onTouchEvent(MotionEvent event) {

        if (mCamera == null)
            return true;

        // Do nothing in the auto (series) mode:
        if (!mManualFocus)
            return true;

        // We wait until the finger is up again:
        if (event.getAction() != MotionEvent.ACTION_UP)
            return true;


        float touchX = event.getX();
        float touchY = event.getY();

        final PointF touchScreen = new PointF(touchX, touchY);
        mCameraPreviewCallback.onFocusTouch(touchScreen);

        // The camera field of view is normalized so that -1000,-1000 is top left and 1000, 1000 is
        // bottom right. Note that multiple areas are possible, but currently only one is used.

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
                // Stop the drawing of the auto focus circle anyhow, do not care if the auto focus
                // was successful:
                mCameraPreviewCallback.onFocusTouchSuccess();
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

        return new Point(Math.round(normalizedPoint.x), Math.round(normalizedPoint.y));

    }

    public boolean isPreviewFitting() {
        return mIsPreviewFitting;
    }

    @SuppressWarnings("deprecation")
    private void initPreview() {

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Log.d(TAG, "Preview stopped.");
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        if (mCamera == null)
            return;

        // Get the rotation of the screen to adjust the preview image accordingly.
        int displayOrientation = CameraActivity.getOrientation();
        int orientation = calculatePreviewOrientation(mCameraInfo, displayOrientation);
        mCamera.setDisplayOrientation(orientation);

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> cameraSizes = params.getSupportedPreviewSizes();

        Camera.Size pictureSize = getLargestPictureSize();
        params.setPictureSize(pictureSize.width, pictureSize.height);

//        if (!mPreviewSizeSet) {
//            Camera.Size bestSize = getBestFittingSize(cameraSizes, orientation);
//
//            mFrameWidth = bestSize.width;
//            mFrameHeight = bestSize.height;
//            mPreviewSizeSet = true;
//        }
        Camera.Size previewSize = getPreviewSize(cameraSizes, pictureSize);
        mFrameWidth = previewSize.width;
        mFrameHeight = previewSize.height;

        mIsPreviewFitting = isPreviewFitting(previewSize);

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        params.setPreviewSize(mFrameWidth, mFrameHeight);

        List<String> flashModes = params.getSupportedFlashModes();
        mCameraPreviewCallback.onFlashModesFound(flashModes);

        // Restore the last used flash mode - if available:
        if (mFlashMode != null)
            params.setFlashMode(mFlashMode);

        mCamera.setParameters(params);

        mPageSegmentationThread = new PageSegmentationThread(this);
        if (mPageSegmentationThread.getState() == Thread.State.NEW)
            mPageSegmentationThread.start();

        mFocusMeasurementThread = new FocusMeasurementThread(this);
        if (mFocusMeasurementThread.getState() == Thread.State.NEW)
            mFocusMeasurementThread.start();

        mIlluminationThread = new IlluminationThread(this);
        if (mIlluminationThread.getState() == Thread.State.NEW)
            mIlluminationThread.start();

        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();

            if (params.getFocusMode() == Camera.Parameters.FOCUS_MODE_AUTO) {

                float centerX = getMeasuredWidth() / 2;
                float centerY = getMeasuredHeight() / 2;
                PointF centerScreen = new PointF(centerX, centerY);

                float touchX = centerX;
                float touchY = centerY;
                PointF touchScreen = new PointF(touchX, touchY);

                // The camera field of view is normalized so that -1000,-1000 is top left and 1000, 1000 is
                // bottom right. Not that multiple areas are possible, but currently only one is used.

                float focusRectHalfSize = .2f;

                // Normalize the coordinates of the touch event:
                Point upperLeft = transformPoint(centerScreen, touchScreen, -focusRectHalfSize, -focusRectHalfSize);
                Point lowerRight = transformPoint(centerScreen, touchScreen, focusRectHalfSize, focusRectHalfSize);

                Rect focusRect = new Rect(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);

                Camera.Area focusArea = new Camera.Area(focusRect, 750);
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(focusArea);

                mCamera.cancelAutoFocus();

                if (params.getMaxNumFocusAreas() > 0) {

//                    params.setFocusAreas(focusAreas);
//                    params.setMeteringAreas(focusAreas);
                    params.setFocusAreas(focusAreas);
                    params.setMeteringAreas(focusAreas);

//            mCamera.setParameters(parameters);
                }


                mCamera.setParameters(params);

                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }

                });
            }
            Log.d(TAG, "Camera preview started.");

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);


    }

    private Camera.Size getLargestPictureSize() {

        Camera.Size size = null;
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> supportedSizes = params.getSupportedPictureSizes();
        int bestArea = 0;
        int area;

        for (Camera.Size s : supportedSizes) {
            area = s.width * s.height;
            if (area > bestArea) {
                bestArea = area;
                size = s;
            }
        }

        return size;

    }

    public void cancelAutoFocus() {

        mCamera.cancelAutoFocus();

    }

    public void startAutoFocus() {

        mManualFocus = false;

        Camera.Parameters params = mCamera.getParameters();

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(params);

    }


    public void pauseImageProcessing(boolean pause) {

        mIsImageProcessingPaused = pause;
        mFocusMeasurementThread.setRunning(!pause);
        mPageSegmentationThread.setRunning(!pause);
        mIlluminationThread.setRunning(!pause);

        // Take care that no patches or pages are rendered in the PaintView:
        if (pause) {
            synchronized (this) {
                DkPolyRect[] r = {};
                mCVCallback.onPageSegmented(r, mFrameCnt);
                Patch[] p = {};
                mCVCallback.onFocusMeasured(p);
            }
        }

    }

    public boolean isFrameSteady() {

        if (!mAwaitFrameChanges)
            return true;
        else {
            return mCVThreadManager.isFrameSteadyAndNew();
        }
    }

    private Mat byte2Mat(byte[] pixels) {

        Mat yuv = new Mat((int) (mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
        yuv.put(0, 0, pixels);

        Mat result = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
        Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2RGB_NV21);

        return result;
    }

    /**
     * Returns if enough time has past after the last shot in series mode. The function should prevent
     * that a shot is taken in series mode, just because of small changes of the image content
     * (e.g. a finger is laid on a manuscript).
     * @param time The current time.
     * @return boolean indicating if more time has to pass.
     */
    private boolean pauseBetweenShots(long time) {

        boolean pause = false;

        if (mIsSeriesMode && mLastShotTime != LAST_SHOT_TIME_NOT_INIT) {
            if (time - mLastShotTime <= MIN_TIME_BETWEEN_SHOTS) {
                pause = true;
            }
        }

        return pause;

    }

    private boolean isFrameSteadyAndNew() {

//                Check if there is sufficient image change between the current frame and the last image taken:
        boolean isFrameSteady;
        boolean isFrameDifferent = false;

//                    The ChangeDetector is only initialized after an image has been taken:
        if (ChangeDetector.isNewFrameDetectorInitialized()) {

            mTimerCallbacks.onTimerStarted(MOVEMENT_CHECK);
            isFrameSteady = ChangeDetector.isFrameSteady(mFrameMat);
            mTimerCallbacks.onTimerStopped(MOVEMENT_CHECK);

            if (!isFrameSteady) {
                mCVCallback.onMovement(true);
                return false;
            }
            else {
                mCVCallback.onMovement(false);
            }

            if (!isFrameDifferent) {
                mTimerCallbacks.onTimerStarted(NEW_DOC);
                isFrameDifferent = ChangeDetector.isNewFrame(mFrameMat);
                mTimerCallbacks.onTimerStopped(NEW_DOC);

                if (!isFrameDifferent) {
                    mCVCallback.onWaitingForDoc(true);
                    return false;
                } else {
                    mCVCallback.onWaitingForDoc(false);
                    mTimerCallbacks.onTimerStarted(FLIP_SHOT_TIME);
                }
            }
        }

        return true;

    }

    private void updateFPS() {

        // Take care that in this case the timer is first stopped (contrary to the other calls):
        mTimerCallbacks.onTimerStopped(CAMERA_FRAME);
        mTimerCallbacks.onTimerStarted(CAMERA_FRAME);

    }


    public void storeMat(boolean isSeriesMode) {

        mStoreMat = true;

        // If the user switched just to series mode do not make a break:
        if (!mIsSeriesMode && isSeriesMode)
            mLastShotTime = LAST_SHOT_TIME_NOT_INIT;
        else
            mLastShotTime = System.currentTimeMillis();

        mIsSeriesMode = isSeriesMode;

    }

    public void startFocusMeasurement(boolean start) {

        mFocusMeasurementThread.setRunning(start);
        mMeasureFocus = start;

    }

    public boolean isFocusMeasured() {

        return mFocusMeasurementThread.isRunning();

    }

    public void startIllumination(boolean start) {

        mIlluminationThread.setRunning(start);

    }

    public void setIlluminationRect(DkPolyRect illuminationRect) {

        mIlluminationRect = illuminationRect;
        
    }

    @SuppressWarnings("deprecation")
    private boolean isPreviewFitting(Camera.Size previewSize) {
        int width, height;
//        Hack: Before the getWidth/getHeight was used, but on the Galaxy S6 the layout was differently initalized,
//        so that the height returned the entire height without subtracting the height of the camera control layout.
//        Therefore, we calculate the dimension of the preview manually.
//        int height = getHeight();
        Point dim = CameraActivity.getPreviewDimension();
        if (dim != null) {
            width = dim.x;
            height = dim.y;
        }
        else {
            width = getWidth();
            height = getHeight();
        }

        if (width < height) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        float previewRatio = (float) previewSize.width / previewSize.height;
        float spaceRatio = (float) width / height;

        return (spaceRatio >= previewRatio);

    }

    @SuppressWarnings("deprecation")
    private Camera.Size getPreviewSize(List<Camera.Size> previewSizes, Camera.Size pictureSize) {

        float optRatio = (float) pictureSize.width / pictureSize.height;
        float ratio;
        int bestResArea = 0;
        int resArea;
        Camera.Size bestSize = null;
        boolean optRatioFound = false;

//        First try to find an optimal ratio:
        for (Camera.Size size : previewSizes) {
            ratio = (float) size.width / size.height;
            resArea = size.width * size.height;

            if ((ratio == optRatio) && (resArea >= bestResArea)) {
                bestResArea = resArea;
                bestSize = size;
            }
        }

        if (bestSize != null)
            return bestSize;

        float bestRatio = 0;
//        Second find the closest ratio:
        for (Camera.Size size : previewSizes) {
            ratio = (float) size.width / size.height;
            resArea = size.width * size.height;

            if ((Math.abs(ratio - optRatio) <= Math.abs(bestRatio - optRatio))) {
                bestResArea = resArea;
                bestRatio = ratio;
                bestSize = size;
            }
        }

        return bestSize;

//        if ((bestSize != null) && (bestResArea >= MIN_RESOLUTION_AREA))
//            return bestSize;
//
////        Third find the largest resolution:
//        for (Camera.Size size : previewSizes) {
//            resArea = size.width * size.height;
//
//            if (resArea >= bestResArea) {
//                bestResArea = resArea;
//                bestSize = size;
//            }
//        }
//
//        return bestSize;

    }

    /**
     * Returns the preview size that fits best into the surface view.
     * @param cameraSizes possible frame sizes (camera dependent)
     * @param orientation orientation of the surface view
     * @return best fitting size
     */
    @SuppressWarnings("deprecation")
    private Camera.Size getBestFittingSize(List<Camera.Size> cameraSizes, int orientation) {

        // If the app  is paused (=in background) and the orientation is changed, the width and
        // height are not switched. So here we have to switch if necessary:
//        int width = getWidth();

        int width, height;
//        Hack: Before the getWidth/getHeight was used, but on the Galaxy S6 the layout was differently initalized,
//        so that the height returned the entire height without subtracting the height of the camera control layout.
//        Therefore, we calculate the dimension of the preview manually.
//        int height = getHeight();
        Point dim = CameraActivity.getPreviewDimension();
        if (dim != null) {
            width = dim.x;
            height = dim.y;
        }
        else {
            width = getWidth();
            height = getHeight();
        }

        if (((orientation == 90 || orientation == 270) && (width > height)) ||
                ((orientation == 0 || orientation == 180) && (width < height))) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        Camera.Size bestSize = null;

        final double ASPECT_TOLERANCE = 0.1;

        double targetRatio;
        int targetLength;

        if (width > height) {
            targetRatio = (double) height / width;
            targetLength = height;
        }
        else {
            targetRatio = (double) width / height;
            targetLength = width;
        }

        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : cameraSizes) {
            double ratio = (double) size.height / size.width;

            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            int length = size.height;

            int diff = Math.abs(length - targetLength);
            if (diff < minDiff) {
                bestSize = size;
                minDiff = diff;
            }
        }

        if (bestSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : cameraSizes) {
                int length = size.height;

                int diff = Math.abs(length - targetLength);
                if (diff < minDiff) {
                    bestSize = size;
                    minDiff = diff;
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
    @SuppressWarnings("deprecation")
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

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
    }

    @SuppressWarnings("deprecation")
    public void setFlashMode(String flashMode) {

        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(flashMode);
        mCamera.setParameters(params);

        mFlashMode = flashMode;

    }


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

    @SuppressWarnings("deprecation")
    private void initCamera() {

//        releaseCamera();
        // Open an instance of the first camera and retrieve its info.
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        mCamera = Camera.open(0);
        mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, mCameraInfo);

    }

    /**
     * This function is called after orientation changes. The Activity is not resumed on orientation
     * changes, in order to prevent a camera restart. Thus, the preview has to adapt to the new
     * orientation.
     */
    public void displayRotated() {

        if (mCamera == null)
            return;

        // Get the rotation of the screen to adjust the preview image accordingly.
        int displayOrientation = CameraActivity.getOrientation();
        int orientation = calculatePreviewOrientation(mCameraInfo, displayOrientation);
        mCamera.setDisplayOrientation(orientation);

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);

    }

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

    // Callbacks:
    public interface CVCallback {

        void onFocusMeasured(Patch[] patches);
        void onPageSegmented(DkPolyRect[] polyRects, int frameID);
        void onIluminationComputed(double value);
        void onMovement(boolean moved);
        void onWaitingForDoc(boolean waiting);
        void onCaptureVerified();

    }

    /**
     * Interfaces used to tell the activity that a dimension is changed. This is used to enable
     * a conversion between frame and screen coordinates (necessary for drawing in PaintView).
     */
    public interface CameraPreviewCallback {

        void onMeasuredDimensionChange(int width, int height);
        void onFrameDimensionChange(int width, int height, int cameraOrientation);
        void onFlashModesFound(List<String> modes);
        void onFocusTouch(PointF point);
        void onFocusTouchSuccess();

    }

    public class FocusMeasurementThread extends Thread {

        private CameraPreview mCameraView;
        private boolean mIsRunning;


        public FocusMeasurementThread(CameraPreview cameraView) {

            mCameraView = cameraView;

        }

        /**
         * The main loop of the thread.
         */
        @Override
        public void run() {

            synchronized (mCameraView) {

                while (true) {

                    try {
                        mCameraView.wait();

                        if (mIsRunning) {

                            mTimerCallbacks.onTimerStarted(FOCUS_MEASURE);

                            Patch[] patches = NativeWrapper.getFocusMeasures(mFrameMat);
                            mTimerCallbacks.onTimerStopped(FOCUS_MEASURE);

                            mCVCallback.onFocusMeasured(patches);

                        }

                    } catch (InterruptedException e) {
                    }

                }

            }
        }

        public void setRunning(boolean running) {

            mIsRunning = running;

            if (!mIsRunning)
                mCVCallback.onFocusMeasured(null);

        }

        public boolean isRunning() {
            return mIsRunning;
        }



    }

    public class PageSegmentationThread extends Thread {

        private CameraPreview mCameraView;
        private boolean mIsRunning;


        public PageSegmentationThread(CameraPreview cameraView) {

            mCameraView = cameraView;
            mIsRunning = true;

        }

        /**
         * The main loop of the thread.
         */
        @Override
        public void run() {

            synchronized (mCameraView) {

                while (true) {

                    try {
                        mCameraView.wait();


                        if (mIsRunning) {

                            mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);

                            DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mFrameMat);
//                            DkPolyRect[] polyRects = {new DkPolyRect()};
                            mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);

                            mCVCallback.onPageSegmented(polyRects, mFrameCnt);


                        }
//                        execute();

                    } catch (InterruptedException e) {
                    }

                }

            }
        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }



    }

    public class IlluminationThread extends Thread {

        private CameraPreview mCameraView;
        private boolean mIsRunning;


        public IlluminationThread(CameraPreview cameraView) {

            mCameraView = cameraView;
            mIsRunning = false;

        }

        /**
         * The main loop of the thread.
         */
        @Override
        public void run() {

//            synchronized (mCameraView) {
//
//                while (true) {
//
//                    try {
//                        mCameraView.wait();
//
//                        if (mIsRunning) {
//
////                            // Measure the time if required:
////                            if (CameraActivity.isDebugViewEnabled())
////                                mTimerCallbacks.onTimerStarted(ILLUMINATION);
//
//                            double illuminationValue = -1;
//                            if (mIlluminationRect != null)
//                                illuminationValue = NativeWrapper.getIllumination(mFrameMat, mIlluminationRect);
//
////                            if (CameraActivity.isDebugViewEnabled())
////                                mTimerCallbacks.onTimerStopped(ILLUMINATION);
//
//                            mCVCallback.onIluminationComputed(illuminationValue);
//
//
//                        }
//
//                    } catch (InterruptedException e) {
//                    }
//
//                }
//
//            }
        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }
    }

}