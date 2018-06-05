package at.ac.tuwien.caa.docscan.camera.threads;

import java.io.File;

public class MapTask implements MapRunnable.TaskRunnableMapMethods{

    private File mFile;

    private Runnable mMapRunnable;
    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /*
     * An object that contains the ThreadPool singleton.
     */
//    private static MapManager sCropManager;
    private static CropManager sCropManager;

    MapTask() {

        mMapRunnable = new MapRunnable(this);
//        sCropManager = MapManager.getInstance();

    }

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

    /**
     * Initializes the task.
     */
    void initializeMapTask(CropManager mapManager) {
        sCropManager = mapManager;
    }

//    /**
//     * Initializes the task.
//     */
//    void initializeMapTask(MapManager mapManager) {
//        sCropManager = mapManager;
//    }

    Runnable getMapRunnable() {
        return mMapRunnable;
    }


}
