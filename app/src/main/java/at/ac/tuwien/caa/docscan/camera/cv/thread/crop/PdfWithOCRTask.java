package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class PdfWithOCRTask extends ImageProcessTask {

    PdfWithOCRTask() {
        mRunnable = new PdfWithOCRRunnable(this);
    }
}
