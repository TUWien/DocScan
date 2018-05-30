package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.syncui.SyncAdapter;

/**
 * Created by fabian on 18.08.2017.
 */

public class SyncInfo implements Serializable {


    private static final String SYNC_FILE_NAME = "syncinfo.txt";
    private static final String CLASS_NAME = "SyncInfo";

    private static SyncInfo mInstance = null;
    private ArrayList<FileSync> mFileSyncList;
    private ArrayList<FileSync> mUploadedList;
    private ArrayList<FileSync> mUnfinishedSyncList;
    private ArrayList<Integer> mUnprocessedUploadIDs;
    private ArrayList<Integer> mUnfinishedUploadIDs;
    private ArrayList<File> mUploadDirs;

    // TODO: this is a dirty workaround, think about something better useable:
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

        Log.d(CLASS_NAME, "readFromDisk1");

        try {
            FileInputStream fis = new FileInputStream (syncFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mInstance = (SyncInfo) ois.readObject();

            Log.d(CLASS_NAME, "readFromDisk2");

            if (mInstance.getUploadedList() == null) {
                Log.d(CLASS_NAME, "readFromDisk3");
                mInstance.setUploadedList(new ArrayList<FileSync>());
            }
            else
                Log.d(CLASS_NAME, "readFromDisk4");


            Log.d("SyncInfo", "SyncInfo list: " + mInstance.getSyncList().size());
            ois.close();
        }
        catch(Exception e) {

        }

    }

    public static boolean isInstanceNull() {

        return (mInstance == null);

    }

    public static void saveToDisk(Context context) {

        File syncPath = context.getFilesDir();
        File syncFile = new File(syncPath, SYNC_FILE_NAME);

        try {
            FileOutputStream fos = new FileOutputStream(syncFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mInstance);
            oos.close();
        }
        catch(Exception e) {

        }

    }

    /**
     * Restart a sync job. This method should only be called after an error occured. We set here a
     * larger time window to prevent too much server requests.
     * @param context
     */
    public static void restartSyncJob(Context context) {

        DataLog.getInstance().writeUploadLog(context, "SyncInfo", "restartSyncJob");

        DataLog.getInstance().writeUploadLog(context, "SyncInfo", "time: ");

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        String tag = "sync_job";

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean useMobileConnection = sharedPref.getBoolean(context.getResources().getString(R.string.key_upload_mobile_data), false);
        int[] constraints;
        if (useMobileConnection)
            constraints = new int[]{Constraint.ON_ANY_NETWORK};
        else
            constraints = new int[]{Constraint.ON_UNMETERED_NETWORK};

        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SyncService.class)
                // uniquely identifies the job
                .setTag(tag)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // start between 0 and 60 seconds from now
//                .setTrigger(Trigger.executionWindow(60, 100))
                .setTrigger(Trigger.executionWindow(1, 7))
//                .setTrigger(Trigger.NOW)
                // overwrite an existing job with the same tag - this assures that just one job is running at a time:
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
//                .setConstraints(
//                        // only run on an unmetered network
//                        Constraint.ON_UNMETERED_NETWORK,
//                        // only run when the device is charging
//                        Constraint.DEVICE_CHARGING
//                )
                .setConstraints(
                        constraints
                )
//                .setConstraints(
//                        Constraint.ON_ANY_NETWORK
//                )
                .build();



        dispatcher.mustSchedule(syncJob);

    }



    /**
     * Starts a new sync job. Note that a current job will not be overwritten.
     * @param context
     */
    public static void startSyncJob(Context context) {

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Log.d("SyncInfo", "startSyncJob");
        DataLog.getInstance().writeUploadLog(context, "SyncService", "startSyncJob");


        String tag = "sync_job";

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean useMobileConnection = sharedPref.getBoolean(context.getResources().getString(R.string.key_upload_mobile_data), false);
        int[] constraints;
        if (useMobileConnection)
            constraints = new int[]{Constraint.ON_ANY_NETWORK};
        else
            constraints = new int[]{Constraint.ON_UNMETERED_NETWORK};


//        if (useMobileConnection)


        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SyncService.class)
                // uniquely identifies the job
                .setTag(tag)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // start between 0 and 60 seconds from now
//                .setTrigger(Trigger.executionWindow(5, 15))
                .setTrigger(Trigger.executionWindow(1, 7))
//                .setTrigger(Trigger.NOW)
                // overwrite an existing job with the same tag - this assures that just one job is running at a time:
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
//                .setConstraints(
//                        // only run on an unmetered network
//                        Constraint.ON_UNMETERED_NETWORK,
//                        // only run when the device is charging
//                        Constraint.DEVICE_CHARGING
//                )
                .setConstraints(
                        constraints
                )
                .build();



        dispatcher.mustSchedule(syncJob);

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

    public void addToUnfinishedSyncList(File file, int uploadID) {

        mUnfinishedSyncList.add(new TranskribusFileSync(file, uploadID));

    }


    public ArrayList<FileSync> getSyncList() {

        return mFileSyncList;

    }

    private ArrayList<FileSync> getUploadedList() {

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

        if (files.length == 0)
            return false;

        // Is the dir already added to the upload list:
        if ((mUploadDirs != null) && (mUploadDirs.contains(dir))) {
//            Check if all files in the dir are added to the awaiting upload list:
            for (File file : files) {
                if (!mAwaitingUploadFiles.contains(file))
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
            switch(mState) {
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
