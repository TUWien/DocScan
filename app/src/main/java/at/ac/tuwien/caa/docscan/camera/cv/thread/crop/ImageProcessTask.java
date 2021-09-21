package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;

import at.ac.tuwien.caa.docscan.logic.Document;

public class ImageProcessTask implements CropRunnable.TaskRunnableCropMethods {

    private File mFile;
    private Document mDocument;

    Runnable mRunnable;
    // The Thread on which this task is currently running.
    private Thread mCurrentThread;

    /*
     * An object that contains the ThreadPool singleton.
     */
//    private static MapManager sImageProcessor;
    private static ImageProcessor sImageProcessor;

    @Override
    public void setCropThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void handleState(int state) {
        sImageProcessor.handleState(this, state);
    }

    @Override
    public File getFile() {
        return mFile;
    }

    @Override
    public void setFile(File file) {
        mFile = file;
    }

    @Override
    public Document getDocument() {
        return mDocument;
    }

    @Override
    public void setDocument(Document document) {
        mDocument = document;
    }

    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    public void setCurrentThread(Thread thread) {
        synchronized (sImageProcessor) {
            mCurrentThread = thread;
        }
    }

    public void recycle() {

        mFile = null;
        mDocument = null;

    }


    /**
     * Initializes the task.
     */
    void initializeTask(ImageProcessor mapManager) {
        sImageProcessor = mapManager;
    }

    Runnable getRunnable() {
        return mRunnable;
    }


}
