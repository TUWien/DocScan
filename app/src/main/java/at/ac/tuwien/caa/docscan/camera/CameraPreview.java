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

import com.crashlytics.android.Crashlytics;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.cv.thread.preview.IPManager;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static com.google.zxing.BarcodeFormat.QR_CODE;


/**
 * Class for showing the camera preview. This class is extending SurfaceView and making use of the
 * Camera API. The received frames are converted to OpenCV Mat's in a fixed time interval. The Mat
 * is used by two thread classes (inner classes): FocusMeasurementThread and PageSegmentationThread.
 */
@SuppressWarnings("deprecation")
public class CameraPreview  extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    private static final String CLASS_NAME = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private CVCallback mCVCallback;
    private CameraPreviewCallback mCameraPreviewCallback;

    private CameraHandlerThread mThread = null;

    private int mFrameWidth;
    private int mFrameHeight;

    // This is used to setIsPaused the CV tasks for a short time after an image has been taken in series mode.
    // Prevents a shooting within a very short time range:
    private boolean mIsQRMode;

   // Used for the size of the auto focus area:
    private static final int FOCUS_HALF_AREA = 1000;

    private boolean isCameraInitialized;
    private String mFlashMode; // This is used to save the current flash mode, during Activity lifecycle.
    private boolean mIsPreviewFitting = false;

    private MultiFormatReader mMultiFormatReader;

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

//        CVManager.getInstance().setCVCallback(mCVCallback);
        IPManager.getInstance().setCVCallback(mCVCallback);


        mFlashMode = FLASH_MODE_OFF;

        initMultiFormatReader();

    }




//    QR code stuff starts here:

    public void startQrMode(boolean qrMode) {

        mIsQRMode = qrMode;

    }

    private void initMultiFormatReader() {
        Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);
    }


    private void detectBarcode(byte[] pixels) {


        PlanarYUVLuminanceSource source = buildLuminanceSource(pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = null;

        Log.d(CLASS_NAME, "detectBarcode");

        Map<DecodeHintType,Object> hintsMap = new EnumMap<>(DecodeHintType.class);
        hintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

        try {
            result = mMultiFormatReader.decode(bitmap, hintsMap);
            if (result != null)
                Log.d(CLASS_NAME, "result: " + result.toString());
            else
                Log.d(CLASS_NAME, "detectBarcode: no result");
        } catch (NotFoundException e) {
            Crashlytics.logException(e);
            Log.d(CLASS_NAME, "detectBarcode: not found");
            e.printStackTrace();
        }

        if (result != null) {
            Log.d(CLASS_NAME, "rawresult output: " + result.toString());
            mCVCallback.onQRCode(result);
        }
        else
            Log.d(CLASS_NAME, "rawresult still null");

    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data) {

        // Go ahead and assume it's YUV rather than die.
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, mFrameWidth, mFrameHeight, 0, 0,
                    mFrameWidth, mFrameHeight, false);
        } catch(Exception e) {
            Crashlytics.logException(e);
        }

        return source;
    }


    /**
     * Called after the preview received a new frame (as byte array).
     * @param pixels byte array containgin the frame.
     * @param camera camera
     */
    @Override
    public void onPreviewFrame(byte[] pixels, Camera camera) {

        if (pixels == null)
            return;

        if (mIsQRMode) {
            Log.d(CLASS_NAME, "detecting qr code");
            detectBarcode(pixels);
        }
        else {
//            Log.d(CLASS_NAME, "doing cv stuff");
            cvManagerAction(pixels);
        }


    }

    private void cvManagerAction(byte[] pixels) {

        IPManager.getInstance().receiveFrame(pixels, mFrameWidth, mFrameHeight);

    }

    public void shortFlash() {

        new Thread(new Runnable() {
            public void run(){

                if (mCamera == null)
                    return;

                Camera.Parameters params = mCamera.getParameters();
                if (params == null)
                    return;

                params.setFlashMode(FLASH_MODE_TORCH);
                mCamera.setParameters(params);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                params.setFlashMode(mFlashMode);
                mCamera.setParameters(params);

            }
        }).start();

    }


    private class CameraHandlerThread extends HandlerThread {

        Handler mHandler;

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
                Crashlytics.logException(e);
                Log.w(CLASS_NAME, "wait was interrupted");
            }
        }
    }

    // Callbacks:
    public interface CVCallback {

        void onFocusMeasured(Patch[] patches);
        void onPageSegmented(DkPolyRect[] polyRects);
        void onMovement(boolean moved);
        void onWaitingForDoc(boolean waiting);
        void onCaptureVerified();
        void onQRCode(Result result);

    }

    /**
     * Interfaces used to tell the activity that a dimension is changed. This is used to enable
     * a conversion between frame and screen coordinates (necessary for drawing in PaintView).
     */
    public interface CameraPreviewCallback {

        void onMeasuredDimensionChange(int width, int height);
        void onFrameDimensionChange(int width, int height, int cameraOrientation);
        void onFlashModesFound(List<String> modes);
        void onExposureLockFound(boolean isSupported);
//        void onWhiteBalanceFound(List<String> whiteBalances);
        void onFocusTouch(PointF point);
        void onFocusTouchSuccess();

    }


    public void stop() {

        isCameraInitialized = false;

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
            Log.d(CLASS_NAME, "Preview surface does not exist");
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

        Log.d(CLASS_NAME, "releasing camera");
        isCameraInitialized = false;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void pause() {

        isCameraInitialized = false;
        releaseCamera();


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

        Log.d(CLASS_NAME, "onTouchEvent");

        if (mCamera == null)
            return true;

        // Do nothing in the auto (series) mode:
//        if (!mManualFocus)
//            return true;

        // We wait until the finger is up again:
        if (event.getAction() != MotionEvent.ACTION_UP)
            return true;

        float touchX = event.getX();
        float touchY = event.getY();

        final PointF screenPoint = new PointF(touchX, touchY);
        mCameraPreviewCallback.onFocusTouch(screenPoint);

        Rect focusRect = getFocusRect(screenPoint);
        if (focusRect == null) {
            Log.d(CLASS_NAME, "focus rectangle is not valid!");
            return true;
        }

        Camera.Area focusArea = new Camera.Area(focusRect, 750);
        List<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(focusArea);

        mCamera.cancelAutoFocus();

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setJpegQuality(100);
// Use autofocus if available:
//        useAutoFocus(parameters);


        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)
            && parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO)
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        if (parameters.getMaxNumFocusAreas() > 0) {

            if (mCamera.getParameters().getMaxNumFocusAreas() > 0)
                parameters.setFocusAreas(focusAreas);

//            Tested devices supporting metering areas:         Nexus 5X
//            Tested devices that do no support metering areas: Samsung S6
            if (mCamera.getParameters().getMaxNumMeteringAreas() > 0)
                parameters.setMeteringAreas(focusAreas);

        }
