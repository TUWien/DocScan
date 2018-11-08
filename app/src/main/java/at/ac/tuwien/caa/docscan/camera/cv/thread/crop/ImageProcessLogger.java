package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class ImageProcessLogger implements Serializable {


    public static final int TASK_TYPE_PAGE_DETECTION = 0;
    public static final int TASK_TYPE_MAP = 1;
    public static final int TASK_TYPE_ROTATE = 2;
    public static final int TASK_TYPE_PDF = 3;
    private static final String CROP_FILE_NAME = "crop_log.txt";
    private static final String CLASS_NAME = "ImageProcessLogger";

    private static ImageProcessLogger sInstance = null;

    private ArrayList<TaskLog> mTasks;


    static {

        sInstance = new ImageProcessLogger();

    }

    public void readFromDisk(Context context) {

        Log.d(CLASS_NAME, "readFromDisk:");

        File cropLoggerPath = context.getFilesDir();
        File cropFile = new File(cropLoggerPath, CROP_FILE_NAME);
        if (!cropFile.exists()) {
            Log.d(CLASS_NAME, "readFromDisk: is not existing: " + cropFile);
            return;
        }

        try {
            FileInputStream fis = new FileInputStream (cropFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            sInstance = (ImageProcessLogger) ois.readObject();
            ois.close();
        }
        catch(Exception e) {
            Log.d(CLASS_NAME, "readFromDisk: " + e.toString());
        }

    }

    public void saveToDisk(Context context) {

        File cropLoggerPath = context.getFilesDir();
        File syncFile = new File(cropLoggerPath, CROP_FILE_NAME);

        try {
            FileOutputStream fos = new FileOutputStream(syncFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(sInstance);
            oos.close();
        }
        catch(Exception e) {
            Log.d(CLASS_NAME, "saveToDisk: " + e.toString());
        }

    }


    public static ImageProcessLogger getInstance() {

        return sInstance;
    }

    private ImageProcessLogger() {

        mTasks = new ArrayList<>();

    }

    public synchronized static void removeTask(File file, int type) {

        sInstance.removeTask(type, file);

    }

//    public synchronized static void removeRotateTask(File file) {
//
//        sInstance.removeTask(TASK_TYPE_ROTATE, file);
//
//    }
//
//    public synchronized static void removePageDetectionTask(File file) {
//
//        sInstance.removeTask(TASK_TYPE_PAGE_DETECTION, file);
//
//    }
//
//    public static void removeMapTask(File file) {
//
//        sInstance.removeTask(TASK_TYPE_MAP, file);
//
//    }

    private synchronized void removeTask(int type, File file) {

//        TODO: check if we might have duplicates here:

        Iterator<TaskLog> iter = mTasks.iterator();
        while (iter.hasNext()) {
            TaskLog task = iter.next();
            if ((task.getType() == type) && (task.getFile().equals(file))) {
                iter.remove();
                break;
            }
        }

    }

    public static boolean isAwaitingCropping(File file) {

        for (TaskLog task : sInstance.mTasks) {
            if (task.getFile().equals(file))
                return true;
        }

        return false;

    }

    public static boolean isAwaitingImageProcessing(File file) {

        for (TaskLog task : sInstance.mTasks) {
            if (task.getFile().equals(file))
                return true;
        }

        return false;

    }

    public static boolean isAwaitingPageDetection(File file) {

        for (TaskLog task : sInstance.mTasks) {
            if ((task.getType() == TASK_TYPE_PAGE_DETECTION) && (task.getFile().equals(file)))
                return true;
        }

        return false;

    }

//    public static boolean isAwaitingMapping(File file) {
//
//        for (TaskLog task : sInstance.mTasks) {
//            if ((task.getType() == TASK_TYPE_MAP) && (task.getFile().equals(file)))
//                return true;
//        }
//
//        return false;
//
//    }

    public static void addPageDetectionTask(File file) {

        sInstance.addTask(TASK_TYPE_PAGE_DETECTION, file);

    }

    public static void addMapTask(File file) {

        sInstance.addTask(TASK_TYPE_MAP, file);

    }

    public static void addRotateTask(File file) {

        sInstance.addTask(TASK_TYPE_ROTATE, file);

    }

    public static void addPdfTask(File file) {

        sInstance.addTask(TASK_TYPE_PDF, file);

    }


    private void addTask(int type, File file) {

        mTasks.add(new TaskLog(type, file));

    }

    private class TaskLog {

        private int mType;
        private File mFile;

        private TaskLog(int type, File file) {

            mType = type;
            mFile = file;

        }

        private int getType() {

            return mType;

        }

        private File getFile() {

            return mFile;

        }

    }



}
