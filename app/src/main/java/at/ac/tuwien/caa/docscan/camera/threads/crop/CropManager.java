/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Note this is mainly based on the thread pool example here:
 * https://developer.android.com/training/multiple-threads/create-threadpool
 */

package at.ac.tuwien.caa.docscan.camera.threads.crop;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.logic.DataLog;

import static at.ac.tuwien.caa.docscan.ui.syncui.UploadActivity.UPLOAD_FILE_DELETED_ERROR_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadActivity.UPLOAD_INTEND_KEY;

public class CropManager {

    public static final int MESSAGE_COMPLETED_TASK = 0;

    public static final String INTENT_FILE_NAME = "INTENT_FILE_NAME";
    public static final String INTENT_FILE_MAPPED = "INTENT_FILE_MAPPED";

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;

    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;

    /**
     * NOTE: This is the number of total available cores. On current versions of
     * Android, with devices that use plug-and-play cores, this will return less
     * than the total number of cores. The total number of cores is not
     * available in current Android implementations.
     */
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    private static final String CLASS_NAME = "CropManager";

    // A queue of Runnables for the page detection
    private final BlockingQueue<Runnable> mCropQueue;
    // A managed pool of background crop threads
    private final ThreadPoolExecutor mCropThreadPool;
//    We use here a weak reference to avoid memory leaks:
//    @see https://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
    private WeakReference<Context> mContext;

    // An object that manages Messages in a Thread
    private Handler mHandler;

//    Singleton:
    private static CropManager sInstance = null;

    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        sInstance = new CropManager();
    }


    private CropManager() {

        mCropQueue = new LinkedBlockingQueue<>();

        mCropThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
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

//                // Gets the map task from the incoming Message object.
                CropTask task = (CropTask) inputMessage.obj;
                int messageId = inputMessage.what;
                boolean isMessageProcessed = false;

                if (task instanceof PageDetectionTask) {
                    switch (messageId) {
                        case MESSAGE_COMPLETED_TASK:
                            Log.d(CLASS_NAME, "handleMessage: PageDetectionTask completed");

//                            Remove the corresponding TaskLog from the logger:
                            CropLogger.removePageDetectionTask(task.getFile());

//                            notifyImageChanged(task.getFile());
                            isMessageProcessed = true;

                            break;
                    }
                }
                else if (task instanceof MapTask) {
                    switch (messageId) {
                        case MESSAGE_COMPLETED_TASK:
                            Log.d(CLASS_NAME, "handleState: MapTask completed");

//                            Remove the corresponding TaskLog from the logger:
                            CropLogger.removeMapTask(task.getFile());
                            notifyImageChanged(task.getFile());
                            sendFileMappedIntent(task.getFile().getAbsolutePath());
                            isMessageProcessed = true;

                            break;
                    }
                }

                if (!isMessageProcessed)
                    super.handleMessage(inputMessage);

            }

            /**
             * Informs DocScan and other (system) apps that the image has been changed.
             * @param file
             */
            private void notifyImageChanged(File file) {

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);

//                Send the broadcast:
                if ((mContext != null) && (mContext.get() != null))
                    mContext.get().sendBroadcast(mediaScanIntent);

            }
        };

    }

    public void handleState(CropTask task, int state) {

        Message completeMessage = mHandler.obtainMessage(state, task);
        completeMessage.sendToTarget();

    }


    /**
     * Returns the CropManager object
     * @return The global CropManager object
     */
    public static CropManager getInstance() {

        return sInstance;
    }

    public static void initContext(Context context) {

        Context applicationContext = context.getApplicationContext();
        sInstance.mContext = new WeakReference<>(applicationContext);

//        Load the logger:
        CropLogger.getInstance().readFromDisk(sInstance.mContext.get());

    }



    public static PageDetectionTask pageDetection(File file) {

        Log.d(CLASS_NAME, "pageDetection:");

        PageDetectionTask pageDetectionTask = new PageDetectionTask();
        pageDetectionTask.initializeTask(sInstance);
        pageDetectionTask.setFile(file);

//        Inform the logger that we got a new file here:
        CropLogger.addPageDetectionTask(file);

        sInstance.mCropThreadPool.execute(pageDetectionTask.getRunnable());

        return pageDetectionTask;

    }

    public static MapTask mapFile(File file) {

        Log.d(CLASS_NAME, "mapFile:");

        MapTask mapTask = new MapTask();
        mapTask.initializeTask(sInstance);
        mapTask.setFile(file);

//        Inform the logger that we got a new file here:
        CropLogger.addMapTask(file);

        sInstance.mCropThreadPool.execute(mapTask.getRunnable());

        return mapTask;

    }

    private void sendFileMappedIntent(String fileName) {

        Log.d(CLASS_NAME, "sendFileMappedIntent:");

        Intent intent = new Intent(INTENT_FILE_MAPPED);
        intent.putExtra(INTENT_FILE_NAME, fileName);

        LocalBroadcastManager.getInstance(mContext.get()).sendBroadcast(intent);

    }



}
