package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class MapTask extends ImageProcessTask {

    MapTask() {
        mRunnable = new MapRunnable(this);
    }
}
