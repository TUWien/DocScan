package at.ac.tuwien.caa.docscan.camera.threads.at2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector2;

import static at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect.KEY_POLY_RECT;
import static at.ac.tuwien.caa.docscan.camera.cv.Patch.KEY_FOCUS;

public class IPManager implements ImageProcessor.ImageProcessorCallback {

    public static final int TASK_TYPE_PAGE = 0;
    public static final int TASK_TYPE_FOCUS = 1;
    public static final int TASK_TYPE_MOVEMENT = 2;
    public static final int TASK_TYPE_NO_MOVEMENT = 3;
    public static final int TASK_TYPE_NEW_FRAME = 4;
    public static final int TASK_TYPE_SAME_FRAME = 5;
    public static final int TASK_TYPE_VERIFIED_FRAME = 6;
    public static final int TASK_TYPE_UNVERIFIED_FRAME = 7;

    protected static final int MESSAGE_CHANGE_DETECTED = 0;
    protected static final int MESSAGE_NO_CHANGE_DETECTED = 1;
    protected static final int MESSAGE_PAGE_DETECTED = 2;
    protected static final int MESSAGE_FOCUS_MEASURED = 3;
    protected static final int MESSAGE_DUPLICATE_FOUND = 4;
    protected static final int MESSAGE_NO_DUPLICATE_FOUND = 5;
    protected static final int MESSAGE_FRAME_NOT_VERIFIED = 6;
    protected static final int MESSAGE_FRAME_VERIFIED = 7;

    public static final int CHANGE_TASK_CHECK_MOVEMENT = 0;
    public static final int CHANGE_TASK_CHECK_NEW_FRAME = 1;
    public static final int CHANGE_TASK_CHECK_VERIFY_FRAME = 2;

    private static final long MIN_STEADY_TIME = 1000;        // The time in which there must be no movement.
    private static final long FRAME_TIME_DIFF = 300;
    private static final long NO_TIME_SET = -1;

    private static final String CLASS_NAME = "IPManager";

    private final Executor mExecutor;

    // An object that manages Messages in a Thread
    private Handler mHandler;
    private CameraPreview.CVCallback mCVCallback;
    private boolean mIsRunning = false;
    private int mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
    private CVResult mCVResult;
    private long mLastSteadyTime = NO_TIME_SET;
    private long mLastFrameReceivedTime = NO_TIME_SET;

    //    Singleton:
    private static IPManager sInstance;

    static {
        Log.d(CLASS_NAME, "==========================creating new instance\"==========================");

        sInstance = new IPManager();
    }

    public static IPManager getInstance() {

        return sInstance;

    }

    public void setCVResult(CVResult cVResult) {

        mCVResult = cVResult;

    }

    public void setCVCallback(CameraPreview.CVCallback callback) {

        mCVCallback = callback;

    }


