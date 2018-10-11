package at.ac.tuwien.caa.docscan.sync;

import java.io.File;

public class DropboxSyncFile extends SyncFile {

    private String mDocumentName;

    public DropboxSyncFile(File file, String documentName) {

        super(file);
        mDocumentName = documentName;

    }

    public String getDocumentName() {
        return mDocumentName;
    }

}
