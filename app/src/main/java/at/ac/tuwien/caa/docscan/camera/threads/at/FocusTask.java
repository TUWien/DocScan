package at.ac.tuwien.caa.docscan.camera.threads.at;

public class FocusTask extends CVTask {

    FocusTask() {
        mRunnable = new FocusRunnable(this);
    }
}
