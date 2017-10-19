package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.widget.BaseExpandableListAdapter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;

/**
 * Created by fabian on 02.10.2017.
 */

public abstract class BaseDocumentAdapter extends BaseExpandableListAdapter {


    protected Context mContext;
    protected ArrayList<File> mDirs;
    protected ArrayList<File[]> mFiles;


    public BaseDocumentAdapter(Context context) {

        super();
        mContext = context;

//        fillLists();

    }

    public File getGroupFile(int position) {

        return mDirs.get(position);
//        return mDirs[position];

    }

    protected void fillLists() {

        File mediaStorageDir = Helper.getMediaStorageDir(mContext.getResources().getString(R.string.app_name));

        if (mediaStorageDir == null)
            return;

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] dirs = mediaStorageDir.listFiles(directoryFilter);
        mDirs = new ArrayList<>(Arrays.asList(dirs));

//        TODO: sort!
        java.util.Collections.sort(mDirs, new FileComparator());
        mFiles = new ArrayList<>(mDirs.size());
        for (File dir : mDirs) {
            mFiles.add(getFiles(dir));
        }

    }

    class FileComparator implements Comparator<File>
    {
        @Override public int compare(File file1, File file2)
        {

            return file1.getName().compareToIgnoreCase(file2.getName());

        }
    }


//    private void fillLists() {
//
//        File mediaStorageDir = Helper.getMediaStorageDir(mContext.getResources().getString(R.string.app_name));
//
//        if (mediaStorageDir == null)
//            return;
//
//        FileFilter directoryFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return file.isDirectory();
//            }
//        };
//
//        mDirs = mediaStorageDir.listFiles(directoryFilter);
//        Arrays.sort(mDirs);
//        mFiles = new ArrayList<>(mDirs.length);
//        int i = 0;
//        for (File dir : mDirs) {
//            mFiles.add(getFiles(dir));
//            i++;
//        }
//
//
//
//    }

    protected File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }



    @Override
    public int getGroupCount() {
        return mDirs.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mFiles.get(groupPosition).length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mDirs.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mFiles.get(groupPosition)[childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


}