// Use this to keep a stable exposure:
//        parameters.setAutoExposureLock(false);
        mCamera.setParameters(parameters);

        try {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    // Stop the drawing of the auto focus circle anyhow, do not care if the auto focus
                    // was successful -> do not use value of success.
                    mCameraPreviewCallback.onFocusTouchSuccess();
//                    Use this to keep a stable exposure:
//                    Camera.Parameters parameters = mCamera.getParameters();
//                    if (parameters != null && parameters.isAutoExposureLockSupported()) {
//                        parameters.setAutoExposureLock(true);
//                        mCamera.setParameters(parameters);
//                    }
                }

            });
        }
        catch (RuntimeException e) {
            Crashlytics.logException(e);
//            This can happen if the user touches the CameraPreview, while the preview is not
//            started. In this case we do nothing.
        }

        return true; //processed

    }

    private Rect getFocusRect(PointF touchScreen) {

        // The camera field of view is normalized so that -1000,-1000 is top left and 1000, 1000 is
        // bottom right. Note that multiple areas are possible, but currently only one is used.

        // Get the rotation of the screen to adjust the preview image accordingly.
        int displayOrientation = CameraActivity.getDisplayRotation();
        int orientation = calculatePreviewOrientation(mCameraInfo, displayOrientation);

        // Transform the point:
        PointF rotatedPoint = rotatePoint(touchScreen, orientation);
        PointF scaledPoint = scalePoint(rotatedPoint, orientation);
        return rectFromPoint(scaledPoint, 50);
    }

    private PointF rotatePoint(PointF point, int orientation) {

        PointF result = new PointF();

        Log.d(CLASS_NAME, "orientation: " + orientation);

        switch(orientation) {
            case 0:
                result.x = point.x;
                result.y = point.y;
                break;
            case 90:
                result.x = point.y;
                result.y = getMeasuredWidth() - point.x;
                break;
            case 180:
                result.x = getMeasuredWidth() - point.x;
                result.y = getMeasuredHeight() - point.y;
                break;
            case 270:
                result.x = getMeasuredHeight() - point.y;
                result.y = point.x;
                break;
        }

        return result;

    }

    private PointF scalePoint(PointF point, int orientation) {


        float halfScreenWidth, halfScreenHeight;

        if (orientation == 0 || orientation == 180) {
            halfScreenWidth = (float) getMeasuredWidth() / 2;
            halfScreenHeight = (float) getMeasuredHeight() / 2;
        }
        else {
            halfScreenHeight = (float) getMeasuredWidth() / 2;
            halfScreenWidth = (float) getMeasuredHeight() / 2;
        }

        // Translate the point:
        PointF translatedPoint = new PointF(point.x, point.y);
        translatedPoint.offset(-halfScreenWidth, -halfScreenHeight);

        // Norm the point between -1 and 1:
        PointF normedPoint = new PointF(translatedPoint.x, translatedPoint.y);
        normedPoint.x /= halfScreenWidth;
        normedPoint.y /= halfScreenHeight;

        // Scale the point between -FOCUS_HALF_AREA and +FOCUS_HALF_AREA:
        PointF scaledPoint = new PointF(normedPoint.x, normedPoint.y);
        scaledPoint.x *= FOCUS_HALF_AREA;
        scaledPoint.y *= FOCUS_HALF_AREA;

        return scaledPoint;

    }

    private Rect rectFromPoint(PointF point, int halfSideLength) {

        Rect result = null;

        int startX = Math.round(point.x - halfSideLength);
        int startY = Math.round(point.y - halfSideLength);
        int endX = Math.round(point.x + halfSideLength);
        int endY = Math.round(point.y + halfSideLength);

        if (startX < -FOCUS_HALF_AREA)
            startX = -FOCUS_HALF_AREA;
        if (endX > FOCUS_HALF_AREA)
            endX = FOCUS_HALF_AREA;
        if (startY < -FOCUS_HALF_AREA)
            startY = -FOCUS_HALF_AREA;
        if (endY > FOCUS_HALF_AREA)
            endY = FOCUS_HALF_AREA;

        int width = endX - startX;
        int height = endY - startY;
        // Just return an initialized rect if it is valid (otherwise an exception is thrown by Camera.setParameters.setFocusAreas
        if (width > 0 && height > 0)
            result = new Rect(startX, startY, endX, endY);

        return result;

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

        Point result = new Point(Math.round(normalizedPoint.x), Math.round(normalizedPoint.y));

        // Clamp the values if necessary:
        if (result.x < -FOCUS_HALF_AREA)
            result.x = -FOCUS_HALF_AREA;
        else if (result.x > FOCUS_HALF_AREA)
            result.x = FOCUS_HALF_AREA;

        if (result.y < -FOCUS_HALF_AREA)
            result.y = -FOCUS_HALF_AREA;
        else if (result.y > FOCUS_HALF_AREA)
            result.y = FOCUS_HALF_AREA;

        return result;

    }

    public boolean isPreviewFitting() {
        return mIsPreviewFitting;
    }

    @SuppressWarnings("deprecation")
    private void initPreview() {

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Log.d(CLASS_NAME, "Preview stopped.");
        } catch (Exception e) {
            Crashlytics.logException(e);
            // ignore: tried to stop a non-existent preview
            Log.d(CLASS_NAME, "Error starting camera preview: " + e.getMessage());
        }

        if (mCamera == null)
            return;

        // Get the rotation of the screen to adjust the preview image accordingly.
        int displayOrientation = CameraActivity.getDisplayRotation();
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
        useAutoFocus(params);


        params.setPreviewSize(mFrameWidth, mFrameHeight);

        List<String> flashModes = params.getSupportedFlashModes();
        mCameraPreviewCallback.onFlashModesFound(flashModes);

        // Restore the last used flash mode - if available:
        if (mFlashMode != null)
            params.setFlashMode(mFlashMode);

