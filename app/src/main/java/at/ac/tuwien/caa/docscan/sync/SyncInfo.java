package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.syncui.SyncAdapter;

/**
 * Created by fabian on 18.08.2017.
 */

public class SyncInfo implements Serializable {

    private static final long serialVersionUID = -1078733185746102614L;

    private static final String SYNC_FILE_NAME = "syncinfo.txt";
    private static final String CLASS_NAME = "SyncInfo";

    private static SyncInfo mInstance = null;
    private ArrayList<FileSync> mFileSyncList;
    private ArrayList<FileSync> mUploadedList;
    private ArrayList<FileSync> mUnfinishedSyncList;
    private ArrayList<Integer> mUnprocessedUploadIDs;
    private ArrayList<Integer> mUnfinishedUploadIDs;
    private ArrayList<File> mUploadDirs;

    ArrayList<File> mAwaitingUploadFiles;

    public static SyncInfo getInstance() {

        if (mInstance == null) {
            mInstance = new SyncInfo();
        }

        return mInstance;

    }

    public static void readFromDisk(Context context) {

        File syncPath = context.getFilesDir();
        File syncFile = new File(syncPath, SYNC_FILE_NAME);

        Log.d(CLASS_NAME, "readFromDisk");
        DataLog.getInstance().writeUploadLog(context, CLASS_NAME, "readFromDisk");

        try {
            FileInputStream fis = new FileInputStream(syncFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mInstance = (SyncInfo) ois.readObject();

            if (mInstance.getUploadedList() == null) {
                mInstance.setUploadedList(new ArrayList<FileSync>());
            }
            ois.close();
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.d(CLASS_NAME, e.toString());
        }

    }

    public void clearSyncList() {

        mFileSyncList = new ArrayList<>();

    }

    private SyncInfo() {

        Log.d(CLASS_NAME, "constructor");

        mFileSyncList = new ArrayList<>();
        mUnfinishedSyncList = new ArrayList<>();
        mUploadedList = new ArrayList<>();
        mUnfinishedUploadIDs = new ArrayList<>();
        mUnprocessedUploadIDs = new ArrayList<>();

    }

    //    We need this method although it is not used, because the serializable SyncInfo was build with
//    this method.
//    @see: https://stackoverflow.com/questions/8335813/java-serialization-java-io-invalidclassexception-local-class-incompatible
    public void addFile(Context context, File file) {

        if (mFileSyncList != null) {
            mFileSyncList.add(new FileSync(file));
//            startSyncJob(context);
        }

    }

    public void setUploadDirs(ArrayList<File> dirs) {

        mUploadDirs = dirs;
        createAwaitingUploadList();

    }

    public void addUploadDirs(ArrayList<File> dirs) {

        if (mUploadDirs == null)
            mUploadDirs = new ArrayList<>();

//        Just add the directory if it is not already contained:
        for (File dir : dirs)
            if (!mUploadDirs.contains(dir))
                mUploadDirs.add(dir);

        createAwaitingUploadList();

    }

    public ArrayList<Integer> getUnprocessedUploadIDs() {

        return mUnprocessedUploadIDs;

    }


    public ArrayList<Integer> getUnfinishedUploadIDs() {

        return mUnfinishedUploadIDs;

    }

    public void printUnfinishedUploadIDs() {

        for (Integer i : mUnfinishedUploadIDs) {
            Log.d(CLASS_NAME, "printUnfinishedUploadIDs: unfinished ID:" + i);
        }

    }

    private void createAwaitingUploadList() {

        mAwaitingUploadFiles = new ArrayList<>();

        if (mUploadDirs == null)
            return;

        for (File dir : mUploadDirs) {
            File[] files = SyncAdapter.getFiles(dir);
            if ((files == null) || files.length == 0)
                continue;

            for (File file : files) {
                mAwaitingUploadFiles.add(file);
            }
        }

    }

    public ArrayList<File> getUploadDirs() {

        return mUploadDirs;
    }

    public void addToUploadedList(FileSync fileSync) {

        if (mUploadedList != null)
            mUploadedList.add(fileSync);

    }

    public void addTranskribusFile(Context context, File file, int uploadId) {

        if (mFileSyncList != null) {
            mFileSyncList.add(new TranskribusFileSync(file, uploadId));
//            startSyncJob(context);
        }

    }

    public void removeDocument(String title) {

        Iterator<FileSync> iter = mUploadedList.iterator();
        while (iter.hasNext()) {
            File file = iter.next().getFile();
            String subDir = file.getParentFile().getName();
            if (subDir.equals(title)) {
                Log.d(CLASS_NAME, "removeDocument: " + file);
                iter.remove();
            }
        }

    }


    public ArrayList<FileSync> getSyncList() {

        return mFileSyncList;

    }

    //    Note this was private, I am not sure if i can change the modifier, without breaking serialization.
//    private ArrayList<FileSync> getUploadedList() {
    public ArrayList<FileSync> getUploadedList() {

        return mUploadedList;

    }

    private void setUploadedList(ArrayList<FileSync> uploadedList) {

        mUploadedList = uploadedList;

    }

    public class TranskribusFileSync extends FileSync {

        private int mUploadId;

        private TranskribusFileSync(File file, int uploadId) {

            super(file);

            mUploadId = uploadId;
        }

        public int getUploadId() {
            return mUploadId;
        }

    }

    public boolean areFilesUploaded(File[] files) {

        if (files.length == 0)
            return false;

        // Check if every file contained in the folder is already uploaded:
        for (File file : files) {
            if (!isFileUploaded(file))
                return false;
        }

        return true;

    }

    public boolean isDirAwaitingUpload(File dir, File[] files) {

        Log.d(CLASS_NAME, "isDirAwaitingUpload");

        if (files.length == 0)
            return false;

        // Is the dir already added to the upload list:
        if ((mUploadDirs != null) && (mUploadDirs.contains(dir))) {
            Log.d(CLASS_NAME, "isDirAwaitingUpload: mUploadDirs.size: " + mUploadDirs.size());
//            Check if all files in the dir are added to the awaiting upload list:
//            for (File file : files) {
//                if (!mAwaitingUploadFiles.contains(file))
//                    return false;
//            }

            return true;
        } else if ((mFileSyncList != null) && (mFileSyncList.size() > 0)) {
            Log.d(CLASS_NAME, "isDirAwaitingUpload: mFileSyncList.size: " + mFileSyncList.size());
            Log.d(CLASS_NAME, "isDirAwaitingUpload: files.size: " + files.length);
            for (File file : files) {
                boolean isFileOnList = false;
                for (FileSync fileSync : mFileSyncList) {
                    if (fileSync.getFile().equals(file)) {
                        isFileOnList = true;
                        break;
                    }
                }

                if (!isFileOnList)
                    return false;

            }

            return true;

        }

        return false;


    }

    private boolean isFileUploaded(File file) {

        for (SyncInfo.FileSync fileSync : mUploadedList) {
            if (file.getAbsolutePath().compareTo(fileSync.getFile().getAbsolutePath()) == 0)
                return true;
        }

        return false;
    }

//    private boolean isFileUploaded(File file) {
//
//        for (SyncInfo.FileSync fileSync : mFileSyncList) {
//            if ((file.getAbsolutePath().compareTo(fileSync.getFile().getAbsolutePath()) == 0)
//                    && fileSync.getState() == SyncInfo.FileSync.STATE_UPLOADED)
//                return true;
//        }
//
//        return false;
//    }

    public class FileSync implements Serializable {

        public static final int STATE_NOT_UPLOADED = 0;
        public static final int STATE_AWAITING_UPLOAD = 1;
        public static final int STATE_UPLOADED = 2;


        private File mFile;
        private int mState;

        private FileSync(File file) {

            mFile = file;
            mState = STATE_NOT_UPLOADED;

        }

        @Override
        public String toString() {

            String state;
            switch (mState) {
                case STATE_AWAITING_UPLOAD:
                    state = "STATE_AWAITING_UPLOAD";
                    break;
                case STATE_NOT_UPLOADED:
                    state = "STATE_NOT_UPLOADED";
                    break;
                case STATE_UPLOADED:
                    state = "STATE_UPLOADED";
                    break;
                default:
                    state = "undefined";
            }

            return mFile.getAbsolutePath() + ", " + state;
        }


        public int getState() {
            return mState;
        }

        public void setState(int state) {
            mState = state;
        }

        public File getFile() {
            return mFile;
        }
    }


    public interface Callback {
        void onUploadComplete(SyncInfo.FileSync fileSync);

        void onError(Exception e);
    }


}
