package at.ac.tuwien.caa.docscan.camera.threads.at;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import static at.ac.tuwien.caa.docscan.camera.threads.at.ChangeRunnable.MESSAGE_FRAME_MOVED;


public class CVManager {

    public static final int TYPE_NONE = -1;
    public static final int TYPE_MOVE = 0;
    private static final int TYPE_NEW = 1;
    private static final int TYPE_PAGE = 2;
    private static final int TYPE_FOCUS = 3;

    private static final long FRAME_TIME_DIFF = 200;
    private static final long FRAME_TIME_STEADY = 500;

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;

    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;



    // A queue of Runnables for the page detection
    private final BlockingQueue<Runnable> mCropQueue;
    // A managed pool of background crop threads
    private final ThreadPoolExecutor mCVThreadPool;

    // An object that manages Messages in a Thread
    private Handler mHandler;
    private CameraPreview.CVCallback mCVCallback;
    private int cnt = 0;

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
                int message = inputMessage.what;

                if (task instanceof PageTask) {

                    cnt++;
//                    TODO: message handling...
                    mCVCallback.onPageSegmented(task.getPolyRect(), cnt);

                    sInstance.mActiveTaskType = TYPE_FOCUS;
                    FocusTask focusTask = new FocusTask();
                    focusTask.setMat(task.getMat());
                    sInstance.mCVThreadPool.execute(focusTask.getRunnable());

//                    task.recycle();

                }

                else if (task instanceof ChangeTask) {

                    boolean isMoved = message == MESSAGE_FRAME_MOVED ? true : false;

                    if (isMoved) {
                        DkPolyRect[] polyRects = new DkPolyRect[]{};
                        mCVCallback.onFocusMeasured(null);
                        mCVCallback.onPageSegmented(null,0);
                        mIsActive = false;
                        mCVCallback.onMovement(isMoved);
                        return;
                    }

                    Log.d(CLASS_NAME, "handleMessage: message: " + message);
                    mCVCallback.onMovement(isMoved);

                    if (sInstance.mLastTime - sInstance.mSteadyTime > FRAME_TIME_STEADY) {
                        sInstance.mActiveTaskType = TYPE_PAGE;
                        PageTask pageTask = new PageTask();
                        pageTask.setMat(task.getMat());
                        sInstance.mCVThreadPool.execute(pageTask.getRunnable());
                    }
                    else {
                        task.recycle();
                        mIsActive = false;
                    }

                }

                else if (task instanceof FocusTask) {

                    mCVCallback.onFocusMeasured(task.getPatch());
                    mIsActive = false;
                    task.recycle();

                }




            }
        };

    }

    public void handleState(CVTask task, int state) {

        Message completeMessage = mHandler.obtainMessage(state, task);
        completeMessage.sendToTarget();

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

    public static void performTask(int taskType, byte[] pixels, int frameWidth, int frameHeight) {

        sInstance.mLastTime = System.currentTimeMillis();
        sInstance.mIsActive = true;

        Mat mat = byte2Mat(pixels, frameWidth, frameHeight);

        CVTask task = null;

        switch (taskType) {

            case TYPE_MOVE:
                task = new ChangeTask();
                break;
//TODO: fill out the other cases

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
