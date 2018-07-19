package at.ac.tuwien.caa.docscan.camera.threads.at2;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector2;

public class ChangeProcessor extends ImageProcessor {

    protected ChangeProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        super(imageProcessorCallback, mat);

    }

    @Override
    protected void process() {

        if (ChangeDetector2.getInstance().isMoving(mMat))
            mImageProcessorCallback.handleState(IPManager.MESSAGE_CHANGE_DETECTED, mMat);
        else
            mImageProcessorCallback.handleState(IPManager.MESSAGE_NO_CHANGE_DETECTED, mMat);

    }
}
