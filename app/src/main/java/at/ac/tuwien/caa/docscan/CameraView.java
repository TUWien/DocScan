package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.List;

/**
 * Created by fabian on 16.06.2016.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{


    class CameraFrameThread extends Thread {

        private CameraView mCameraView;
        private boolean mIsRunning;
        private int mSurfaceWidth,mSurfaceHeight;

        public CameraFrameThread(CameraView cameraView) {

            mCameraView = cameraView;

        }

        public void setRunning(boolean running) {

            mIsRunning = running;

        }

        public void setSurfaceSize(int width, int height) {

            mSurfaceWidth = width;
            mSurfaceHeight = height;

        }

        @Override
        public void run() {

            byte[] frame;


            while (mIsRunning) {
                // TODO: I am not sure if this access should be synchronized, but I guess:
//                synchronized (mCameraView) {
                frame = mCameraView.getFrame();
                if (frame != null) {
                    Mat mat = new Mat(mSurfaceWidth, mSurfaceHeight, CvType.CV_8UC3);
                    mat.put(0, 0, frame);

                    NativeWrapper.processFrame(mat);
                }

//                }

            }
        }

    }

    private Camera mCamera = null;
    private ImageView MyCameraPreview = null;
    private Bitmap bitmap = null;
    private int[] pixels = null;
    private byte[] data = null;
    private int imageFormat;
    private int previewSizeWidth;
    private int previewSizeHeight;
    private boolean bProcessing = false;
    private SurfaceHolder holder;
    private byte[] mFrame;
    private CameraFrameThread mCameraFrameThread;

    private Handler mHandler;

//    public CameraView(Context context) {
//
//        super(context);
//
//        holder = getHolder();
//        holder.addCallback(this);
//
//
//
//    }
//
//    public CameraView(Context context, AttributeSet attrs, int defStyle) {
//
//        super(context, attrs, defStyle);
//
//        holder = getHolder();
//        holder.addCallback(this);
//
//    }

    public CameraView(Context context, AttributeSet attrs) {


        super(context, attrs);

        holder = getHolder();
        holder.addCallback(this);

        mCameraFrameThread = new CameraFrameThread(this);

    }

    @Override
    public void onPreviewFrame(byte[] pixels, Camera arg1)
    {
        mFrame = pixels;
//        // At preview mode, the frame data will push to here.
//        if (imageFormat == ImageFormat.NV21)
//        {
//            //We only accept the NV21(YUV420) format.
//            if ( !bProcessing )
//            {
//                data = pixels;
//                mHandler.post(DoImageProcessing);
//            }
//        }
    }

    public void onPause()
    {

        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
//        Parameters parameters;

//        parameters = mCamera.getParameters();
//        // Set the camera preview size
//        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        holder = holder;
//        mHandler = new Handler(Looper.getMainLooper());

        mCameraFrameThread.setSurfaceSize(width, height);
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

//        mFrameWidth = width;
//        mFrameHeight = height;

        // selecting optimal camera preview size

        int minDiff = Integer.MAX_VALUE;
        for (Camera.Size size : sizes) {
            if (Math.abs(size.height - height) < minDiff) {
                previewSizeWidth = size.width;
                previewSizeHeight = size.height;
                minDiff = Math.abs(size.height - height);
            }
        }

        // Use autofocus if available:
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        params.setPreviewSize(previewSizeWidth, previewSizeHeight);

        bitmap = Bitmap.createBitmap(previewSizeWidth, previewSizeHeight, Bitmap.Config.ARGB_8888);
        pixels = new int[previewSizeWidth * previewSizeHeight];

        imageFormat = params.getPreviewFormat();

        mCamera.setParameters(params);

        MainActivity.setCameraDisplayOrientation(mCamera);
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        mCamera = Camera.open();
        try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }

        mCameraFrameThread.setRunning(true);
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

        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    protected byte[] getFrame() {

        return mFrame;

    }


    //
    // Native JNI
    //
//    public native boolean ImageProcessing(int width, int height,
//                                          byte[] NV21FrameData, int [] pixels);
//    static
//    {
//        System.loadLibrary("ImageProcessing");
//    }

    private Runnable DoImageProcessing = new Runnable()
    {
        public void run()
        {
//            Log.i("MyRealTimeImageProcessing", "DoImageProcessing():");
//            bProcessing = true;
////            NativeWrapper.handleFrame2(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);
////            ImageProcessing(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);
//
//            bitmap.setPixels(pixels, 0, previewSizeWidth, 0, 0, previewSizeWidth, previewSizeHeight);
//            MyCameraPreview.setImageBitmap(bitmap);
//            bProcessing = false;

            Canvas canvas = holder.lockCanvas();

            if (canvas != null) {
                Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                canvas.drawBitmap(b, (canvas.getWidth() - previewSizeWidth) / 2, (canvas.getHeight() - previewSizeHeight) / 2, null);
                holder.unlockCanvasAndPost(canvas);
            }

        }
    };
}