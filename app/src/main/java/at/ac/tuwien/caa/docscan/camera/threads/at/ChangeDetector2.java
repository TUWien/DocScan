package at.ac.tuwien.caa.docscan.camera.threads.at;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.io.File;

import at.ac.tuwien.caa.docscan.logic.Helper;

public class ChangeDetector2 {


    private static final int FRAME_SIZE = 300;
    private static final double CHANGE_THRESH = 0.05;       // A threshold describing a movement between successive frames.
    private static ChangeDetector2 sInstance;

    private Mat mFrame;
    private BackgroundSubtractorMOG2 mNewFrameDetector, mMovementDetector, mVerifyDetector;

    static {

        sInstance = new ChangeDetector2();

    }



    public static ChangeDetector2 getInstance() {

        return sInstance;

    }

//    private ChangeDetector2() {
//
//
//
//    }

    public void initVerifyDetector(Mat frame) {

        Mat mat = resizeMat(frame);

        if (mVerifyDetector != null) {
            mVerifyDetector.clear();
        }

        mVerifyDetector = Video.createBackgroundSubtractorMOG2();
        Mat fgMask = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);

        mVerifyDetector.apply(mat, fgMask, 1);

    }

    public boolean isSameFrame(Mat frame) {

        double changeRatio = getChangeRatio(frame, mVerifyDetector, 0);

        return changeRatio < .01;

    }

    public void initDetectors(Mat frame) {

        if (mFrame != null) {
            mFrame.release();
            mFrame = null;
        }

        mFrame = resizeMat(frame);

//        TODO: check how we can clear the detectors:
        if (mNewFrameDetector != null)
            mNewFrameDetector.clear();

        if (mMovementDetector != null)
            mMovementDetector.clear();

        mNewFrameDetector = Video.createBackgroundSubtractorMOG2();
        mMovementDetector = Video.createBackgroundSubtractorMOG2();

        Mat fgMask = new Mat(mFrame.rows(), mFrame.cols(), CvType.CV_8UC1);

        mNewFrameDetector.apply(mFrame, fgMask, 1);
        mMovementDetector.apply(mFrame, fgMask, 1);


    }

    public boolean isNewFrame(Mat frame) {

        double changeRatio = getChangeRatio(frame, mNewFrameDetector, 0);

        if (changeRatio > .025)
            return true;
        else
            return false;
    }

    public boolean isMoving(Mat mat) {

        if (mFrame == null) {
            initDetectors(mat);
            return false;
        }

        double changeRatio = getChangeRatio(mat, mMovementDetector, 0.8);
        boolean isMoving = changeRatio > CHANGE_THRESH;

        return isMoving;

    }

//    private double getChangeRatioV(Mat mat, BackgroundSubtractorMOG2 subtractor, double learnRate) {
//
//        Mat resizedMat = resizeMat(mat);
//        Mat fgMask = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
//
//        subtractor.apply(resizedMat, fgMask, learnRate);
//
//        Mat thMat = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
//        Imgproc.threshold(fgMask, thMat, 1, 1, Imgproc.THRESH_BINARY); // 127 is a shadow, but the majority of the pixels is classified as shadow - we do not know why.
//
//        Scalar fgPixels = Core.sumElems(thMat);
//
//        double changeRatio = fgPixels.val[0] / (fgMask.rows() * fgMask.cols());
//
//        if (changeRatio > 0.01) {
//            File file = new File(Helper.getMediaStorageDir("DocScan"), "fgmask.jpg");
//            Imgcodecs.imwrite(file.getAbsolutePath(), thMat);
//        }
//
//        return changeRatio;
//
//    }

    private double getChangeRatio(Mat mat, BackgroundSubtractorMOG2 subtractor, double learnRate) {

        Mat resizedMat = resizeMat(mat);
        Mat fgMask = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);

        subtractor.apply(resizedMat, fgMask, learnRate);
        Imgproc.threshold(fgMask, fgMask, 1, 1, Imgproc.THRESH_BINARY);

        Scalar fgPixels = Core.sumElems(fgMask);

        double changeRatio = fgPixels.val[0] / (fgMask.rows() * fgMask.cols());

        return changeRatio;

    }

    private Mat resizeMat(Mat mat) {

        if (mat.cols() == 0 || mat.rows() == 0)
            return null;

//        The frame height might be larger than the frame width:
        double resizeFac = mat.cols() > mat.rows() ? (double) FRAME_SIZE /
                (double) mat.rows() : (double) FRAME_SIZE / (double) mat.cols();

        int width = (int) Math.round(mat.cols() * resizeFac);
        int height = (int) Math.round(mat.rows() * resizeFac);

        Mat result = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(mat, result, Imgproc.COLOR_RGB2GRAY);
        Imgproc.resize(result, result, new Size(width, height));

        return result;
    }



}
