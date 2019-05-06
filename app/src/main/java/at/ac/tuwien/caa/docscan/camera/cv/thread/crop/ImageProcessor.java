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

package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_FOCUS_MEASURE;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_MAP;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_PAGE_DETECTION;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_PDF;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_PDF_OCR;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger.TASK_TYPE_ROTATE;

public class ImageProcessor {

    public static final int MESSAGE_COMPLETED_TASK = 0;
    public static final int MESSAGE_CREATED_DOCUMENT = 1;

    public static final String INTENT_FILE_NAME = "INTENT_FILE_NAME";
//    public static final String INTENT_FILE_MAPPED = "INTENT_FILE_MAPPED";
    public static final String INTENT_IMAGE_PROCESS_ACTION = "INTENT_IMAGE_PROCESS_ACTION";
    public static final String INTENT_IMAGE_PROCESS_TYPE = "INTENT_IMAGE_PROCESS_TYPE";
    public static final int INTENT_IMAGE_PROCESS_FINISHED = 0;
    public static final int INTENT_PDF_PROCESS_FINISHED = 1;

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;

    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;


    private static final String CLASS_NAME = "ImageProcessor";

    // A queue of Runnables for the page detection
    private final BlockingQueue<Runnable> mProcessQueue;
    // A managed pool of background threads
    private final ThreadPoolExecutor mProcessThreadPool;

//    This is a single thread executor, in order to avoid OOM's when opening too many images in
//    parallel, instead the pdf's are created in serial:
    private final Executor mPDFExecutor;
//    We use here a weak reference to avoid memory leaks:
//    @see https://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
    private WeakReference<Context> mContext;

    // An object that manages Messages in a Thread
    private Handler mHandler;

//    Singleton:
    private static ImageProcessor sInstance;

    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

