package at.ac.tuwien.caa.docscan.camera.threads.at;

public class ChangeTask extends CVTask {

    ChangeTask() {
        mRunnable = new ChangeRunnable(this);
    }
}
