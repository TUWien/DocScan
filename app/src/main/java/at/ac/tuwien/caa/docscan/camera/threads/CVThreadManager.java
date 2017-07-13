package at.ac.tuwien.caa.docscan.camera.threads;

import android.os.Process;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;

/**
 * Created by Frank Tan on 11/04/2016.
 * A Singleton Manager for managing the thread pool
 */
public class CVThreadManager {

    private static CVThreadManager sInstance = null;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private final ExecutorService mExecutorService;
    private final BlockingQueue<Runnable> mTaskQueue;
    private List<Future> mPageList, mFocusList, mChangeList;

    public static final int TASK_PAGE   = 0;
    public static final int TASK_FOCUS  = 1;
    public static final int TASK_CHANGE = 2;

//    private WeakReference<UiThreadCallback> uiThreadCallbackWeakReference;

    // The class is used as a singleton
    static {
        sInstance = new CVThreadManager();
    }

    // Made constructor private to avoid the class being initiated from outside
    private CVThreadManager() {

        // initialize a queue for the thread pool. New tasks will be added to this queue
        mTaskQueue = new LinkedBlockingQueue<Runnable>();

        mPageList = new ArrayList<>();
        mFocusList = new ArrayList<>();
        mChangeList = new ArrayList<>();

        mExecutorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE, new BackgroundThreadFactory());
    }

    public static CVThreadManager getsInstance() {
        return sInstance;
    }

    // Add a callable to the queue, which will be executed by the next available thread in the pool
    public void addCallable(Callable callable, int type){

        Future future = mExecutorService.submit(callable);

        // Determine which task list should be used:
        List<Future> mList = null;
        switch (type) {
            case TASK_PAGE:
                mList = mPageList;
                break;
            case TASK_FOCUS:
                mList = mFocusList;
                break;
            case TASK_CHANGE:
                mList = mChangeList;
                break;
        }
        mList.add(future);
    }

    public boolean isFrameSteadyAndNew() {

        for (Future task : mChangeList) {
            if (!task.isDone()) {
                try {
                    return (Boolean) task.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public boolean isRunning(int type) {

        // Determine which task list should be used:
        List<Future> mList = null;
        switch (type) {
            case TASK_PAGE:
                mList = mPageList;
                break;
            case TASK_FOCUS:
                mList = mFocusList;
                break;
            case TASK_CHANGE:
                mList = mChangeList;
                break;
        }

        for (Future task : mList) {
            if (!task.isDone()) {
                return true;
            }
        }

        return false;

    }

    /* Remove all tasks in the queue and stop all running threads
     * Notify UI thread about the cancellation
     */
    public void cancelAllTasks() {
//        synchronized (this) {
//            mTaskQueue.clear();
//            for (Future task : mRunningTaskList) {
//                if (!task.isDone()) {
//                    task.cancel(true);
//                }
//            }
//            mRunningTaskList.clear();
//        }
//        sendMessageToUiThread(Util.createMessage(Util.MESSAGE_ID, "All tasks in the thread pool are cancelled"));
    }

    private class PageHandler implements Runnable {

        protected Mat mMat;
        protected CameraPreview.CVCallback mCVCallback;
        protected TaskTimer.TimerCallbacks mTimerCallbacks;
        protected  byte[] mPixels;
        private int mFrameWidth, mFrameHeight;


        public PageHandler(byte[] pixels, int frameWidth, int frameHeight, CameraPreview.CVCallback cvCallback, TaskTimer.TimerCallbacks timerCallbacks) {

            mPixels = pixels;
            mFrameWidth = frameWidth;
            mFrameHeight = frameHeight;
            mCVCallback = cvCallback;
            mTimerCallbacks = timerCallbacks;

        }

        @Override
        public void run() {


            mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);

            Mat yuv = new Mat((int) (mFrameHeight * 1.5), mFrameWidth, CvType.CV_8UC1);
            yuv.put(0, 0, mPixels);
            mMat = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC3);
            Imgproc.cvtColor(yuv, mMat, Imgproc.COLOR_YUV2RGB_NV21);
            yuv.release();
            mPixels = null;

            DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mMat);
            mCVCallback.onPageSegmented(polyRects);
            mMat.release();
//            }

            mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);


        }
    }

    /* A ThreadFactory implementation which create new threads for the thread pool.
       The threads created is set to background priority, so it does not compete with the UI thread.
     */
    private static class BackgroundThreadFactory implements ThreadFactory {
        private static int sTag = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("CustomThread" + sTag);
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Log.d(this.getClass().getName(), "created new BackgroundThreadFactory");

            // A exception handler is created to log the exception from threads
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.d(this.getClass().getName(), thread.getName() + " encountered an error: " + ex.getMessage());
                }
            });
            return thread;
        }
    }
}