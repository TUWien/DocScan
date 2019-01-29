package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class PdfTask extends ImageProcessTask {

    PdfTask() {
        mRunnable = new PdfRunnable(this);
    }
}
