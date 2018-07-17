package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

import static at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect.KEY_POLY_RECT;
import static at.ac.tuwien.caa.docscan.camera.cv.Patch.KEY_FOCUS;
import static at.ac.tuwien.caa.docscan.camera.threads.CVThreadManager.TASK_FOCUS;

public class CVManager {

    public static final int TASK_TYPE_NONE = -1;
    public static final int TASK_TYPE_MOVE = 0;
    public static final int TASK_TYPE_NEW = 1;
    public static final int TASK_TYPE_PAGE = 2;
    public static final int TASK_TYPE_FOCUS = 3;

    private static final long FRAME_TIME_DIFF = 200;
    private static final long FRAME_TIME_STEADY = 500;

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAXIMUM_POOL_SIZE = 8;

    protected static final int MESSAGE_FRAME_STEADY = 0;
    protected static final int MESSAGE_FRAME_MOVED = 1;
    protected static final int MESSAGE_TASK_COMPLETED = 2;

    // A queue of Runnables for the page detection
    private final BlockingQueue<Runnable> mCropQueue;
    // A managed pool of background crop threads
    private final ThreadPoolExecutor mCVThreadPool;
    private final Executor mExecutor;
    private final Queue<CVTask> mTaskWorkQueue;

    // An object that manages Messages in a Thread
    private Handler mHandler;
    private CameraPreview.CVCallback mCVCallback;

    //    Singleton:
    private static CVManager sInstance = null;

    private static final String CLASS_NAME = "CVManager";

    private boolean mIsActive = false;
    private long mLastTime = -1;
    private Mat mCompareMat;
    private int mNextTask = TASK_TYPE_NONE;
    private long mSteadyTime;
    private int mActiveTaskType = -1;

    static {

        Log.d(CLASS_NAME, "==========================creating new instance\"==========================");
        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

//        sInstance = new CVManager();
    }

    /**
     * Returns the CVManager object
     * @return The global CVManager object
     */
    public static CVManager getInstance() {

        return sInstance;

    }

    public boolean receivesFrames() {

//        Log.d(CLASS_NAME, "active threads: " + mCVThreadPool.getActiveCount());

//        return mNextTask == TASK_TYPE_MOVE;

        if (!mIsActive)
            return true;
        else {
            return mNextTask == TASK_TYPE_MOVE;
//            return true;
//            return true;
//            long currentTime = System.currentTimeMillis();
//            return currentTime - mLastTime >= FRAME_TIME_DIFF;
        }

    }

    public void setCVCallback(CameraPreview.CVCallback callback) {

        mCVCallback = callback;

    }

    private CVManager() {

        mCropQueue = new LinkedBlockingQueue<>();

        mCVThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mCropQueue);
        mTaskWorkQueue = new LinkedBlockingQueue<CVTask>();

        mExecutor = Executors.newSingleThreadExecutor();

        /*
         * Instantiates a new anonymous Handler object and defines its
         * handleMessage() method.
         */
        mHandler = new Handler(Looper.getMainLooper()) {

            /*
             * handleMessage() defines the operations to perform when the
             * Handler receives a new Message to process.
             */
            @Override
            public void handleMessage(Message inputMessage) {


////                Do nothing, the CameraActivity should tell us what to do:
//                mNextTask = TASK_TYPE_NONE;

                CVTask task = (CVTask) inputMessage.obj;


                if (task instanceof ChangeTask) {

                    int message = inputMessage.what;
                    boolean isMoved = message == MESSAGE_FRAME_MOVED ? true : false;
                    Log.d(CLASS_NAME, "handleMessage: onMovement");
                    mCVCallback.onMovement(isMoved);

                }

                else if (task instanceof PageTask) {

                    Bundle bundle = inputMessage.getData();
                    DkPolyRect[] p = (DkPolyRect[]) bundle.getParcelableArray(KEY_POLY_RECT);
                    Log.d(CLASS_NAME, "handleMessage: onPageSegmented");
                    mCVCallback.onPageSegmented(p);

                }

                else if (task instanceof FocusTask) {

                    Bundle bundle = inputMessage.getData();
                    Patch[] p = (Patch[]) bundle.getParcelableArray(KEY_FOCUS);
                    Log.d(CLASS_NAME, "handleMessage: onFocusMeasured");
                    mCVCallback.onFocusMeasured(p);

                }

                task.recycle();
                mIsActive = false;
//                mNextTask = TASK_TYPE_NONE;

            }
        };

    }

    public void setNextTask(int nextTask) {

        mNextTask = nextTask;

    }

