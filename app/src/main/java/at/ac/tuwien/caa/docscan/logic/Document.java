package at.ac.tuwien.caa.docscan.logic;

import java.util.ArrayList;

/**
 * Created by fabian on 01.02.2018.
 */

public class Document {

    private String mTitle;
    private ArrayList<Page> mPages;

    public Document() {

    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public ArrayList<Page> getPages() {
        return mPages;
    }

    public void setPages(ArrayList<Page> mPages) {
        this.mPages = mPages;
    }
}
