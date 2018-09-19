package at.ac.tuwien.caa.docscan.sync;

import java.io.File;

public class TranskribusSyncFile extends SyncFile {

    private int mUploadId;

    public TranskribusSyncFile(File file, int uploadId) {

        super(file);
        mUploadId = uploadId;

    }

    public int getUploadId() {

        return mUploadId;

    }
}