//        params.setAutoExposureLock(true);
        mCamera.setParameters(params);

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
            Log.d(CLASS_NAME, "Camera preview started.");

        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.d(CLASS_NAME, "Error starting camera preview: " + e.getMessage());
        }

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);

//        Tell the activity if the auto exposure can be locked:
        mCameraPreviewCallback.onExposureLockFound(params.isAutoExposureLockSupported());
////        Tell the activity how we can control the white balance:
//        mCameraPreviewCallback.onWhiteBalanceFound(params.getSupportedWhiteBalance());

    }

    public void lockExposure(boolean lock) {

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            if (params != null && params.isAutoExposureLockSupported()) {
                params.setAutoExposureLock(lock);
                mCamera.setParameters(params);
            }
        }

    }

    public void lockWhiteBalance(boolean lock) {

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            if (params != null && params.isAutoWhiteBalanceLockSupported()) {
                params.setAutoWhiteBalanceLock(lock);
                mCamera.setParameters(params);
            }
        }

    }


    public void setWhiteBalance(String value) {

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            if (params != null) {
                params.setWhiteBalance(value);
                mCamera.setParameters(params);
            }
        }
    }

    private void useAutoFocus(Camera.Parameters params) {
        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
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


    public void startAutoFocus() {

//        Check here if the camera is still accessible.
        if (isCameraInitialized && mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();

            // Use autofocus if available:
            useAutoFocus(params);

            mCamera.setParameters(params);
        }
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

            if ((Math.abs(ratio - optRatio) <= Math.abs(bestRatio - optRatio))) {
                bestRatio = ratio;
                bestSize = size;
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

        Log.d(CLASS_NAME, "initCamera:");
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
        int displayOrientation = CameraActivity.getDisplayRotation();
        int cameraOrienation = calculatePreviewOrientation(mCameraInfo, displayOrientation);
        mCamera.setDisplayOrientation(cameraOrienation);

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, cameraOrienation);

        Log.d(CLASS_NAME, "display orientation changed: " + displayOrientation);
        Log.d(CLASS_NAME, "changed camera orientation: " + cameraOrienation);

    }



}