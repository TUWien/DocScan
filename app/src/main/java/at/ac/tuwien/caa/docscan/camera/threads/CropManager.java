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

package at.ac.tuwien.caa.docscan.camera.threads;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CropManager {

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


    // A queue of Runnables for the page detection
    private final BlockingQueue<Runnable> mCropQueue;
    // A managed pool of background crop threads
    private final ThreadPoolExecutor mCropThreadPool;

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

       mCropQueue = new LinkedBlockingQueue<Runnable>();

        mCropThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mCropQueue);

        /*
         * Instantiates a new anonymous Handler object and defines its
         * handleMessage() method. I am not sure if it is a good design that the handler is run on
         * the UI thread...
         * TODO: see if we really need UI updates here
         */
        mHandler = new Handler(Looper.getMainLooper()) {

        };


    }

    /**
     * Returns the PhotoManager object
     * @return The global PhotoManager object
     */
    public static CropManager getInstance() {

        return sInstance;
    }


    static public CropTask saveCropResult(File file) {

        CropTask cropTask = new CropTask();
        cropTask.initializeCropTask(sInstance);
        cropTask.setFile(file);
        sInstance.mCropThreadPool.execute(cropTask.getCropRunnable());

        return cropTask;

    }
}
