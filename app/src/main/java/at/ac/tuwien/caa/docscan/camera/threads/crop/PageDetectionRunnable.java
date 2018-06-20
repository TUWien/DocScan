package at.ac.tuwien.caa.docscan.camera.threads.crop;

import android.graphics.PointF;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static at.ac.tuwien.caa.docscan.camera.threads.crop.CropManager.MESSAGE_COMPLETED_TASK;

public class PageDetectionRunnable extends CropRunnable {

    private static final String CLASS_NAME = "PageDetectionRunnable";

    public PageDetectionRunnable(PageDetectionTask pageDetectionTask) {
        super(pageDetectionTask);
    }

    @Override
    public void run() {

        Log.d(CLASS_NAME, "run:");

        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        File file = mCropTask.getFile();

        try {
            // Before continuing, checks to see that the Thread hasn't been
            // interrupted
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }


            ArrayList<PointF> points = PageDetector.findRect(file.getAbsolutePath());
            if (points != null && points.size() > 0)
                PageDetector.savePointsToExif(file.getAbsolutePath(), points);
            else
                PageDetector.savePointsToExif(file.getAbsolutePath(),
                        PageDetector.getNormedDefaultPoints());

//            Thread.sleep(3000);

            mCropTask.handleState(MESSAGE_COMPLETED_TASK);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

            // Does nothing

            // In all cases, handle the results
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            // If the file is null, reports that the cropping failed.
            if (file == null) {
//                mPhotoTask.handleDownloadState(HTTP_STATE_FAILED);
            }

            // Sets the reference to the current Thread to null, releasing its storage
            mCropTask.setCropThread(null);

            // Clears the Thread's interrupt flag
            Thread.interrupted();
        }

    }

}
