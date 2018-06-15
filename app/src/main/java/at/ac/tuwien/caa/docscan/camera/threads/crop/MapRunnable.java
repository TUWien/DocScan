package at.ac.tuwien.caa.docscan.camera.threads.crop;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.logic.Helper;

import static at.ac.tuwien.caa.docscan.camera.threads.crop.CropManager.MESSAGE_COMPLETED_TASK;

public class MapRunnable extends CropRunnable{

    private static final String CLASS_NAME = "MapRunnable";

    public MapRunnable(MapTask mapTask) {
        super(mapTask);
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

            String fileName = file.getAbsolutePath();
            performMapping(fileName);

            mCropTask.handleState(MESSAGE_COMPLETED_TASK);

            // Catches exceptions thrown in response to a queued interrupt
        } catch (InterruptedException e1) {

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

    private void performMapping(String fileName) {

        ArrayList<PointF> points = PageDetector.getNormedCropPoints(fileName);

        if (Mapper.replaceWithMappedImage(fileName, points))
            PageDetector.saveAsCropped(fileName);

    }

    private float getDiagonalLength(String fileName) {

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        float length = (float) Math.sqrt(
                options.outWidth * options.outWidth + options.outHeight * options.outHeight);

        return length;

    }

}
