package at.ac.tuwien.caa.docscan.logic;

import java.io.File;

/**
 * Created by fabian on 01.02.2018.
 */

public class Page {

    private File mFile;

    public Page() {

    }

    public Page(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

}
