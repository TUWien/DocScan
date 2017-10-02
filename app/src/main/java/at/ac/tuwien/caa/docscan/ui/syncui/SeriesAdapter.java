package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 26.09.2017.
 */

public class SeriesAdapter extends BaseDocumentAdapter {

//    private Context mContext;
//    private File[] mDirs;
//    private ArrayList<File[]> mFiles;
//
//
    public SeriesAdapter(Context context) {

        super(context);
//        mContext = context;
//
//        fillLists();

    }
//
//    public File getGroupFile(int position) {
//
//        return mDirs[position];
//
//    }
//
//
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
//        mFiles = new ArrayList<>(mDirs.length);
//        int i = 0;
//        for (File dir : mDirs) {
//            mFiles.add(getFiles(dir));
//            i++;
//        }
//
//    }
//
//    private File[] getFiles(File dir) {
//
//        FileFilter filesFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return !file.isDirectory();
//            }
//        };
//
//        File[] files = dir.listFiles(filesFilter);
//
//        return files;
//    }
//
//
//
//    @Override
//    public int getGroupCount() {
//        return mDirs.length;
//    }
//
//    @Override
//    public int getChildrenCount(int groupPosition) {
//        return mFiles.get(groupPosition).length;
//    }
//
//    @Override
//    public Object getGroup(int groupPosition) {
//        return mDirs[groupPosition];
//    }
//
//    @Override
//    public Object getChild(int groupPosition, int childPosition) {
//        return mFiles.get(groupPosition)[childPosition];
//    }
//
//    @Override
//    public long getGroupId(int groupPosition) {
//        return 0;
//    }
//
//    @Override
//    public long getChildId(int groupPosition, int childPosition) {
//        return 0;
//    }
//
//    @Override
//    public boolean hasStableIds() {
//        return false;
//    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        String headerTitle = ((File) getGroup(groupPosition)).getName();
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = ((File) getChild(groupPosition, childPosition)).getName();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        return convertView;
    }

//    @Override
//    public boolean isChildSelectable(int groupPosition, int childPosition) {
//        return false;
//    }

}
