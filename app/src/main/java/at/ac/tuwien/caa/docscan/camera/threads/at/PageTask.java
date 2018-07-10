package at.ac.tuwien.caa.docscan.camera.threads.at;

public class PageTask extends CVTask {

    PageTask() {
        mRunnable = new PageRunnable(this);
    }
}
