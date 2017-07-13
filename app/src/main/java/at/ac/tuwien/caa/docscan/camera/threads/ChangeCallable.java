package at.ac.tuwien.caa.docscan.camera.threads;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.ChangeDetector;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.MOVEMENT_CHECK;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.NEW_DOC;

/**
 * Created by fabian on 13.07.2017.
 */

public class ChangeCallable extends CVCallable {

    public ChangeCallable(Mat mat, CameraPreview.CVCallback cvCallback, TaskTimer.TimerCallbacks timerCallbacks) {
        super(mat, cvCallback, timerCallbacks);
    }

    @Override
    public Boolean call() throws Exception {
        try {
            // check if thread is interrupted before lengthy operation
            if (Thread.interrupted())
                throw new InterruptedException();

            boolean isFrameSteady;
            //            Watch out for movements:
            if (ChangeDetector.isMovementDetectorInitialized()) {

                mTimerCallbacks.onTimerStarted(MOVEMENT_CHECK);
                isFrameSteady = ChangeDetector.isFrameSteady(mMat);
                mTimerCallbacks.onTimerStopped(MOVEMENT_CHECK);

                if (!isFrameSteady) {
                    mCVCallback.onMovement(true);
                    return false;
                } else {
                    mCVCallback.onMovement(false);
                }

            }

//            Watch out for new frames:
            if (ChangeDetector.isNewFrameDetectorInitialized()) {
                mTimerCallbacks.onTimerStarted(NEW_DOC);
                boolean isFrameDifferent = ChangeDetector.isNewFrame(mMat);
                mTimerCallbacks.onTimerStopped(NEW_DOC);

                if (!isFrameDifferent) {
                    mCVCallback.onWaitingForDoc(true);
                    return false;
                } else {
                    mCVCallback.onWaitingForDoc(false);
                    mTimerCallbacks.onTimerStarted(FLIP_SHOT_TIME);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }
}
