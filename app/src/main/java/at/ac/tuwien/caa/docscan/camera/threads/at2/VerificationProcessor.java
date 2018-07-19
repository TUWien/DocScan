package at.ac.tuwien.caa.docscan.camera.threads.at2;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector2;

/**
 * A class that is used to determine if the current frame is the same as the one on which the other
 * image processing steps (page detection and so on) have been applied.
 */
public class VerificationProcessor extends ImageProcessor {

    protected VerificationProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        super(imageProcessorCallback, mat);

    }

    @Override
    protected void process() {

        if (ChangeDetector2.getInstance().isSameFrame(mMat))
            mImageProcessorCallback.handleState(IPManager.MESSAGE_FRAME_VERIFIED, mMat);
        else
            mImageProcessorCallback.handleState(IPManager.MESSAGE_FRAME_NOT_VERIFIED, mMat);


    }
}
