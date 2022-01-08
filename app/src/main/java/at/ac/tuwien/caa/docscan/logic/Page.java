package at.ac.tuwien.caa.docscan.logic;

import java.io.File;

/**
 * Created by fabian on 01.02.2018.
 */
@Deprecated
public class Page {

    private File mFile;
    private String mTitle;
    private boolean mIsFocused = true;

    public Page() {

    }

    public Page(File file) {
        mFile = file;
    }

    public Page(File file, String title) {
        mFile = file;
        mTitle = title;
    }

    public boolean changeDir(File newDir) {

        File newFile = new File(newDir, mFile.getName());
        if (newFile.exists())
            return false;

        return true;

    }

    public boolean isFocused() {
        return mIsFocused;
    }

    public void setIsFocused(boolean isFocused) {
        mIsFocused = isFocused;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

}
