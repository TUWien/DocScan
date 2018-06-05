package at.ac.tuwien.caa.docscan.camera.threads;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;

public class MapRunnable implements Runnable {

    // Defines a field that contains the calling object of type PhotoTask.
    final TaskRunnableMapMethods mMapTask;


    interface TaskRunnableMapMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setCropThread(Thread currentThread);
        void handleState(int state);
        File getFile();
        void setFile(File file);

    }

    /**
     * This constructor creates an instance of CropRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param mapTask The CropTask, which implements TaskRunnableCropMethods
     */
    MapRunnable(TaskRunnableMapMethods mapTask) {
        mMapTask = mapTask;
    }


    @Override
    public void run() {

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        File file = mMapTask.getFile();

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

//            This is where the magic happens :)

            String fileName = file.getAbsolutePath();
            ArrayList<PointF> points = Cropper.getNormedCropPoints(fileName);
            Mapper.replaceWithMappedImage(fileName, points);
            mMapTask.handleState(0);

                // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
//        } catch (IOException e) {
//            e.printStackTrace();
        } finally {

            // If the file is null, reports that the cropping failed.
            if (file == null) {
//                mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
            }

            // Sets the reference to the current Thread to null, releasing its storage
            mMapTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }



}
