package at.ac.tuwien.caa.docscan.sync;

import java.io.File;

@Deprecated
public class SyncFile {

    public static final int STATE_NOT_UPLOADED = 0;
    public static final int STATE_AWAITING_UPLOAD = 1;
    public static final int STATE_UPLOADED = 2;


    private File mFile;
    private int mState;
    private Long mModifiedDate;


    public SyncFile(File file) {

        mFile = file;
        mState = STATE_NOT_UPLOADED;

    }

    public SyncFile(File file, int state) {

        mFile = file;
        mState = state;

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

    public Long getModifiedDate() {

        return mModifiedDate;

    }

    public void setState(int state) {

        mState = state;
//        Store the modification date at upload time so that we can check for local changes that
//        should be uploaded:
        if (state == STATE_UPLOADED)
            mModifiedDate = mFile.lastModified();

    }

    public File getFile() {
        return mFile;
    }
}
