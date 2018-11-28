package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;
import java.util.ArrayList;

public class PdfWithOCRRunnable extends CropRunnable{

    private static final String CLASS_NAME = "PdfWithOCRRunnable";

    public PdfWithOCRRunnable(TaskRunnableCropMethods cropTask) {
        super(cropTask);
    }

    @Override
    protected void performTask(String documentFiles) {
        String[] fileNames = documentFiles.split(">");
        ArrayList<File> files = new ArrayList<>();
        for (String fileName : fileNames) {
            File file = new File(fileName);
            files.add(file);
            //PdfCreator.createPdf(file);
        }
        PdfCreator.createPdfWithOCR(files);
    }

}
