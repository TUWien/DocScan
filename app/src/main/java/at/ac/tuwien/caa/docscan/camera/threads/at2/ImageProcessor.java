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
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.threads.at.ChangeDetector2;

import static at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect.KEY_POLY_RECT;
import static at.ac.tuwien.caa.docscan.camera.cv.Patch.KEY_FOCUS;

public class ImageProcessor implements ImageRunnable.ImageProcessorCallback,
        ImageChangeRunnable.ImageChangeCallback {

    public static final int TASK_TYPE_PAGE = 0;
    public static final int TASK_TYPE_FOCUS = 1;
    public static final int TASK_TYPE_MOVEMENT = 2;
    public static final int TASK_TYPE_NO_MOVEMENT = 3;
    public static final int TASK_TYPE_NEW_FRAME = 4;
    public static final int TASK_TYPE_SAME_FRAME = 5;
    public static final int TASK_TYPE_VERIFIED_FRAME = 6;
    public static final int TASK_TYPE_UNVERIFIED_FRAME = 7;

    public static final int CHANGE_TASK_CHECK_MOVEMENT = 0;
    public static final int CHANGE_TASK_CHECK_NEW_FRAME = 1;
    public static final int CHANGE_TASK_CHECK_VERIFY_FRAME = 2;

    private static final long MIN_STEADY_TIME = 1500;        // The time in which there must be no movement.
    private static final long FRAME_TIME_DIFF = 300;
    private static final long NO_TIME_SET = -1;

    private static final String CLASS_NAME = "ImageProcessor";

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAXIMUM_POOL_SIZE = 8;



    private final Executor mExecutor;

    // An object that manages Messages in a Thread
    private Handler mHandler;
    private CameraPreview.CVCallback mCVCallback;
    private boolean mIsRunning = false;
    private boolean mCheckForNewFrame = false;
    private boolean mVerifyFrame = false;
    private int mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
    private CVResult mCVResult;
    private long mLastSteadyTime = NO_TIME_SET;
    private long mLastFrameReceivedTime = NO_TIME_SET;

    //    Singleton:
    private static ImageProcessor sInstance;

    static {
        Log.d(CLASS_NAME, "==========================creating new instance\"==========================");
        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        sInstance = new ImageProcessor();
    }

    public static ImageProcessor getInstance() {

        return sInstance;

    }

    public void setCVResult(CVResult cVResult) {

        mCVResult = cVResult;

    }

    public void setCVCallback(CameraPreview.CVCallback callback) {

        mCVCallback = callback;

    }


    private ImageProcessor() {

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

                if (message == TASK_TYPE_PAGE) {

                    Log.d(CLASS_NAME, "handleMessage: onPageSegmented");

                    Bundle bundle = inputMessage.getData();
                    DkPolyRect[] p = (DkPolyRect[]) bundle.getParcelableArray(KEY_POLY_RECT);
                    if (mCVCallback != null)
                        mCVCallback.onPageSegmented(p);

                }
                else if (message == TASK_TYPE_FOCUS) {

                    Log.d(CLASS_NAME, "handleMessage: onFocusMeasured");

                    Bundle bundle = inputMessage.getData();
                    Patch[] p = (Patch[]) bundle.getParcelableArray(KEY_FOCUS);
                    mCVCallback.onFocusMeasured(p);

                    if (mCVResult.getCVState() == CVResult.DOCUMENT_STATE_OK) {
                        ChangeDetector2.getInstance().initVerifyDetector(mat);
                        mCheckState = CHANGE_TASK_CHECK_VERIFY_FRAME;
                    }
                    else
                        mCheckState = CHANGE_TASK_CHECK_MOVEMENT;

                    mat.release();
                    mIsRunning = false;



                }
                else if (message == TASK_TYPE_MOVEMENT) {

                    Log.d(CLASS_NAME, "handleMessage: onMovement: true");

                    mCVCallback.onMovement(true);
                    mat.release();
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;
                    mLastSteadyTime = NO_TIME_SET;

                }
                else if (message == TASK_TYPE_NO_MOVEMENT) {

                    Log.d(CLASS_NAME, "handleMessage: onMovement: false");

                    mCVCallback.onMovement(false);
//                    mCheckForNewFrame = true;

                    if (mLastSteadyTime == NO_TIME_SET) {
                        mLastSteadyTime = System.currentTimeMillis();
                        mat.release();
                    }
                    else if (System.currentTimeMillis() - mLastSteadyTime > MIN_STEADY_TIME) {
                        mLastSteadyTime = NO_TIME_SET;
                        mIsRunning = false;
                        createChangeRunnable(mat, CHANGE_TASK_CHECK_NEW_FRAME);

//                        mCheckState = CHANGE_TASK_CHECK_NEW_FRAME;

                    }

                    mIsRunning = false;

                }
                else if (message == TASK_TYPE_NEW_FRAME) {

                    Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: false");

                    mCVCallback.onWaitingForDoc(false);
                    mCheckForNewFrame = false;
                    mIsRunning = false;
//                    Start the page detection and the focus measurement:
                    createImageRunnable(mat);

                }
                else if (message == TASK_TYPE_SAME_FRAME) {

                    Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: true");

                    mCVCallback.onWaitingForDoc(true);
//                    mCheckForNewFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mat.release();
                    mIsRunning = false;

                }

                else if (message == TASK_TYPE_UNVERIFIED_FRAME) {

                    Log.d(CLASS_NAME, "handleMessage: unverified frame");

                    mCVCallback.onMovement(true);
                    mat.release();
//                    mVerifyFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;

                }

                else if (message == TASK_TYPE_VERIFIED_FRAME) {

                    Log.d(CLASS_NAME, "handleMessage: verified frame");

                    mCVCallback.onCaptureVerified();
                    mLastFrameReceivedTime = NO_TIME_SET;
                    mLastSteadyTime = NO_TIME_SET;
                    mat.release();
//                    mVerifyFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;

                }



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
            mExecutor.execute(new ImageChangeRunnable(this, mat, mCheckState));

        }

    }

    private static Mat byte2Mat(byte[] pixels, int frameWidth, int frameHeight) {

        Mat yuv = new Mat((int) (frameHeight * 1.5), frameWidth, CvType.CV_8UC1);
        yuv.put(0, 0, pixels);

        Mat result = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
        Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2RGB_NV21);

        return result;
    }

    public void createChangeRunnable(Mat mat, int type) {

        if (!mIsRunning) {
            mIsRunning = true;

            mExecutor.execute(new ImageChangeRunnable(this, mat, type));
        }

    }

    public void createChangeRunnable(Mat mat) {

        if (!mIsRunning) {
            mIsRunning = true;

            mExecutor.execute(new ImageChangeRunnable(this, mat, mCheckState));
        }

    }

    public void createImageRunnable(Mat mat) {

        if (!mIsRunning) {
            mIsRunning = true;
            mExecutor.execute(new ImageRunnable(this, mat));
        }

    }


    @Override
    public void handleObject(int type, Object[] object, Mat mat) {

        Message completeMessage = mHandler.obtainMessage(type, mat);
        Bundle bundle = new Bundle();


        switch (type) {
            case TASK_TYPE_PAGE:
                bundle.putParcelableArray(KEY_POLY_RECT, (Parcelable[]) object);
                break;
            case TASK_TYPE_FOCUS:
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
