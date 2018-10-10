package at.ac.tuwien.caa.docscan.camera.cv.thread.preview;

import android.os.Process;
import org.opencv.core.Mat;

public abstract class ImageProcessor implements Runnable {

    protected enum ProcessorType {
        CHANGE, DUPLICATE, VERIFY, PAGE, FOCUS
    }

    protected Mat mMat;
    protected ImageProcessorCallback mImageProcessorCallback;

    protected abstract void process();

    protected ImageProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        mImageProcessorCallback = imageProcessorCallback;
        mMat = mat;

    }

    @Override
    public void run() {

//        Log.d(getClassName(), "run");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

//            Do the image processing:
            process();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }
    }

    protected interface ImageProcessorCallback {

        void handleState(int type, Mat mat);
        void handleObject(int type, Object[] object, Mat mat);

    }

}
