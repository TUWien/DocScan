package at.ac.tuwien.caa.docscan.camera.threads.crop;

public class PageDetectionTask extends CropTask {


    PageDetectionTask() {

        mRunnable = new PageDetectionRunnable(this);

    }
}
