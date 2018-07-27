package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

public class MapTask extends CropTask {

    MapTask() {
        mRunnable = new MapRunnable(this);
    }
}
