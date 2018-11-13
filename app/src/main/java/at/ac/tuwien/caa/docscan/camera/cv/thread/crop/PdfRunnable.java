package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;

public class PdfRunnable extends CropRunnable{

    private static final String CLASS_NAME = "PdfRunnable";

    public PdfRunnable(TaskRunnableCropMethods cropTask) {
        super(cropTask);
    }

    @Override
    protected void performTask(String fileName) {
        File file = new File(fileName);
        PdfCreator.createPdf(file);
    }

}
