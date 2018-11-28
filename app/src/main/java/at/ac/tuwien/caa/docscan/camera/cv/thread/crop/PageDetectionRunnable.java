package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.PointF;

import java.io.IOException;
import java.util.ArrayList;

public class PageDetectionRunnable extends CropRunnable {

    private static final String CLASS_NAME = "PageDetectionRunnable";

    public PageDetectionRunnable(PageDetectionTask pageDetectionTask) {
        super(pageDetectionTask);
    }

    @Override
    protected void performTask(String fileName) {

        ArrayList<PointF> points = PageDetector.findRect(fileName);
        try {
            if (points != null && points.size() > 0)
                PageDetector.savePointsToExif(fileName, points);
            else
                PageDetector.savePointsToExif(fileName,
                        PageDetector.getNormedDefaultPoints());
        }
        catch (IOException e) {

        }
    }

}
