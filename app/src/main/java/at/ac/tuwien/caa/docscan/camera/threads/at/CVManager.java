package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;

import static at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect.KEY_POLY_RECT;
import static at.ac.tuwien.caa.docscan.camera.cv.Patch.KEY_FOCUS;

public class CVManager {

    public static final int TYPE_NONE = -1;
    public static final int TYPE_MOVE = 0;
    private static final int TYPE_NEW = 1;
    private static final int TYPE_PAGE = 2;
    private static final int TYPE_FOCUS = 3;

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

    // An object that manages Messages in a Thread
    private Handler mHandler;
    private CameraPreview.CVCallback mCVCallback;

    //    Singleton:
    private static CVManager sInstance = null;

    private static final String CLASS_NAME = "CVManager";

    private boolean mIsActive = false;
    private long mLastTime = -1;
    private long mSteadyTime;
    private int mActiveTaskType = -1;

    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        sInstance = new CVManager();
    }

    /**
     * Returns the CropManager object
     * @return The global CropManager object
     */
    public static CVManager getInstance() {

        return sInstance;

    }

    public static boolean receivesFrames() {

        if (sInstance.mIsActive)
            return false;
        else {
            long currentTime = System.currentTimeMillis();

            return currentTime - sInstance.mLastTime >= FRAME_TIME_DIFF;
        }

    }

    public void setCVCallback(CameraPreview.CVCallback callback) {

        mCVCallback = callback;

    }

    private CVManager() {

        mCropQueue = new LinkedBlockingQueue<>();

        mCVThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mCropQueue);

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

                CVTask task = (CVTask) inputMessage.obj;

                if (task instanceof PageTask) {

                    Bundle bundle = inputMessage.getData();
                    DkPolyRect[] p = (DkPolyRect[]) bundle.getParcelableArray(KEY_POLY_RECT);
                    mCVCallback.onPageSegmented(p);
                }

                else if (task instanceof ChangeTask) {

                    int message = inputMessage.what;

                    boolean isMoved = message == MESSAGE_FRAME_MOVED ? true : false;
                    mCVCallback.onMovement(isMoved);

                }

                else if (task instanceof FocusTask) {

//                    mCVCallback.onFocusMeasured(task.getPatch());
                    Bundle bundle = inputMessage.getData();
                    Patch[] p = (Patch[]) bundle.getParcelableArray(KEY_FOCUS);
                    mCVCallback.onFocusMeasured(p);

                }

                mIsActive = false;
                task.recycle();




            }
        };

    }


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

    public static void performTask(int taskType, byte[] pixels, int frameWidth, int frameHeight) {

        sInstance.mLastTime = System.currentTimeMillis();
        sInstance.mIsActive = true;

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        CVTask task;

        switch (taskType) {
            case TYPE_MOVE:
                task = new ChangeTask();
                break;
            case TYPE_PAGE:
                task = new PageTask();
                break;
            case TYPE_FOCUS:
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

    public static void performTask(byte[] pixels, int frameWidth, int frameHeight) {

        sInstance.mLastTime = System.currentTimeMillis();

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        CVTask task = null;

        switch (sInstance.mActiveTaskType) {

            case TYPE_NONE:
                sInstance.mActiveTaskType = TYPE_MOVE;
                sInstance.mSteadyTime = -1;
                sInstance.mIsActive = true;
                task = new ChangeTask();
                break;

//            case TYPE_MOVE:
//                sInstance.mIsActive = true;
//                if (sInstance.mLastTime - sInstance.mSteadyTime > FRAME_TIME_STEADY) {
//                    sInstance.mActiveTaskType = TYPE_PAGE;
//                    task = new PageTask();
//                }
//                else {
//                    task = new ChangeTask();
//                }
//                break;
//
//            case TYPE_PAGE:
//                sInstance.mIsActive = true;
//                task = new PageTask();
//                break;
        }

        if (task == null)
            return;

        task.initializeTask(sInstance);
        task.setMat(mat);

        sInstance.mCVThreadPool.execute(task.getRunnable());


    }



    public static void performChangeTask(byte[] pixels, int frameWidth, int frameHeight) {

        if (sInstance.mIsActive)
            return;

        sInstance.mIsActive = true;

        Log.d(CLASS_NAME, "performChangeTask: start");

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        ChangeTask changeTask = new ChangeTask();
        changeTask.initializeTask(sInstance);
        changeTask.setMat(mat);

        sInstance.mCVThreadPool.execute(changeTask.getRunnable());

        Log.d(CLASS_NAME, "performChangeTask: end");


    }

    public static void performPageTask(byte[] pixels, int frameWidth, int frameHeight) {

//        if (!sInstance.mCVThreadPool.getQueue().isEmpty())
//            return;
        if (sInstance.mIsActive)
            return;

        sInstance.mIsActive = true;

        Log.d(CLASS_NAME, "performPageTask: start");

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        PageTask pageTask = new PageTask();
        pageTask.initializeTask(sInstance);
        pageTask.setMat(mat);

        sInstance.mCVThreadPool.execute(pageTask.getRunnable());

        Log.d(CLASS_NAME, "performPageTask: end");

    }


    private static Mat byte2Mat(byte[] pixels, int frameWidth, int frameHeight) {

        Mat yuv = new Mat((int) (frameHeight * 1.5), frameWidth, CvType.CV_8UC1);
        yuv.put(0, 0, pixels);

        Mat result = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
        Imgproc.cvtColor(yuv, result, Imgproc.COLOR_YUV2RGB_NV21);

        return result;
    }



}
