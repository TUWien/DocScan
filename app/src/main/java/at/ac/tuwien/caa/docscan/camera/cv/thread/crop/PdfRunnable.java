package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.content.Context;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PdfRunnable extends CropRunnable {

    private static final String CLASS_NAME = "PdfRunnable";

    public PdfRunnable(TaskRunnableCropMethods cropTask) {
        super(cropTask);

    }

    @Override
    protected void performTask(String fileName) {

    }

    protected void performTask(boolean performOCR, String pdfName, ArrayList<File> files,
                               WeakReference<Context> context) {

        if (performOCR)
            PdfCreator.createPdfWithOCR(pdfName, files, this, context);
        else
            PdfCreator.createPdfWithoutOCR(pdfName, files, this, context);
    }

}
