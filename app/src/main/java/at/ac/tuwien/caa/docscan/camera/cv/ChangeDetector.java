package at.ac.tuwien.caa.docscan.camera.cv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

/**
 * Created by fabian on 12.01.2017.
 */
public class ChangeDetector {

//    TODO: find a better way for resizing!
    private static int mFrameWidth, mFrameHeight;

    private static int FRAME_SIZE = 300;
    private static final String TAG = "ChangeDetector";
    private static final long MIN_STEADY_TIME = 300;        // The time in which there must be no movement.
    private static final double CHANGE_THRESH = 0.05;       // A threshold describing a movement between successive frames.
    private static final double DIFFERENCE_THRESH = 0.1;    // Used to measure the difference between a current frame and the frame that has been written to disk

    private static Mat initMat;
    private static boolean mLastFrameChanged = false;
    private static long mSteadyStartTime;
    private static boolean isInit = false;

//    private static final double PERC_THRESH = 0.05;

    private static BackgroundSubtractorMOG2 bgSubtractor;

    public static void init(Mat frame) {

        isInit = true;
        initMat = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame, initMat, Imgproc.COLOR_RGB2GRAY);

        initFrameSize(frame);

        bgSubtractor = Video.createBackgroundSubtractorMOG2();

    }

    private static void initFrameSize(Mat frame) {

        if (frame.cols() == 0 || frame.rows() == 0)
            return;
//        The frame height might be larger than the frame width:
        double resizeFac = frame.cols() > frame.rows() ? (double) FRAME_SIZE / (double)frame.rows() : (double)FRAME_SIZE / (double)frame.cols();

        mFrameWidth = (int) Math.round(frame.cols() * resizeFac);
        mFrameHeight = (int) Math.round(frame.rows() * resizeFac);


    }


    public static boolean isInitialized() {

        return isInit;

    }

    public static boolean isFrameSteady(Mat frame) {

        if (!isInit || ((frame.rows() != initMat.rows()) || (frame.cols() != initMat.cols()))) {
            init(frame);
            return false;
        }

        double changeRatio = getChangeRatio(frame);
        boolean isCurrentChange = changeRatio > CHANGE_THRESH;
        boolean isFrameSteady = false;

        if (!isCurrentChange) {
//            Check if a change occurred in the last frame:
            if (mLastFrameChanged)
                mSteadyStartTime = System.currentTimeMillis();
            else {
//                Check if there has no change happened during the MIN_STEADY_TIME:
                if ((System.currentTimeMillis() - mSteadyStartTime) > MIN_STEADY_TIME)
                    isFrameSteady = true;
            }
        }


        mLastFrameChanged = isCurrentChange;
        return isFrameSteady;

    }

    public static boolean isFrameDifferent(Mat frame) {

        Mat currentFrame = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame, currentFrame, Imgproc.COLOR_RGB2GRAY);

        Mat subtractResult = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC1);
        Core.absdiff(currentFrame, initMat, subtractResult);
        Imgproc.threshold(subtractResult, subtractResult, 50, 1, Imgproc.THRESH_BINARY);
        Scalar sumDiff = Core.sumElems(subtractResult);
        double diffRatio = sumDiff.val[0] / (currentFrame.cols() * currentFrame.rows());

        return diffRatio > DIFFERENCE_THRESH;

    }

    private static double getChangeRatio(Mat frame) {

        Mat tmp = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(frame, tmp, Imgproc.COLOR_RGB2GRAY);
        Imgproc.resize(tmp, tmp, new Size(mFrameWidth, mFrameHeight));
        Mat fg = new Mat(frame.rows(), mFrameWidth, CvType.CV_8UC1);

        bgSubtractor.apply(tmp, fg, 0.8);
        Imgproc.threshold(fg, fg, 120, 1, Imgproc.THRESH_BINARY); // 127 is a shadow, but the majority of the pixels is classified as shadow - we do not know why.

        Scalar fgPixels = Core.sumElems(fg);
//            double percChanged = fgPixels.val[0] / (mFrameWidth * mFrameHeight);
        double changeRatio = fgPixels.val[0] / (mFrameWidth * mFrameHeight);

        return changeRatio;

    }

}
