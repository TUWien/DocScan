package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.os.Process;
import android.util.Log;

import org.opencv.core.Mat;

public class ChangeRunnable extends CVRunnable {

    private static final String CLASS_NAME = "ChangeRunnable";
    protected static final int MESSAGE_FRAME_STEADY = 0;
    protected static final int MESSAGE_FRAME_MOVED = 1;

    public ChangeRunnable(ChangeTask changeTask) {

        super(changeTask);

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

            if (ChangeDetector2.getInstance().isMoving(mat))
                mCVTask.handleState(MESSAGE_FRAME_MOVED);
            else
                mCVTask.handleState(MESSAGE_FRAME_STEADY);


            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
//        } catch (IOException e) {
//            e.printStackTrace();
        } finally {

//            // If the file is null, reports that the cropping failed.
//            if (file == null) {
////                mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
//            }
//
//            // Sets the reference to the current Thread to null, releasing its storage
//            mCropTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }
}
