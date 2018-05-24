package at.ac.tuwien.caa.docscan.camera.threads;

import java.io.File;

public class CropTask  implements CropRunnable.TaskRunnableCropMethods{

    private File mFile;

    private Runnable mCropRunnable;
    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /*
     * An object that contains the ThreadPool singleton.
     */
    private static CropManager sCropManager;

    CropTask() {

        mCropRunnable = new CropRunnable(this);
        sCropManager = CropManager.getInstance();

    }

    @Override
    public void setCropThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public File getFile() {
        return mFile;
    }

    @Override
    public void setFile(File file) {
        mFile = file;
    }

    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    public void setCurrentThread(Thread thread) {
        synchronized(sCropManager) {
            mCurrentThread = thread;
        }
    }

    /**
     * Initializes the task.
     */
    void initializeCropTask(CropManager cropManager) {
        sCropManager = cropManager;
    }

    Runnable getCropRunnable() {
        return mCropRunnable;
    }

}
