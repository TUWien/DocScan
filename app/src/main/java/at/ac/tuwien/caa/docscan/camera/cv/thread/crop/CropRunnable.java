package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.util.Log;

import java.io.File;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.MESSAGE_COMPLETED_TASK;

public abstract class CropRunnable implements Runnable {

    private static final String CLASS_NAME = "CropRunnable";

    // Defines a field that contains the calling object of type ImageProcessTask.
    final protected TaskRunnableCropMethods mCropTask;

    interface TaskRunnableCropMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setCropThread(Thread currentThread);
        void handleState(int state);
        File getFile();
        void setFile(File file);

    }

    protected abstract void performTask(String fileName);

//    public CropRunnable() {}

    /**
     * This constructor creates an instance of PageDetectionRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param cropTask The ImageProcessTask, which implements TaskRunnableCropMethods
     */
    public CropRunnable(TaskRunnableCropMethods cropTask) {
        mCropTask = cropTask;
    }


    @Override
    public void run() {

        Log.d(CLASS_NAME, "run:");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        File file = mCropTask.getFile();

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            String fileName = file.getAbsolutePath();

//            Perform here the task:
            performTask(fileName);

            mCropTask.handleState(MESSAGE_COMPLETED_TASK);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

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
