package at.ac.tuwien.caa.docscan.camera.threads;

import org.opencv.core.Mat;

import java.util.concurrent.Callable;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;

/**
 * Created by fabian on 06.07.2017.
 */

public abstract class CVCallable implements Callable {

    protected Mat mMat;
    protected CameraPreview.CVCallback mCVCallback;
    protected TaskTimer.TimerCallbacks mTimerCallbacks;


    public CVCallable(Mat mat, CameraPreview.CVCallback cvCallback, TaskTimer.TimerCallbacks timerCallbacks) {

        mMat = mat;
        mCVCallback = cvCallback;
        mTimerCallbacks = timerCallbacks;

    }


}
