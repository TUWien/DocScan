package at.ac.tuwien.caa.docscan.camera.threads.at2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import org.opencv.core.Mat;

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
    private static final long NO_STEADY_TIME = -1;

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
    private long mLastSteadyTime = NO_STEADY_TIME;

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

                    Bundle bundle = inputMessage.getData();
                    DkPolyRect[] p = (DkPolyRect[]) bundle.getParcelableArray(KEY_POLY_RECT);
                    Log.d(CLASS_NAME, "handleMessage: onPageSegmented");
                    if (mCVCallback != null)
                        mCVCallback.onPageSegmented(p);

                }
                else if (message == TASK_TYPE_FOCUS) {

                    Bundle bundle = inputMessage.getData();
                    Patch[] p = (Patch[]) bundle.getParcelableArray(KEY_FOCUS);
                    Log.d(CLASS_NAME, "handleMessage: onFocusMeasured");
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

                    mCVCallback.onMovement(true);
                    Log.d(CLASS_NAME, "handleMessage: onMovement: true");
                    mat.release();
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;

                }
                else if (message == TASK_TYPE_NO_MOVEMENT) {

                    mCVCallback.onMovement(false);
                    Log.d(CLASS_NAME, "handleMessage: onMovement: false");
//                    mCheckForNewFrame = true;
                    mat.release();

                    if (mLastSteadyTime == NO_STEADY_TIME)
                        mLastSteadyTime = System.currentTimeMillis();
                    else if (System.currentTimeMillis() - mLastSteadyTime > MIN_STEADY_TIME)
                        mCheckState = CHANGE_TASK_CHECK_NEW_FRAME;

                    mIsRunning = false;

                }
                else if (message == TASK_TYPE_NEW_FRAME) {

                    mCVCallback.onWaitingForDoc(false);
                    Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: false");
                    mCheckForNewFrame = false;
                    mIsRunning = false;
//                    Start the page detection and the focus measurement:
                    createImageRunnable(mat);

                }
                else if (message == TASK_TYPE_SAME_FRAME) {

                    mCVCallback.onWaitingForDoc(true);
                    Log.d(CLASS_NAME, "handleMessage: onWaitingForDoc: true");
//                    mCheckForNewFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mat.release();
                    mIsRunning = false;

                }

                else if (message == TASK_TYPE_UNVERIFIED_FRAME) {

                    mCVCallback.onMovement(true);
                    mat.release();
//                    mVerifyFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;

                }

                else if (message == TASK_TYPE_VERIFIED_FRAME) {

                    mCVCallback.onCaptureVerified();
                    mLastSteadyTime = NO_STEADY_TIME;
                    mat.release();
//                    mVerifyFrame = false;
                    mCheckState = CHANGE_TASK_CHECK_MOVEMENT;
                    mIsRunning = false;

                }



            }
        };
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
