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

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
import static com.google.zxing.BarcodeFormat.QR_CODE;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

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
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity;
import timber.log.Timber;


/**
 * Class for showing the camera preview. This class is extending SurfaceView and making use of the
 * Camera API. The received frames are converted to OpenCV Mat's in a fixed time interval. The Mat
 * is used by two thread classes (inner classes): FocusMeasurementThread and PageSegmentationThread.
 */
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

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
    private PointF mLastTouchPoint;

    /**
     * Creates the CameraPreview and the callbacks required to send events to the activity.
     *
     * @param context context
     * @param attrs   attributes
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
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
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

        Timber.d("detectBarcode");

        Map<DecodeHintType, Object> hintsMap = new EnumMap<>(DecodeHintType.class);
        hintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

        try {
            result = mMultiFormatReader.decode(bitmap, hintsMap);
            if (result != null)
                Timber.d("result: %s", result.toString());
            else
                Timber.d("detectBarcode: no result");
        } catch (NotFoundException e) {
            Timber.d(e, "Barcode in image not found!");
        }

        if (result != null) {
            Timber.d("rawresult output: %s", result.toString());
            mCVCallback.onQRCode(result);
        } else
            Timber.d("rawresult still null");

    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data) {

        // Go ahead and assume it's YUV rather than die.
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, mFrameWidth, mFrameHeight, 0, 0,
                    mFrameWidth, mFrameHeight, false);
        } catch (Exception e) {
            Timber.e(e);
        }

        return source;
    }


    /**
     * Called after the preview received a new frame (as byte array).
     *
     * @param pixels byte array containgin the frame.
     * @param camera camera
     */
    @Override
    public void onPreviewFrame(byte[] pixels, Camera camera) {

        if (pixels == null)
            return;

        if (mIsQRMode) {
            Timber.d("detecting qr code");
            detectBarcode(pixels);
        } else {
//            Log.d(CLASS_NAME, "doing cv stuff");
            cvManagerAction(pixels);
        }


    }

    private void cvManagerAction(byte[] pixels) {

        IPManager.getInstance().receiveFrame(pixels, mFrameWidth, mFrameHeight);

    }

    public void shortFlash() {

        new Thread(new Runnable() {
            public void run() {

                if (mCamera == null)
                    return;
                try {
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
                } catch (RuntimeException e) {

                }
            }
        }).start();

    }

    public void startContinousFocus() {

        Timber.d("starting continous focus");
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                if (params.getSupportedFocusModes().contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    Timber.d("contains continous focus");
//                    We must cancel the autofocus, because otherwise continuous focus is not possible
                    mCamera.cancelAutoFocus();
                    params.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
                    mCamera.setParameters(params);
                    Timber.d("continous focus started");
                } else if (params.getSupportedFocusModes().contains(FOCUS_MODE_AUTO))
                    startAutoFocus();
            }
        } catch (RuntimeException e) {
//            Nothing to do here, probably camera.release has been called from somewhere.
            Timber.d("catched RuntimeException");
        }

    }

    public void startAutoFocus() {

        Timber.d("starting auto focus");
        try {
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                if (params.getSupportedFocusModes().contains(FOCUS_MODE_AUTO)) {
                    params.setFocusMode(FOCUS_MODE_AUTO);
                    mLastTouchPoint = null;
                    focusOnPoint();
                }
//                In case we have no auto focus, try out the continuous focus (I am not sure, if
//                this might ever happen):
                else if (params.getSupportedFocusModes().contains(FOCUS_MODE_CONTINUOUS_PICTURE))
                    startContinousFocus();

            }
        } catch (RuntimeException e) {
//            Nothing to do here, probably camera.release has been called from somewhere.
            Timber.d("catched RuntimeException");
        }


    }

    private PointF getCenterPoint() {

        Timber.d("getCenterPoint: " + getWidth() + " " + getHeight());
        PointF point = new PointF();
        point.x = getWidth() / 2;
        point.y = getHeight() / 2;

        return point;

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
            } catch (InterruptedException e) {
                Timber.e(e);
            }
        }
    }

    // Callbacks:
    public interface CVCallback {

        void onFocusMeasured(Patch[] patches);

        void onPageSegmented(DkPolyRect[] polyRects);

        void onMovement(boolean moved);

        void onWaitingForDoc(boolean waiting, boolean doAutoFocus);

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

        void onFocused(boolean focused);

        //        void onWhiteBalanceFound(List<String> whiteBalances);
        void onFocusTouch(PointF point);

        void onFocusTouchSuccess();

    }


    public void stop() {

        isCameraInitialized = false;

    }


    /**
     * Called after the surface is created.
     *
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
     *
     * @param holder holder
     * @param format format
     * @param width  width
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
            Timber.d("Preview surface does not exist");
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

        Timber.d("releasing camera");
        isCameraInitialized = false;
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();        // release the camera for other applications
                mCamera = null;
            } catch (RuntimeException e) {

            }
        }
    }

    public void pause() {

        isCameraInitialized = false;
        releaseCamera();


    }


    /**
     * Called after the user touches the view in order to make an auto focus. The auto focus region
     * is set device independent by using Camera.Area.
     *
     * @param event a touch event
     * @return a boolean indicating if the event has been processed
     * @see <a href="https://developer.android.com/reference/android/hardware/Camera.Area.html">Camera.Area</a>
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean onTouchEvent(MotionEvent event) {

        Timber.d("onTouchEvent");

        if (mCamera == null)
            return true;

        // We wait until the finger is up again:
        if (event.getAction() != MotionEvent.ACTION_UP)
            return true;

        float touchX = event.getX();
        float touchY = event.getY();

        final PointF screenPoint = new PointF(touchX, touchY);

//        final PointF screenPoint = getTouchPoint(event);

        Timber.d("onTouchEvent: " + event);

        mLastTouchPoint = screenPoint;
        mCameraPreviewCallback.onFocusTouch(screenPoint);

        boolean processed = focusOnScreenPoint(screenPoint);

        return processed;

    }

    private boolean focusOnScreenPoint(PointF screenPoint) {

        if (screenPoint == null || mCamera == null)
            return false;

        try {

            mCamera.cancelAutoFocus();

            Camera.Parameters parameters = mCamera.getParameters();

            Rect focusRect = getFocusRect(screenPoint);
            if (focusRect == null) {
                Timber.d("focus rectangle is not valid!");
                return true;
            }

            Camera.Area focusArea = new Camera.Area(focusRect, 750);
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(focusArea);

//            Meterting:
            //            Tested devices supporting metering areas:         Nexus 5X
            //            Tested devices that do no support metering areas: Samsung S6
            if (mCamera.getParameters().getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(focusAreas);
                mCamera.setParameters(parameters);
            }

            boolean focusSupported = false;
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)
                    && parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                if (parameters.getMaxNumFocusAreas() > 0) {
                    parameters.setFocusAreas(focusAreas);
                    mCamera.setParameters(parameters);
                    focusSupported = true;
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            // Stop the drawing of the auto focus circle anyhow, do not care if the auto focus
                            // was successful -> do not use value of success.
                            if (success)
                                mCameraPreviewCallback.onFocusTouchSuccess();

                            mCameraPreviewCallback.onFocused(success);
                        }

                    });
                }
            }
//            In case the phone does not support auto-focus, fake the focus procedure.
//            Otherwise in series mode, the imaging is blocked.
            if (!focusSupported) {
                mCameraPreviewCallback.onFocusTouchSuccess();
                mCameraPreviewCallback.onFocused(true);
            }

        } catch (RuntimeException e) {
            //            This can happen if the user touches the CameraPreview, while the preview is not
            //            started. In this case we do nothing.
            Timber.e(e);
        }


        return false;
    }


    private Rect getFocusRect(PointF touchScreen) {

        // The camera field of view is normalized so that -1000,-1000 is top left and 1000, 1000 is
        // bottom right. Note that multiple areas are possible, but currently only one is used.

        int orientation = calculatePreviewOrientation(getContext(), mCameraInfo);

        // Transform the point:
        PointF rotatedPoint = rotatePoint(touchScreen, orientation);
        PointF scaledPoint = scalePoint(rotatedPoint, orientation);
        return rectFromPoint(scaledPoint, 50);
    }

    private PointF rotatePoint(PointF point, int orientation) {

        PointF result = new PointF();

        Timber.d("orientation: " + orientation);

        switch (orientation) {
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
        } else {
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


    public boolean isPreviewFitting() {
        return mIsPreviewFitting;
    }

    @SuppressWarnings("deprecation")
    private void initPreview() {

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Timber.d("Preview stopped.");
        } catch (Exception e) {
            Timber.e(e);
        }

        Timber.d("initPreview");

        if (mCamera == null)
            return;

        int orientation = calculatePreviewOrientation(getContext(), mCameraInfo);
        mCamera.setDisplayOrientation(orientation);

        Camera.Parameters params = initParameters();
//        useAutoFocus(params);

//        Now start the preview:
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            Timber.e(e);
        }

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, orientation);

//        Tell the activity if the auto exposure can be locked:
        mCameraPreviewCallback.onExposureLockFound(params.isAutoExposureLockSupported());
////        Tell the activity how we can control the white balance:
//        mCameraPreviewCallback.onWhiteBalanceFound(params.getSupportedWhiteBalance());

    }

    @NonNull
    private Camera.Parameters initParameters() {

        //        Load the camera parameters:
        Camera.Parameters params = mCamera.getParameters();
//        And change the camera parameters:
        Camera.Size pictureSize = initPictureSize(params);
        initPreviewSize(params, pictureSize);
        initFlashModes(params);
        params.setJpegQuality(100);
        mCamera.setParameters(params);

        return params;
    }

    private void initFlashModes(Camera.Parameters params) {
        List<String> flashModes = params.getSupportedFlashModes();
        mCameraPreviewCallback.onFlashModesFound(flashModes);

        // Restore the last used flash mode - if available:
        if (mFlashMode != null)
            params.setFlashMode(mFlashMode);
    }

    @NonNull
    private Camera.Size initPictureSize(Camera.Parameters params) {
        Camera.Size pictureSize = getLargestPictureSize();
        params.setPictureSize(pictureSize.width, pictureSize.height);
        return pictureSize;
    }

    private void initPreviewSize(Camera.Parameters params, Camera.Size pictureSize) {
        List<Camera.Size> cameraSizes = params.getSupportedPreviewSizes();
        Camera.Size previewSize = getPreviewSize(cameraSizes, pictureSize);
        mFrameWidth = previewSize.width;
        mFrameHeight = previewSize.height;
        mIsPreviewFitting = isPreviewFitting(previewSize);
        params.setPreviewSize(mFrameWidth, mFrameHeight);
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

    public void focusOnPoint() {

        PointF focusPoint;
        if (mLastTouchPoint == null)
            focusPoint = getCenterPoint();
        else
            focusPoint = mLastTouchPoint;

        if (focusPoint != null && mCameraPreviewCallback != null) {
            mCameraPreviewCallback.onFocusTouch(focusPoint);
            focusOnScreenPoint(focusPoint);
        }

    }

    @SuppressWarnings("deprecation")
    private boolean isPreviewFitting(Camera.Size previewSize) {
        int width, height;
//        Hack: Before the getWidth/getHeight was used, but on the Galaxy S6 the layout was differently initalized,
//        so that the height returned the entire height without subtracting the height of the camera control layout.
//        Therefore, we calculate the dimension of the preview manually.
//        int height = getHeight();
        // TODO: This is a dirty cast hack, which will only work here, this should be avoided
        Point dim = ((CameraActivity) getContext()).getPreviewDimension();
        if (dim != null) {
            width = dim.x;
            height = dim.y;
        } else {
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

//            We add a little tolerance, because on the Sony XA2 we do not get the exact preview
//            ratio as on the picture ratio:
            if ((Math.abs(ratio - optRatio) <= 0.01f) && (resArea >= bestResArea)) {
                bestResArea = resArea;
                bestSize = size;
            }
        }

        if (bestSize != null)
            return bestSize;

        float bestRatio = 0;
//        Second find the closest ratio, but add a constraint to the width:
        for (Camera.Size size : previewSizes) {
            ratio = (float) size.width / size.height;

            if ((Math.abs(ratio - optRatio) <= Math.abs(bestRatio - optRatio)) &&
                    (size.width > 500)) {
                bestRatio = ratio;
                bestSize = size;
            }
        }

        if (bestSize != null)
            return bestSize;

//        Third take the highest resolution:
        for (Camera.Size size : previewSizes) {

            resArea = size.width * size.height;

            if (resArea >= bestResArea) {
                bestResArea = resArea;
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
    public static int calculatePreviewOrientation(Context context, Camera.CameraInfo info) {

        // Get the rotation of the screen to adjust the preview image accordingly.
        int rotation = CameraActivity.getDisplayRotation(context);

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

        try {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(flashMode);
            mCamera.setParameters(params);

            mFlashMode = flashMode;
        } catch (RuntimeException e) {

        }
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

        Timber.d("initCamera:");
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

        int cameraOrienation = calculatePreviewOrientation(getContext(), mCameraInfo);
        mCamera.setDisplayOrientation(cameraOrienation);

        mLastTouchPoint = null;

        // Tell the dependent Activity that the frame dimension (might have) change:
        mCameraPreviewCallback.onFrameDimensionChange(mFrameWidth, mFrameHeight, cameraOrienation);

    }


}