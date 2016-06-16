package at.ac.tuwien.caa.docscan;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

/**
 * Created by fabian on 16.06.2016.
 */
public class CameraPreview2 implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private Camera mCamera = null;
    private ImageView MyCameraPreview = null;
    private Bitmap bitmap = null;
    private int[] pixels = null;
    private byte[] FrameData = null;
    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;

    Handler mHandler = new Handler(Looper.getMainLooper());

    public CameraPreview2(int PreviewlayoutWidth, int PreviewlayoutHeight,
                         ImageView CameraPreview)
    {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
        MyCameraPreview = CameraPreview;

    }

    @Override
    public void onPreviewFrame(byte[] arg0, Camera arg1)
    {
        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21)
        {
            //We only accept the NV21(YUV420) format.
            if ( !bProcessing )
            {
                FrameData = arg0;
                mHandler.post(DoImageProcessing);
            }
        }
    }

    public void onPause()
    {
        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int width, int height, int arg3)
    {
//        Parameters parameters;

//        parameters = mCamera.getParameters();
//        // Set the camera preview size
//        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

//        mFrameWidth = width;
//        mFrameHeight = height;

        // selecting optimal camera preview size
        {
            int minDiff = Integer.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    PreviewSizeWidth = size.width;
                    PreviewSizeHeight = size.height;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }

//        Log.d(TAG, "height: ")


//                mFrameHeight = 432;
//                mFrameWidth = 768;

        params.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
        pixels = new int[PreviewSizeWidth * PreviewSizeHeight];

        imageFormat = params.getPreviewFormat();

        mCamera.setParameters(params);

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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
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
            bProcessing = true;
//            NativeWrapper.handleFrame2(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);
//            ImageProcessing(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);

            bitmap.setPixels(pixels, 0, PreviewSizeWidth, 0, 0, PreviewSizeWidth, PreviewSizeHeight);
            MyCameraPreview.setImageBitmap(bitmap);
            bProcessing = false;
        }
    };
}