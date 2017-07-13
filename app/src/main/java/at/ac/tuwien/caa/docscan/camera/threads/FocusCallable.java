package at.ac.tuwien.caa.docscan.camera.threads;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FOCUS_MEASURE;

/**
 * Created by fabian on 11.07.2017.
 */

public class FocusCallable extends CVCallable {

    public FocusCallable(Mat mat, CameraPreview.CVCallback cvCallback, TaskTimer.TimerCallbacks timerCallbacks) {
        super(mat, cvCallback, timerCallbacks);
    }

    @Override
    public Object call() throws Exception {
        try {
            // check if thread is interrupted before lengthy operation
            if (Thread.interrupted())
                throw new InterruptedException();

            mTimerCallbacks.onTimerStarted(FOCUS_MEASURE);

            Patch[] patches = NativeWrapper.getFocusMeasures(mMat);
            mCVCallback.onFocusMeasured(patches);
            mMat.release();

            mTimerCallbacks.onTimerStopped(FOCUS_MEASURE);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return null;
    }

}
