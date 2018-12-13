package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.content.Context;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PdfProcessTask extends ImageProcessTask implements CropRunnable.TaskPdfMethods {

    private ArrayList<File> mFiles;
    private String mPdfName;
    private WeakReference<Context> mContext;
    private boolean mPerformOCR;

    PdfProcessTask(boolean performOCR, ArrayList<File> files, String pdfName) {

        mRunnable = new PdfRunnable(this);
        mPerformOCR = performOCR;
        mFiles = files;
        mPdfName = pdfName;

    }

    @Override
    public void setFiles(ArrayList<File> files) {
        mFiles = files;
    }

    @Override
    public ArrayList<File> getFiles() {
        return mFiles;
    }

    @Override
    public void setPdfName(String pdfName) {
        mPdfName = pdfName;
    }

    @Override
    public String getPdfName() {
        return mPdfName;
    }

    @Override
    public void setContext(WeakReference<Context> context) {
        mContext = context;
    }

    @Override
    public WeakReference<Context> getContext() {
        return mContext;
    }

    @Override
    public boolean getPerformOCR() {
        return mPerformOCR;
    }

}
