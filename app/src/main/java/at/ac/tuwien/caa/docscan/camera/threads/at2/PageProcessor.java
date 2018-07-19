package at.ac.tuwien.caa.docscan.camera.threads.at2;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;

public class PageProcessor extends ImageProcessor {

    protected PageProcessor(ImageProcessorCallback imageProcessorCallback, Mat mat) {

        super(imageProcessorCallback, mat);

    }

    @Override
    protected void process() {

        DkPolyRect[] polyRect = NativeWrapper.getPageSegmentation(mMat);
        mImageProcessorCallback.handleObject(IPManager.MESSAGE_PAGE_DETECTED, polyRect, mMat);

    }
}
