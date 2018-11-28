package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class PageDetectionTask extends ImageProcessTask {


    PageDetectionTask() {

        mRunnable = new PageDetectionRunnable(this);

    }
}
