package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.PointF;

import java.util.ArrayList;

public class PdfRunnable extends CropRunnable{

    private static final String CLASS_NAME = "PdfRunnable";

    public PdfRunnable(PdfTask pdfTask) {
        super(pdfTask);
    }

    @Override
    protected void performTask(String fileName) {


        System.out.println("test");
    }

}
