package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;

public abstract class CropRunnable implements Runnable {

    // Defines a field that contains the calling object of type CropTask.
    final TaskRunnableCropMethods mCropTask;


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

    /**
     * This constructor creates an instance of PageDetectionRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     *
     * @param cropTask The CropTask, which implements TaskRunnableCropMethods
     */
    public CropRunnable(TaskRunnableCropMethods cropTask) {
        mCropTask = cropTask;
    }

}
