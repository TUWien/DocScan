package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
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

/**
 * Created by fabian on 18.08.2017.
 */

public class SyncInfo implements Serializable {


    private static final String SYNC_FILE_NAME = "syncinfo.txt";

    private static SyncInfo mInstance = null;
    private ArrayList<FileSync> mFileSyncList;
    private ArrayList<File> mUploadDirs;

    public static SyncInfo getInstance() {

        if (mInstance == null) {
            mInstance = new SyncInfo();
        }

        return mInstance;

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
     * Starts a new sync job. Note that a current job will not be overwritten.
     * @param context
     */
    public static void startSyncJob(Context context) {

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));


        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SyncService.class)
                // uniquely identifies the job
                .setTag("my-unique-tag")
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.FOREVER)
                // start between 0 and 60 seconds from now
                .setTrigger(Trigger.executionWindow(0, 0))
//                .setTrigger(Trigger.NOW)
                // don't overwrite an existing job with the same tag
                .setReplaceCurrent(false)
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
                        // only run on an unmetered network
                        Constraint.ON_UNMETERED_NETWORK
                )
                .build();

        dispatcher.mustSchedule(syncJob);

    }

    public static void readFromDisk(Context context) {

        File syncPath = context.getFilesDir();
        File syncFile = new File(syncPath, SYNC_FILE_NAME);

        try {
            FileInputStream fis = new FileInputStream (syncFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mInstance = (SyncInfo) ois.readObject();
            Log.d("SyncInfo", "SyncInfo list: " + mInstance.getSyncList().size());
            ois.close();
        }
        catch(Exception e) {

        }

    }

    private SyncInfo() {

        mFileSyncList = new ArrayList<>();

    }

    public void addFile(Context context, File file) {

        if (mFileSyncList != null) {
            mFileSyncList.add(new FileSync(file));
//            startSyncJob(context);
        }

    }

    public void setUploadDirs(ArrayList<File> dirs) {

        mUploadDirs = dirs;

    }

    public ArrayList<File> getUploadDirs() {

        return mUploadDirs;
    }

    public void addTranskribusFile(Context context, File file, int uploadId) {

        if (mFileSyncList != null) {
            mFileSyncList.add(new TranskribusFileSync(file, uploadId));
//            startSyncJob(context);
        }

    }


    public void createSyncList(File[] files) {

        if (mFileSyncList == null)
            mFileSyncList = new ArrayList<>();
        else
            mFileSyncList.clear();

        for (File file : files)
            mFileSyncList.add(new FileSync(file));

    }

    public ArrayList<FileSync> getSyncList() {
        return mFileSyncList;
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
