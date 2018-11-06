package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class RotateTask extends ImageProcessTask {

    RotateTask() {
        mRunnable = new RotateRunnable(this);
    }
}
