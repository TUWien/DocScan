package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import java.io.File;

import at.ac.tuwien.caa.docscan.logic.deprecated.Helper;

public class RotateRunnable extends CropRunnable {

    public RotateRunnable(TaskRunnableCropMethods cropTask) {
        super(cropTask);
    }

    @Override
    protected void performTask(String fileName) {

        File file = new File(fileName);
        Helper.rotateExif(file);

    }
}



