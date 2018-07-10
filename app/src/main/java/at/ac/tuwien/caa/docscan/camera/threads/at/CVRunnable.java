package at.ac.tuwien.caa.docscan.camera.threads.at;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

public abstract class CVRunnable implements Runnable {

    final TaskRunnableCVMethods mCVTask;

    interface TaskRunnableCVMethods {
        void recycle();
        void setMat(Mat mat);
        Mat getMat();
        void setPolyRect(DkPolyRect[] polyRects);
        void setPatch(Patch[] patch);
        Patch[] getPatch();
        DkPolyRect[] getPolyRect();
        void handleState(int state);
    }

    public CVRunnable(TaskRunnableCVMethods cvTask) {

        mCVTask = cvTask;

    }
}
