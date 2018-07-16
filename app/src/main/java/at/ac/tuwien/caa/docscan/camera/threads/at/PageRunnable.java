package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.graphics.PointF;
import android.os.Process;
import android.util.Log;

import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.threads.crop.PageDetector;

import static at.ac.tuwien.caa.docscan.camera.threads.crop.CropManager.MESSAGE_COMPLETED_TASK;

public class PageRunnable extends CVRunnable {


    private final static String CLASS_NAME = "PageRunnable";


    public PageRunnable(PageTask pageTask) {

        super(pageTask);

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

            Mat mat = mCVTask.getMat();
//
            DkPolyRect[] polyRect = NativeWrapper.getPageSegmentation(mat);
            mCVTask.handleObject(polyRect);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {
            Log.d(CLASS_NAME, "InterruptedException");

            // Does nothing

            // In all cases, handle the results
//        } catch (IOException e) {
//            e.printStackTrace();
        } finally {

//            // If the file is null, reports that the cropping failed.
//            if (file == null) {
////                mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
//            }
//
//            // Sets the reference to the current Thread to null, releasing its storage
//            mCropTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }
}