        sInstance = new ImageProcessor();
    }


    private ImageProcessor() {

        mProcessQueue = new LinkedBlockingQueue<>();
        mPDFExecutor = Executors.newSingleThreadExecutor();

        mProcessThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mProcessQueue);

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
                ImageProcessTask task = (ImageProcessTask) inputMessage.obj;
                int messageId = inputMessage.what;
                boolean isMessageProcessed = false;

                if (messageId == MESSAGE_COMPLETED_TASK)
                    isMessageProcessed = finishTask(task);

                if (messageId == MESSAGE_CREATED_DOCUMENT)
                    isMessageProcessed = notifyCreatedDocument(task);

                if (!isMessageProcessed)
                    super.handleMessage(inputMessage);

            }

            private boolean notifyCreatedDocument(ImageProcessTask task){

                if (!(task instanceof PdfProcessTask))
                    return false;

                PdfProcessTask pdfTask = (PdfProcessTask) task;
                if (pdfTask.getPdfName() != null) {
                    File pdfFile = new File(Helper.getPDFStorageDir("DocScan").getAbsolutePath(),
                        pdfTask.getPdfName() + ".pdf");
                    String absolutePath = pdfFile.getAbsolutePath();
                    sendIntent(absolutePath, INTENT_PDF_PROCESS_FINISHED);
                    return true;
                } else {
                    return false;
                }
            }

            /**
             * Returns true if the task is processed.
             * @param task a task
             * @return true if the task is handled.
             */
            private boolean finishTask(ImageProcessTask task) {

                if (task == null)
                    return false;

                if (task instanceof PageDetectionTask) {
                    ImageProcessLogger.removeTask(task.getFile(),
                            ImageProcessLogger.TASK_TYPE_PAGE_DETECTION);

//                    Check if the page is unfocused:
                    if (mContext != null && mContext.get() != null) {
                        boolean isFocused = PageDetector.getNormedCropPoints(task.getFile().
                                getAbsolutePath()).isFocused();
                        DocumentStorage.getInstance(mContext.get()).setPageFocused(
                                task.getFile().getName(), isFocused);
                    }

////                    Check if the page is unfocused:
//                    if (mContext != null && mContext.get() != null &&
//                            !PageDetector.getNormedCropPoints(task.getFile().getAbsolutePath()).isFocused())
//                        DocumentStorage.getInstance(mContext.get()).setPageAsUnsharp(
//                                task.getFile().getName());

                }
                else if (task instanceof MapTask) {
                    ImageProcessLogger.removeTask(task.getFile(), ImageProcessLogger.TASK_TYPE_MAP);
                    // Notify other apps and DocScan about the image change:
                    notifyImageChanged(task.getFile());
                }
                else if (task instanceof RotateTask) {
                    ImageProcessLogger.removeTask(task.getFile(), ImageProcessLogger.TASK_TYPE_ROTATE);
                    // Notify other apps and DocScan about the image change:
                    notifyImageChanged(task.getFile());
                }
                else if (task instanceof PdfProcessTask){
                    ImageProcessLogger.removeTask(task.getDocument(), ImageProcessLogger.TASK_TYPE_PDF);
                }
                else
                    return false;

                if (task.getFile() != null) {
                    sendIntent(task.getFile().getAbsolutePath(), INTENT_IMAGE_PROCESS_FINISHED);
                }

                return true;

            }


            /**
             * Informs DocScan and other (system) apps that the image has been changed.
             * @param file The file.
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


    public void handleState(ImageProcessTask task, int state) {

        Message completeMessage = mHandler.obtainMessage(state, task);
        completeMessage.sendToTarget();

    }


    /**
     * Returns the ImageProcessor object
     * @return The global ImageProcessor object
     */
    public static ImageProcessor getInstance() {

        return sInstance;

    }

    public static void initContext(Context context) {

        Context applicationContext = context.getApplicationContext();
        sInstance.mContext = new WeakReference<>(applicationContext);

//        Load the logger:
        ImageProcessLogger.getInstance().readFromDisk(sInstance.mContext.get());

    }

    public static void pageDetection(File file) {

        executeTask(file, TASK_TYPE_PAGE_DETECTION);

    }

    public static void focusMeasure(File file) {

        executeTask(file, TASK_TYPE_FOCUS_MEASURE);

    }

    public static void rotateFile(File file) {

        executeTask(file, TASK_TYPE_ROTATE);

    }

    public static void createPdfWithOCR(Document document) {

        executeTask(document, TASK_TYPE_PDF_OCR);

    }

    public static void createPdf(Document document) {

        executeTask(document, TASK_TYPE_PDF);

    }

    public static void mapFile(File file) {

        executeTask(file, TASK_TYPE_MAP);

    }

    private static void executeTask(File file, int taskType) {

        ImageProcessTask imageProcessTask = null;

        //        Inform the logger that we got a new file here:
        switch (taskType) {
            case TASK_TYPE_PAGE_DETECTION:
                imageProcessTask = new PageDetectionTask();
                ImageProcessLogger.addPageDetectionTask(file);
                break;
            case TASK_TYPE_MAP:
                imageProcessTask = new MapTask();
                ImageProcessLogger.addMapTask(file);
                break;
            case TASK_TYPE_ROTATE:
                imageProcessTask = new RotateTask();
                ImageProcessLogger.addRotateTask(file);
                break;
            case TASK_TYPE_FOCUS_MEASURE:
                imageProcessTask = new RotateTask();
                ImageProcessLogger.addRotateTask(file);
        }

        if (imageProcessTask != null) {
            imageProcessTask.initializeTask(sInstance);
            imageProcessTask.setFile(file);

            sInstance.mProcessThreadPool.execute(imageProcessTask.getRunnable());
        }

    }

    private static void executeTask(Document document, int taskType) {

        ImageProcessTask imageProcessTask;

        boolean performOCR = false;
        //        Inform the logger that we got a new file here:
        switch (taskType) {
            case TASK_TYPE_PDF:
                performOCR = false;
                ImageProcessLogger.addPdfTask(document);
                break;
            case TASK_TYPE_PDF_OCR:
                ImageProcessLogger.addPdfWithOCRTask(document);
                performOCR = true;
                break;
        }

        imageProcessTask = new PdfProcessTask(performOCR, document.getFiles(), document.getTitle());
//        We need to pass here the context, because it allows us to generate a FirebaseVisionImage:
        ((PdfProcessTask)imageProcessTask).setContext(sInstance.mContext);
        imageProcessTask.initializeTask(sInstance);

        sInstance.mPDFExecutor.execute(imageProcessTask.getRunnable());

    }

    private void sendIntent(String fileName, int type) {

        Log.d(CLASS_NAME, "sendIntent:");

        Intent intent = new Intent(INTENT_IMAGE_PROCESS_ACTION);
        intent.putExtra(INTENT_IMAGE_PROCESS_TYPE, type);
        intent.putExtra(INTENT_FILE_NAME, fileName);

        if (mContext != null)
            LocalBroadcastManager.getInstance(mContext.get()).sendBroadcast(intent);

    }

}
