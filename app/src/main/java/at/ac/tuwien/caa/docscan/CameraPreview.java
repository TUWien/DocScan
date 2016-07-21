package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by fabian on 21.07.2016.
 */
public class CameraPreview  extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mDisplayOrientation;
    private int mSurfaceWidth, mSurfaceHeight;

    private Mat mFrameMat;

    private int mFrameWidth;
    private int mFrameHeight;

    private long mLastTime;
    private static long FRAME_TIME_DIFF = 300;

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

    }


    public CameraPreview(Context context, AttributeSet attrs) {

        super(context, attrs);

        mHolder = getHolder();
        mHolder.addCallback(this);

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
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist");
            return;
        }

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

        try {

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            Log.d(TAG, "Camera preview started.");
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }


    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {

//        if (MainActivity.isDebugViewEnabled()) {
//
//            // Take care that in this case the timer is first stopped (contrary to the other calls):
//            mTimerCallbacks.onTimerStopped(TaskTimer.CAMERA_FRAME_ID);
//            mTimerCallbacks.onTimerStarted(TaskTimer.CAMERA_FRAME_ID);
//
//        }


        long currentTime = System.currentTimeMillis();

        if (currentTime - mLastTime >= FRAME_TIME_DIFF) {

            synchronized (this) {

                // 1.5 since YUV
                Mat yuv = new Mat((int)(mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
                yuv.put(0, 0, pixels);


                mFrameMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
                Imgproc.cvtColor(yuv, mFrameMat, Imgproc.COLOR_YUV2RGB_NV21);

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

    }


}