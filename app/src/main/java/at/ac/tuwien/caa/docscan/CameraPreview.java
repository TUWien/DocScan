package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

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
    private boolean mThreadRun = false;

    private static String LOGTAG = "CameraPreview";

    public CameraPreview(Context context) {
        super(context);
//        mCamera = camera;



        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
//        openCamera();

        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


//        openCamera();

    }

//    @Override
//    public void onPreviewFrame(byte[] data, Camera camera)
//    {
////        Log.d(TAG, "onpreviewframe");
//        synchronized (CameraPreview.this) {
//            frameCounter++;
//            System.arraycopy(data, 0, mFrame, 0, data.length);
//            CameraPreview.this.notify();
//        }
//
//    }
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

    protected void onPreviewStopped() {

        if(mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
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

        releaseCamera();

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

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            mFrameWidth = width;
            mFrameHeight = height;

            // selecting optimal camera preview size
            {
                int  minDiff = Integer.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - height) < minDiff) {
                        mFrameWidth = size.width;
                        mFrameHeight = size.height;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }

            params.setPreviewSize(getFrameWidth(), getFrameHeight());

            List<String> FocusModes = params.getSupportedFocusModes();
            if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            mCamera.setParameters(params);
//
//            mCamera.setPreviewDisplay(mHolder);
//            mCamera.setPreviewCallback(this);
//            int size = params.getPreviewSize().width * params.getPreviewSize().height;
//            int size = width * height;
//            size  = size * ImageFormat.getBitsPerPixel(format) / 8;
//            mBuffer = new byte[size];
//                /* The buffer where the current frame will be copied */
//            mFrame = new byte [size];
//            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);


            params = mCamera.getParameters();
            int size = params.getPreviewSize().width * params.getPreviewSize().height;
            size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
            mBuffer = new byte[size];
                /* The buffer where the current frame will be copied */
            mFrame = new byte [size];
            mCamera.addCallbackBuffer(mBuffer);

            mCamera.setPreviewTexture( new SurfaceTexture(10) );

//            try {
//                setPreview();
//            } catch (IOException e) {
//                Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
//            }

//            mCamera.setPreviewCallbackWithBuffer(this);
            mBitmap = Bitmap.createBitmap(params.getPictureSize().width, params.getPictureSize().height, Bitmap.Config.ARGB_8888);
            mCamera.startPreview();

            Log.d(TAG, "Camera started!!!!!!!!!!!!!!!!");

//            mFrameSize = previewWidtd * previewHeight;
//            mRGBA = new int[mFrameSize];


        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    //To create a buffer of the preview bytes size
    private byte[] previewBuffer() {
        Log.d("Function", "previewBuffer iniciado");
        int bufferSize;
        byte buffer[];
        int bitsPerPixel;

        Camera.Parameters mParams = mCamera.getParameters();
        Camera.Size mSize = mParams.getPreviewSize();
        Log.d("Function", "previewBuffer: preview size=" + mSize.height + " " + mSize.width);
        int mImageFormat = mParams.getPreviewFormat();

        if (mImageFormat == ImageFormat.YV12) {
            int yStride = (int) Math.ceil(mSize.width / 16.0) * 16;
            int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
            int ySize = yStride * mSize.height;
            int uvSize = uvStride * mSize.height / 2;
            bufferSize = ySize + uvSize * 2;
            buffer = new byte[bufferSize];
            Log.d("Function", "previewBuffer: buffer size=" + Integer.toString(bufferSize));
            return buffer;
        }

        bitsPerPixel = ImageFormat.getBitsPerPixel(mImageFormat);
        bufferSize = (int) (mSize.height * mSize.width * ((bitsPerPixel / (float) 8)));
        buffer = new byte[bufferSize];
        Log.d("Function", "previewBuffer: buffer size=" + Integer.toString(bufferSize));
        return buffer;
    }



    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        releaseCamera();
        mCamera = Camera.open();

        if(mCamera == null) {
            Log.e(TAG, "Can't open camera!");
            return false;
        }

        mCamera.addCallbackBuffer(mBuffer);

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (CameraPreview.this) {
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    CameraPreview.this.notify();
                    Log.d(TAG, "onpreviewframe");
//                    camera.addCallbackBuffer(mBuffer);
                }

            }
        });

        mCamera.addCallbackBuffer(mBuffer);

        return true;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }


    public void run() {

        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");

        while (mThreadRun) {

            synchronized (this) {
                updateFPSOutput();
//                try {
                    Log.d(TAG, "before wait");
//                    this.wait();
                    Log.d(TAG, "after wait");
                    //            Bitmap bitmap = null;
                    NativeWrapper.handleFrame(mFrameWidth, mFrameHeight, mFrame, mBitmap);
//
//
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

            }

            if (mBitmap != null) {

                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    mBitmap.eraseColor(Color.GREEN);
                    if (mFrame != null) {
//                        mBitmap = BitmapFactory.decodeByteArray(mFrame, 0, mFrame.length);
                        Log.d(TAG, "decoding bitmap");
                    }

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