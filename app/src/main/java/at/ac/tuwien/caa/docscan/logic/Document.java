package at.ac.tuwien.caa.docscan.logic;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.ui.document.DocumentMetaData;

/**
 * Created by fabian on 01.02.2018.
 */

public class Document {

    private String mTitle;
    private ArrayList<Page> mPages;
    private boolean mIsUploaded = false;
    private boolean mIsCropped = false;
    private boolean mIsAwaitingUpload = false;

    private DocumentMetaData mMetaData = null;

    public Document() {

    }

    public Document(String title) {

        mTitle = title;
        mPages = new ArrayList<>();

    }

    public void setMetaData(DocumentMetaData metaData) {

        mMetaData = metaData;

    }

    public DocumentMetaData getMetaData() {

        return mMetaData;

    }

    public ArrayList<File> getFiles() {

        if (mPages == null)
            return null;

        ArrayList<File> files = new ArrayList<>(mPages.size());
        for (Page page : mPages) {
            files.add(page.getFile());
        }

        return files;

    }

    public void setDir(File newDir) {

        mTitle = newDir.getName();

        for (Page page : mPages)
            page.changeDir(newDir);

    }

    //    TODO: temporary
    public File getDir() {

        if (mPages != null) {
            if (mPages.size() > 0) {
                if (mPages.get(0).getFile() != null)
                    return mPages.get(0).getFile().getParentFile();
            }
        }

        return null;

    }

    public void setIsAwaitingUpload(boolean isAwaitingUpload) {

        mIsAwaitingUpload = isAwaitingUpload;

    }

    public boolean isAwaitingUpload() {

        return mIsAwaitingUpload;

    }

    public void setIsUploaded(boolean isUploaded) {

        mIsUploaded = isUploaded;

    }

    public boolean isUploaded() {

        return mIsUploaded;

    }

    public void setIsCropped(boolean isCropped) {

        mIsCropped = isCropped;

    }

    public boolean isCropped() {

        return mIsCropped;

    }

    public String getTitle() {
        return mTitle;

    }

    public void setTitle(String title) {

        mTitle = title;

    }

    public ArrayList<Page> getPages() {

        return mPages;

    }

    public void setPages(ArrayList<Page> pages) {

        mPages = pages;

    }
}
