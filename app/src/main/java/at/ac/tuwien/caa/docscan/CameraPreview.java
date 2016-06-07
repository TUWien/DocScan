package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Runnable {

    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private byte[] mFrame;
    private byte[] mBuffer;
    private int frameCounter = 0;
    private long lastNanoTime;
    private Bitmap mBitmap;
    private int mFrameWidth;
    private int mFrameHeight;

    private static String LOGTAG = "CameraPreview";

    public CameraPreview(Context context) {
        super(context);
//        mCamera = camera;



        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        openCamera();

        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


//        openCamera();

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
//        Log.d(TAG, "onpreviewframe");
        synchronized (CameraPreview.this) {
            frameCounter++;
            System.arraycopy(data, 0, mFrame, 0, data.length);
            CameraPreview.this.notify();
        }

    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
//        try {
//            mCamera.setPreviewDisplay(holder);

//            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//                public void onPreviewFrame(byte[] data, Camera camera) {
//                    synchronized (CameraPreview.this) {
//                        frameCounter++;
//                        System.arraycopy(data, 0, mFrame, 0, data.length);
//                        CameraPreview.this.notify();
//                    }
//                    camera.addCallbackBuffer(mBuffer);
//                }
//            });
//
//            mCamera.startPreview();


            frameCounter = 0;
            lastNanoTime = System.nanoTime();

            (new Thread(this)).start();

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

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
//            mCamera.setPreviewDisplay(mHolder);
//
//            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
//                public void onPreviewFrame(byte[] data, Camera camera) {
//                    synchronized (CameraPreview.this) {
//                        frameCounter++;
//                        System.arraycopy(data, 0, mFrame, 0, data.length);
//                        CameraPreview.this.notify();
//                    }
//                    camera.addCallbackBuffer(mBuffer);
//                }
//            });
//            mCamera.startPreview();

            mFrameWidth = width;
            mFrameHeight = height;

            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(width, height);
//            mCamera.setParameters(params);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
//            int size = params.getPreviewSize().width * params.getPreviewSize().height;
            int size = width * height;
            size  = size * ImageFormat.getBitsPerPixel(format) / 8;
            mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
            mFrame = new byte [size];
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

//            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();

//            mFrameSize = previewWidtd * previewHeight;
//            mRGBA = new int[mFrameSize];


        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }



    public boolean openCamera() {
        Log.i(TAG, "openCamera");
//        releaseCamera();
        mCamera = Camera.open();

        if(mCamera == null) {
            Log.e(TAG, "Can't open camera!");
            return false;
        }


        return true;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }


    public void run() {
        boolean mThreadRun = true;
        Log.i(TAG, "Starting processing thread");

        while (mThreadRun) {

            synchronized (this) {
                updateFPSOutput();
                try {
                    this.wait();
                    //            Bitmap bitmap = null;
                    NativeWrapper.handleFrame(mFrameWidth, mFrameHeight, mFrame, mBitmap);



                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            if (mBitmap != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(mBitmap, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }

            }

//            NativeWrapper.logPolar(new Mat(), new Mat(), (float) 1, (float)1, (double) 1, (double) 1, (double) 1);


//            Bitmap bmp = null;
//
//            synchronized (this) {
//                try {
//                    this.wait();
//                    bmp = processFrame(mFrame);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            if (bmp != null) {
//                Canvas canvas = mHolder.lockCanvas();
//                if (canvas != null) {
//                    canvas.drawBitmap(bmp, (canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
//                    mHolder.unlockCanvasAndPost(canvas);
//                }
//            }



        }
    }

    private void updateFPSOutput() {

        if(frameCounter >= 30) {


//            Log.i(LOGTAG, "frames: " + frameCounter);
            final int fps = (int) (frameCounter * 1e9 / (System.nanoTime() - lastNanoTime));
            Log.i(LOGTAG, "drawFrame() FPS: " + fps);

            lastNanoTime = System.nanoTime();
            frameCounter = 0;

        }

    }




}