    private IPManager() {

        mExecutor = Executors.newSingleThreadExecutor();

        mHandler = new Handler(Looper.getMainLooper()) {

            /*
             * handleMessage() defines the operations to perform when the
             * Handler receives a new Message to process.
             */
            @Override
            public void handleMessage(Message inputMessage) {

                int message = inputMessage.what;
                Mat mat = (Mat) inputMessage.obj;

// ===================== new message handling here: =====================

                switch (message) {

                    case MESSAGE_CHANGE_DETECTED:

                        Log.d(CLASS_NAME, "handleMessage: onMovement: true");

                        mCVCallback.onMovement(true);
                        mat.release();
                        mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                        mLastSteadyTime = NO_TIME_SET;

                        break;

                    case MESSAGE_NO_CHANGE_DETECTED:

                        Log.d(CLASS_NAME, "handleMessage: onMovement: false");

//                        There has been no movement but we better do some more checks to be sure:
                        mCVCallback.onMovement(false);
                        if (mLastSteadyTime == NO_TIME_SET) {
                            mLastSteadyTime = System.currentTimeMillis();
                            mat.release();
                        }
//                        There has been no movement for a sufficient amount of time:
                        else if (System.currentTimeMillis() - mLastSteadyTime > MIN_STEADY_TIME) {
                            mLastSteadyTime = NO_TIME_SET;
                            createDuplicateProcessor(mat);
                        }

                        break;

                    case MESSAGE_PAGE_DETECTED:

                        Log.d(CLASS_NAME, "handleMessage: onPageSegmented");

                        Bundle pageBundle = inputMessage.getData();
                        DkPolyRect[] polyRects = (DkPolyRect[]) pageBundle.getParcelableArray(KEY_POLY_RECT);
                        if (mCVCallback != null)
                            mCVCallback.onPageSegmented(polyRects);

//                    Start the focus measurement:
                        createFocusProcessor(mat);

                        break;

                    case MESSAGE_FOCUS_MEASURED:

                        Log.d(CLASS_NAME, "handleMessage: onFocusMeasured");

                        Bundle focusBundle = inputMessage.getData();
                        Patch[] patches = (Patch[]) focusBundle.getParcelableArray(KEY_FOCUS);
                        mCVCallback.onFocusMeasured(patches);

//                        Start the verification task:
                        if (mCVResult.getCVState() == CVResult.DOCUMENT_STATE_OK) {
                            ChangeDetector2.getInstance().initVerifyDetector(mat);
                            mCheckState = CHANGE_TASK_CHECK_VERIFY_FRAME;
                        }
//                        Start the change task:
                        else
                            mCheckState = CHANGE_TASK_CHECK_MOVEMENT;

                        mat.release();

                        break;

                    case MESSAGE_DUPLICATE_FOUND:

                        Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: true");

                        mCVCallback.onWaitingForDoc(true);

                        mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                        mat.release();

                        break;

                    case MESSAGE_NO_DUPLICATE_FOUND:

                        Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: false");

                        mCVCallback.onWaitingForDoc(false);

//                    Start the page detection:
                        createPageProcessor(mat);

                        break;


                    case MESSAGE_FRAME_NOT_VERIFIED:

//                        The last frame received is different than the one on which the image
//                        processing was done.
                        Log.d(CLASS_NAME, "handleMessage: unverified frame");

                        mCVCallback.onMovement(true);
                        mat.release();
//                        Check for movements again:
                        mCheckState = CHANGE_TASK_CHECK_MOVEMENT;

                        break;

                    case MESSAGE_FRAME_VERIFIED:

//                        The last frame received is the same as the one on which the image
//                        processing was done.
                        Log.d(CLASS_NAME, "handleMessage: verified frame");

                        mCVCallback.onCaptureVerified();

                        mLastFrameReceivedTime = NO_TIME_SET;
                        mLastSteadyTime = NO_TIME_SET;
                        mat.release();
                        mCheckState = CHANGE_TASK_CHECK_MOVEMENT;

                        break;

                }

                mIsRunning = false;


            }
        };
    }

    public void receiveFrame(byte[] pixels, int frameWidth, int frameHeight) {

//        Check if a thread is running or if we should wait in order to lower CPU usage:
        if (!mIsRunning) {

//            Avoid checking the change status too often:
            if (mCheckState == CHANGE_TASK_CHECK_MOVEMENT) {
                if (mLastFrameReceivedTime != NO_TIME_SET &&
                        (System.currentTimeMillis() - mLastFrameReceivedTime < FRAME_TIME_DIFF))
                    return;
            }

            mIsRunning = true;
            Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

//            Remember the last time we received a frame:
            mLastFrameReceivedTime = System.currentTimeMillis();
            if (mCheckState == CHANGE_TASK_CHECK_MOVEMENT)
                mExecutor.execute(new ChangeProcessor(this, mat));
            else if (mCheckState == CHANGE_TASK_CHECK_VERIFY_FRAME)
                mExecutor.execute(new VerificationProcessor(this, mat));
        }

    }

    private static Mat byte2Mat(byte[] pixels, int frameWidth, int frameHeight) {

        Mat yuv = new Mat((int) (frameHeight * 1.5), frameWidth, CvType.CV_8UC1);
        yuv.put(0, 0, pixels);

        Mat result = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
        Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2RGB_NV21);

        return result;
    }


    private void createPageProcessor(Mat mat) {

        mExecutor.execute(new PageProcessor(this, mat));

    }

    private void createFocusProcessor(Mat mat) {

        mExecutor.execute(new FocusProcessor(this, mat));

    }

    private void createDuplicateProcessor(Mat mat) {

        mExecutor.execute(new DuplicateProcessor(this, mat));

    }



    @Override
    public void handleObject(int type, Object[] object, Mat mat) {

        Message completeMessage = mHandler.obtainMessage(type, mat);
        Bundle bundle = new Bundle();


        switch (type) {
            case MESSAGE_PAGE_DETECTED:
                bundle.putParcelableArray(KEY_POLY_RECT, (Parcelable[]) object);
                break;
            case MESSAGE_FOCUS_MEASURED:
                bundle.putParcelableArray(KEY_FOCUS, (Parcelable[]) object);
                break;
        }

        completeMessage.setData(bundle);
        completeMessage.sendToTarget();


    }

    @Override
    public void handleState(int type, Mat mat) {

        Message completeMessage = mHandler.obtainMessage(type, mat);
        completeMessage.sendToTarget();

    }
}
