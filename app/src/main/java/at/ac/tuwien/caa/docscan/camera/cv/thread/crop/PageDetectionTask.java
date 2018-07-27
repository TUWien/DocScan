package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class PageDetectionTask extends CropTask {


    PageDetectionTask() {

        mRunnable = new PageDetectionRunnable(this);

    }
}