//    public void setNextTask(int nextTask, Mat mat) {
//
//        mNextTask = nextTask;
//        mCompareMat = mat;
//
//    }

    public void handleState(CVTask task, int state) {

        Message completeMessage = mHandler.obtainMessage(state, task);
        completeMessage.sendToTarget();

    }

    public void handleState(CVTask cvTask, Object[] object) {

        Message completeMessage = mHandler.obtainMessage(MESSAGE_TASK_COMPLETED, cvTask);
        Bundle bundle = new Bundle();

//        Determine the passed object based on the task:
        if (cvTask instanceof PageTask)
            bundle.putParcelableArray(KEY_POLY_RECT, (Parcelable[]) object);
        else if (cvTask instanceof  FocusTask)
            bundle.putParcelableArray(KEY_FOCUS, (Parcelable[]) object);

        completeMessage.setData(bundle);
        completeMessage.sendToTarget();

    }

    public void performTask(int taskType) {

        Log.d(CLASS_NAME, "performTask:" + taskType);

        mNextTask = taskType;
        mLastTime = System.currentTimeMillis();
        mIsActive = true;

        CVTask task = getTask(taskType);

        if (task == null)
            return;

        task.initializeTask(sInstance);

//        if (mCompareMat != null) {
//            synchronized (mCompareMat) {
//                task.setMat(mCompareMat.clone());
//            }
//        }
//        else
            task.setMat(mCompareMat.clone());

        Log.d(CLASS_NAME, "performTask: executing task " + taskType);
//        mCVThreadPool.execute(task.getRunnable());

        mExecutor.execute(task.getRunnable());

    }

    public void performTask(int taskType, Mat mat) {

        mNextTask = taskType;
        mLastTime = System.currentTimeMillis();
        mIsActive = true;

        CVTask task = getTask(taskType);

        if (task == null)
            return;

        task.initializeTask(sInstance);
        task.setMat(mat);

        mCVThreadPool.execute(task.getRunnable());

    }

    public void performNextTask(byte[] pixels, int frameWidth, int frameHeight) {

        mLastTime = System.currentTimeMillis();
        mIsActive = true;

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

//        if (mCompareMat != null) {
//            synchronized (mCompareMat) {
//
//                if (mCompareMat != null) {
//                    mCompareMat.release();
//                    mCompareMat = null;
//                }
//                mCompareMat = mat.clone();
//            }
//
//        }
//        else

        if (mCompareMat != null) {
            mCompareMat.release();
            mCompareMat = null;
            Log.d(CLASS_NAME, "compare mat released");
        }

        mCompareMat = mat.clone();
        Log.d(CLASS_NAME, "compare mat cloned");



        Log.d(CLASS_NAME, "mCompareMat set");

        CVTask task = getTask(mNextTask);

        if (task == null)
            return;

        task.initializeTask(sInstance);
        task.setMat(mat);

//        mCVThreadPool.execute(task.getRunnable());
        mExecutor.execute(task.getRunnable());

    }

    @Nullable
    private CVTask getTask(int taskType) {

        CVTask task;
        switch (taskType) {

            case TASK_TYPE_MOVE:
                task = new ChangeTask();
                break;
            case TASK_TYPE_PAGE:
                task = new PageTask();
                break;
            case TASK_TYPE_FOCUS:
                task = new FocusTask();
                break;
            default:
                task = null;
                break;
        }
        return task;

    }

    public static void performTask(int taskType, byte[] pixels, int frameWidth, int frameHeight) {

        sInstance.mLastTime = System.currentTimeMillis();
        sInstance.mIsActive = true;


        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        CVTask task;

        switch (taskType) {
            case TASK_TYPE_MOVE:
                task = new ChangeTask();
                break;
            case TASK_TYPE_PAGE:
                task = new PageTask();
                break;
            case TASK_TYPE_FOCUS:
                task = new FocusTask();
                break;
            default:
                task = null;
                break;
        }

        if (task == null)
            return;

        task.initializeTask(sInstance);
        task.setMat(mat);

        sInstance.mCVThreadPool.execute(task.getRunnable());


    }

    private static Mat byte2Mat(byte[] pixels, int frameWidth, int frameHeight) {

        Mat yuv = new Mat((int) (frameHeight * 1.5), frameWidth, CvType.CV_8UC1);
        yuv.put(0, 0, pixels);

        Mat result = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
        Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2RGB_NV21);

        return result;
    }



}
