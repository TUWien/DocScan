package at.ac.tuwien.caa.docscan.camera.threads.at2;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

public class FocusProcessor extends ImageProcessor {

    protected FocusProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        super(imageProcessorCallback, mat);

    }

    @Override
    protected void process() {

        Patch[] patches = NativeWrapper.getFocusMeasures(mMat);
        mImageProcessorCallback.handleObject(IPManager.MESSAGE_FOCUS_MEASURED, patches, mMat);

    }
}
