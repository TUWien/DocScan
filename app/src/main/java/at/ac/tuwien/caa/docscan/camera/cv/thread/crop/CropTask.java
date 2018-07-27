package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;

public class CropTask implements CropRunnable.TaskRunnableCropMethods {

    private File mFile;

    Runnable mRunnable;
    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /*
     * An object that contains the ThreadPool singleton.
     */
//    private static MapManager sCropManager;
    private static CropManager sCropManager;

    @Override
    public void setCropThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void handleState(int state) {
        sCropManager.handleState(this, state);
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

    public void recycle() {

        mFile = null;

    }


    /**
     * Initializes the task.
     */
    void initializeTask(CropManager mapManager) {
        sCropManager = mapManager;
    }

    Runnable getRunnable() {
        return mRunnable;
    }


}
