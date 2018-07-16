package at.ac.tuwien.caa.docscan.camera.threads;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;

/**
 * Created by fabian on 11.07.2017.
 */

public class PageCallable extends CVCallable {

    private int mFrameCnt;

    public PageCallable(Mat mat, CameraPreview.CVCallback cvCallback, TaskTimer.TimerCallbacks timerCallbacks, int frameCnt) {
        super(mat, cvCallback, timerCallbacks);
        mFrameCnt = frameCnt;
    }

    public int getFrameID() {
        return  mFrameCnt;
    }

    @Override
    public Object call() throws Exception {
        try {
            // check if thread is interrupted before lengthy operation
            if (Thread.interrupted())
                throw new InterruptedException();

//            mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);

            DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mMat);
            mCVCallback.onPageSegmented(polyRects);

            mMat.release();

//            mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }


}
