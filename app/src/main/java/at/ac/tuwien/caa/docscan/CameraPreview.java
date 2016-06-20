package at.ac.tuwien.caa.docscan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "OpenCVView";
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private int mFrameWidth;
    private int mFrameHeight;
    private byte[] mFrame;
    private boolean mThreadRun;
    private byte[] mBuffer;
    private int mFrameSize;
    private Bitmap mBitmap;
    private int[] mRGBA;
    private Activity mActivity;
    private int  frameCounter;
    private long lastNanoTime;
    private boolean isCameraReady;
    private int orientation;

    public CameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);

        Log.i(TAG, "Instantiated new " + this.getClass());

        isCameraReady = false;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public void setPreview() throws IOException {



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            mCamera.setPreviewTexture(new SurfaceTexture(10));

        }
        else
            mCamera.setPreviewDisplay(null);
    }

    public boolean openCamera() {

        Log.i(TAG, "openCamera");
        releaseCamera();
        mCamera = Camera.open();


        if (mCamera == null) {
            Log.e(TAG, "Can't open camera!");
            return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {

                synchronized (CameraPreview.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    CameraPreview.this.notify();
                }

                camera.addCallbackBuffer(mBuffer);

            }
        });
        return true;

    }

    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        mThreadRun = false;
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewStopped();
    }

    public void setupCamera(SurfaceHolder holder, int width, int height) {

        Log.i(TAG, "setupCamera");
        synchronized (this) {
            mHolder = holder;
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();

                mFrameWidth = width;
//                mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    int minDiff = Integer.MAX_VALUE;
                    for (Camera.Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = size.width;
                            mFrameHeight = size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }

//                mFrameHeight = 432;
//                mFrameWidth = 768;

                params.setPreviewSize(getFrameWidth(), getFrameHeight());

                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                mCamera.setParameters(params);

                /* Allocate the buffer */
                params = mCamera.getParameters();
                int size = params.getPreviewSize().width * params.getPreviewSize().height;
                size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
//                size = 777600;
                Log.d(TAG, "Size: " + size);
                mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
                mFrame = new byte[size];
                mCamera.addCallbackBuffer(mBuffer);
//                try {
//                    setPreview();
////                    mCamera.setPreviewDisplay(holder);
//
//                } catch (IOException e) {
//                    Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
//                }


                onPreviewStarted(params.getPreviewSize().width, params.getPreviewSize().height);

                /* Now we can start a preview */
                try {

//                    MainActivity.setCameraDisplayOrientation(mCamera);
                    frameCounter = 0;
                    lastNanoTime = System.nanoTime();

                    mCamera.startPreview();

                }
                catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }

                try {
                    setPreview();
                } catch (IOException e) {
                    Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
                }

            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");

//        releaseCamera();
//        isCameraReady = openCamera();
        orientation = getDisplayOrientation();

        setupCamera(holder, width, height);

        (new Thread(this)).start();

    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {

            if (mFrame != null) {

                Bitmap previewBitmap = null;

                // Must be synchronized, otherwise the image is not synchronized:
                synchronized (this) {
//                                    try {
//                                        this.wait();

                    previewBitmap = getPreviewBitmap(mFrame);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                }

                if (previewBitmap != null) {
                    Canvas canvas = mHolder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(previewBitmap, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    /**
     * This method is called when the preview process is being started. It is called before the first frame delivered and processFrame is called
     * It is called with the width and height parameters of the preview process. It can be used to prepare the data needed during the frame processing.
     *
     * @param previewWidth  - the width of the preview frames that will be delivered via processFrame
     * @param previewHeight - the height of the preview frames that will be delivered via processFrame
     */
    protected void onPreviewStarted(int previewWidth, int previewHeight) {
        mFrameSize = previewWidth * previewHeight;
        mRGBA = new int[mFrameSize];
//        if (orientation == 90)
            mBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
//        else
//            mBitmap = Bitmap.createBitmap(previewHeight, previewWidth, Bitmap.Config.ARGB_8888);
    }

    /**
     * This method is called when preview is stopped. When this method is called the preview stopped and all the processing of frames already completed.
     * If the Bitmap object returned via processFrame is cached - it is a good time to recycle it.
     * Any other resources used during the preview can be released.
     */
    protected void onPreviewStopped() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mRGBA = null;


    }

    protected Bitmap getPreviewBitmap(byte[] data) {

        frameCounter++;
        if(frameCounter >= 30)
        {
            int fps = (int) (frameCounter * 1e9 / (System.nanoTime() - lastNanoTime));
            Log.i(TAG, "FPS: "+ fps + " " + getFrameWidth() +" x " + getFrameHeight());


            frameCounter = 0;
            lastNanoTime = System.nanoTime();
        }

        int[] rgba = mRGBA;

        Bitmap previewBitmap = mBitmap;
//        if (orientation == 0)
//            NativeWrapper.handleFrame2(getFrameHeight(), getFrameWidth(), data, previewBitmap);
//        else
            NativeWrapper.handleFrame2(getFrameWidth(), getFrameHeight(), data, previewBitmap);

//        if (orientation == 0 && previewBitmap != null) {
//            // create a matrix for the manipulation
//            Matrix matrix = new Matrix();
//            // resize the bit map
////            matrix.postScale(, scaleHeight);
//            // rotate the Bitmap
//            matrix.postRotate(90);
//            previewBitmap = Bitmap.createBitmap(previewBitmap, 0, 0, previewBitmap.getWidth(), previewBitmap.getHeight(), matrix, true);
//        }

        // recreate the new Bitmap
//        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOriginal, 0, 0, widthOriginal, heightOriginal, matrix, true);

//        Bitmap previewBitmap = mBitmap;
//        previewBitmap.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());

        return previewBitmap;
    }

    public void setActivity(Activity activity) {

        mActivity = activity;

    }

    private int getDisplayOrientation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
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
        return degrees;
    }


//    public native void FindFeatures(int width, int height, byte yuv[], int[] rgba);

//    static {
//        System.loadLibrary("opencv-jni");
//    }

    private native void setup(int width, int height);

    private native void handleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap);

    private native void tearDown();
}