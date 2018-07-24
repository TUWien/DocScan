package at.ac.tuwien.caa.docscan.camera.threads.at2;

import android.util.Log;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector;

/**
 * A class used to determine if the current frame is different to the one that was recently
 * photographed.
 */
public class DuplicateProcessor extends ImageProcessor {

    private static final String CLASS_NAME = "DuplicateProcessor";

    protected DuplicateProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        super(imageProcessorCallback, mat);

    }

    @Override
    protected void process() {

        Log.d(CLASS_NAME, "process");

        if (ChangeDetector.getInstance().isNewFrame(mMat))
            mImageProcessorCallback.handleState(IPManager.MESSAGE_NO_DUPLICATE_FOUND, mMat);
        else
            mImageProcessorCallback.handleState(IPManager.MESSAGE_DUPLICATE_FOUND, mMat);

    }
}
