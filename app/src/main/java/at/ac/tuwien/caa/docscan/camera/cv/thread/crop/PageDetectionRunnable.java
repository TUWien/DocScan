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

        PageDetector.PageFocusResult result = PageDetector.findRectAndFocus(fileName);

        try {
            if (result != null && result.getPoints() != null && result.getPoints().size() > 0)
                PageDetector.savePointsToExif(fileName, result.getPoints(), result.isFocused());
            else
                PageDetector.savePointsToExif(fileName,
                        PageDetector.getNormedDefaultPoints(), true);

        } catch (IOException e) {

        }


    }

//    @Override
//    protected void performTask(String fileName) {
//
//        ArrayList<PointF> points = PageDetector.findRect(fileName);
//
//        try {
//            if (points != null && points.size() > 0)
//                PageDetector.savePointsToExif(fileName, points);
//            else
//                PageDetector.savePointsToExif(fileName,
//                        PageDetector.getNormedDefaultPoints());
//
////            Focus measurement:
//
//        }
//        catch (IOException e) {
//
//        }
//
//
//    }

}
