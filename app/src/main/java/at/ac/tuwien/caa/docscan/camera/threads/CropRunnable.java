package at.ac.tuwien.caa.docscan.camera.threads;

import android.graphics.PointF;
import android.location.Location;
import android.media.ExifInterface;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.GPS;
import at.ac.tuwien.caa.docscan.camera.LocationHandler;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.logic.AppState;
import at.ac.tuwien.caa.docscan.logic.DataLog;

public class CropRunnable implements Runnable {

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableCropMethods mCropTask;


    interface TaskRunnableCropMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setCropThread(Thread currentThread);

        File getFile();
        void setFile(File file);

    }

    /**
     * This constructor creates an instance of CropRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param cropTask The CropTask, which implements TaskRunnableCropMethods
     */
    CropRunnable(TaskRunnableCropMethods cropTask) {
        mCropTask = cropTask;
    }


    @Override
    public void run() {

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        File file = mCropTask.getFile();

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

//            This is where the magic happens :)

            ArrayList<PointF> points = Cropper.findRect(file.getAbsolutePath());
            if (points != null && points.size() > 0) {

                Cropper.savePointsToExif(file.getAbsolutePath(), points);

            }
                // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            // If the file is null, reports that the cropping failed.
            if (file == null) {
//                mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
            }

            // Sets the reference to the current Thread to null, releasing its storage
            mCropTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }



}
