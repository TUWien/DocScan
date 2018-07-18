package at.ac.tuwien.caa.docscan.camera.threads.at2;

import android.os.Process;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector2;
import at.ac.tuwien.caa.docscan.logic.Helper;

public class ImageChangeRunnable implements Runnable {

    private static final String CLASS_NAME = "ImageChangeRunnable";

    private Mat mMat;
    private ImageChangeCallback mImageChangeCallback;
//    private boolean mCheckForNewFrame;
    private int mCheckState;

    protected ImageChangeRunnable(ImageChangeCallback imageChangeCallback, Mat mat,
                                  int checkState) {

        mImageChangeCallback = imageChangeCallback;
        mMat = mat;
//        mCheckForNewFrame = checkForNewFrame;
        mCheckState = checkState;

    }

    public Mat getMat() {
        return mMat;
    }

    @Override
    public void run() {

        Log.d(CLASS_NAME, "run:");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

//            Check if the frame is different compared to the one used for initialization:

            if (mCheckState == ImageProcessor.CHANGE_TASK_CHECK_NEW_FRAME) {
                if (ChangeDetector2.getInstance().isNewFrame(mMat))
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_NEW_FRAME, mMat);
                else
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_SAME_FRAME, mMat);
            }
            else if (mCheckState == ImageProcessor.CHANGE_TASK_CHECK_MOVEMENT){
                if (ChangeDetector2.getInstance().isMoving(mMat))
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_MOVEMENT, mMat);
                else
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_NO_MOVEMENT, mMat);

            }
            else if (mCheckState == ImageProcessor.CHANGE_TASK_CHECK_VERIFY_FRAME){
                
                if (ChangeDetector2.getInstance().isSameFrame(mMat))
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_VERIFIED_FRAME, mMat);
                else
                    mImageChangeCallback.handleState(ImageProcessor.TASK_TYPE_UNVERIFIED_FRAME, mMat);

            }


            // Catches exceptions thrown in response to a queued interrupt
        }
        catch (InterruptedException e1) {
        }
        finally {

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }

    public interface ImageChangeCallback {
        void handleState(int type, Mat mat);
    }
}
