package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;

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

    private static SyncInfo mInstance = null;
    private ArrayList<FileSync> mFileSyncList;

    public static SyncInfo getInstance() {

        if (mInstance == null)
            mInstance = new SyncInfo();

        return mInstance;

    }

    public static boolean isInstanceNull() {

        return (mInstance == null);

    }

    public static void saveToDisk(Context context) {

        File syncPath = context.getFilesDir();
        File syncFile = new File(syncPath, "sync.txt");

        try {
            FileOutputStream fos = new FileOutputStream(syncFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mInstance);
            oos.close();
        }
        catch(Exception e) {

        }

    }

    public static void readFromDisk(Context context) {

        File syncPath = context.getFilesDir();
        File syncFile = new File(syncPath, "sync.txt");

        try {
            FileInputStream fis = new FileInputStream (syncFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mInstance = (SyncInfo) ois.readObject();
            ois.close();
        }
        catch(Exception e) {

        }

    }

    private SyncInfo() {

        mFileSyncList = new ArrayList<>();

    }

    public void addFile(File file) {

        if (mFileSyncList != null)
            mFileSyncList.add(new FileSync(file));
    }

    public ArrayList<FileSync> getSyncList() {
        return mFileSyncList;
    }

    public class FileSync  implements Serializable {

        public static final int STATE_NOT_UPLOADED = 0;
        public static final int STATE_AWAITING_UPLOAD = 1;
        public static final int STATE_UPLOADED = 2;


        private File mFile;
        private int mState;

        private FileSync(File file) {

            mFile = file;
            mState = STATE_NOT_UPLOADED;

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






}
