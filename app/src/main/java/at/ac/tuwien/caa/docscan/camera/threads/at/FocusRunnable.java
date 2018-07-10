package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.os.Process;
import android.util.Log;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

class FocusRunnable extends CVRunnable {

    private static final String CLASS_NAME = "FocusRunnable";

    public FocusRunnable(FocusTask focusTask) {

        super(focusTask);

    }

    @Override
    public void run() {

        Log.d(CLASS_NAME, "run:");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            Mat mat = mCVTask.getMat();

            Patch[] patches = NativeWrapper.getFocusMeasures(mat);
            mCVTask.handleObject(patches);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
        } finally {

//            // Sets the reference to the current Thread to null, releasing its storage
//            mCropTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }
}
