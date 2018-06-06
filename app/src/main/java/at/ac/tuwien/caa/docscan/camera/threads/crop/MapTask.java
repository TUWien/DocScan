package at.ac.tuwien.caa.docscan.camera.threads.crop;

public class MapTask extends CropTask {

    MapTask() {
        mRunnable = new MapRunnable(this);
    }
}
