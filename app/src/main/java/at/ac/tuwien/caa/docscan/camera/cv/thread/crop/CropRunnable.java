package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.logic.Document;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.MESSAGE_COMPLETED_TASK;

public abstract class CropRunnable implements Runnable {

    private static final String CLASS_NAME = "CropRunnable";

    // Defines a field that contains the calling object of type ImageProcessTask.
    final protected TaskRunnableCropMethods mCropTask;

    interface TaskPdfMethods extends TaskRunnableCropMethods{

        void setFiles(ArrayList<File> files);
        ArrayList<File> getFiles();
        void setPdfName(String pdfName);
        String getPdfName();
        void setContext(WeakReference<Context> context);
        WeakReference<Context> getContext();
        boolean getPerformOCR();
    }

    interface TaskRunnableCropMethods {

        /**
         * Sets the Thread that this instance is running on
         * @param currentThread the current Thread
         */
        void setCropThread(Thread currentThread);
        void handleState(int state);
        File getFile();
        void setFile(File file);
        Document getDocument();
        void setDocument(Document document);

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
//        Document document = mCropTask.getDocument();

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (!(mCropTask instanceof PdfProcessTask))
                performTask(file.getAbsolutePath());
            else {
                ArrayList<File> files = ((PdfProcessTask) mCropTask).getFiles();
                String pdfName = ((PdfProcessTask) mCropTask).getPdfName();
                boolean performOCR = ((PdfProcessTask) mCropTask).getPerformOCR();
                WeakReference<Context> context = ((PdfProcessTask) mCropTask).getContext();
                ((PdfRunnable) this).performTask(performOCR, pdfName, files, context);

            }

//            String fileName;
//            if (document != null){
//                StringBuilder sb = new StringBuilder();
//                sb.append(document.getTitle());
//                sb.append("<");
//                for (File f : document.getFiles()){
//                    sb.append(f.getAbsolutePath());
//                    sb.append(">");
//                }
//                sb.deleteCharAt(sb.lastIndexOf(">"));
//                fileName = sb.toString();
//            } else {
//                fileName = file.getAbsolutePath();
//            }
//
////            Perform here the task:
//            performTask(fileName);

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
