package at.ac.tuwien.caa.docscan.sync;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by fabian on 18.08.2017.
 */

public class SyncInfo {

    private static SyncInfo mInstance = null;
    private ArrayList<FileSync> mFileSyncList;

    public static SyncInfo getInstance() {

        if (mInstance == null)
            mInstance = new SyncInfo();

        return mInstance;

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

    public class FileSync {

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
