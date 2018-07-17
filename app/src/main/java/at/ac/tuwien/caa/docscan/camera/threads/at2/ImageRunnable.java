package at.ac.tuwien.caa.docscan.camera.threads.at2;

import android.os.Process;
import android.util.Log;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

public class ImageRunnable implements Runnable {

    private static final String CLASS_NAME = "ImageRunnable";

    private Mat mMat;
    private ImageProcessorCallback mImageProcessorCallback;

    protected ImageRunnable(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        mImageProcessorCallback = imageProcessorCallback;
        mMat = mat;

    }

    @Override
    public void run() {

        Log.d(CLASS_NAME, "run");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            DkPolyRect[] polyRect = NativeWrapper.getPageSegmentation(mMat);
            mImageProcessorCallback.handleObject(ImageProcessor.TASK_TYPE_PAGE, polyRect, mMat);

            Patch[] patches = NativeWrapper.getFocusMeasures(mMat);
            mImageProcessorCallback.handleObject(ImageProcessor.TASK_TYPE_FOCUS, patches, mMat);



        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
//            mMat.release();
            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }
    }


    public interface ImageProcessorCallback {
        void handleObject(int type, Object[] object, Mat mat);
    }

}
