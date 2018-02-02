package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.PageSlideActivity;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.ui.DocumentViewActivity;

/**
 * Created by fabian on 02.10.2017.
 */

public class SyncAdapter extends BaseDocumentAdapter {

    private SparseBooleanArray mSelections;
    private ArrayList<SyncInfo.FileSync> mFileSyncList;
    // Callback to listen to selection changes:
    private SyncAdapterCallback mCallback;


    public SyncAdapter(Context context, ArrayList<SyncInfo.FileSync> fileSyncList) {

        super(context);

        mCallback = (SyncAdapterCallback) context;
        mFileSyncList = fileSyncList;

        // Stores the checkbox states
        mSelections = new SparseBooleanArray();

        fillLists();

    }

    public ArrayList<File> getSelectedDirs() {

        ArrayList<File> selectedDirs = new ArrayList<>();

        for (int i = 0; i < getGroupCount(); i++)
            if (mSelections.get(i))
                selectedDirs.add(getGroupFile(i));

        return selectedDirs;

    }

    /**
     * Fills the list with directories that contain at least one image.
     */
    @Override
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

        // Sort it based on the upload status:
        java.util.Collections.sort(mDirs, new FileComparator());

        mFiles = new ArrayList<>(mDirs.size());

        ArrayList<Integer> rmIdx = new ArrayList<>();

        int idx = 0;
        for (File dir : mDirs) {

            if (getFiles(dir).length == 0)
                rmIdx.add(idx);

            else
                mFiles.add(getFiles(dir));

            idx++;
        }

        // Remove empty directories:
        for (int i = rmIdx.size() - 1; i >= 0; i--)
            mDirs.remove(rmIdx.get(i).intValue());



    }

    class FileComparator implements Comparator<File>
    {
        @Override public int compare(File file1, File file2)
        {
            int value;
            if (isDirUploaded(file1) && !isDirUploaded(file2))
                value = 1;
            else if (!isDirUploaded(file1) && isDirUploaded(file2))
                value = -1;
            else {
                value = file1.getName().compareToIgnoreCase(file2.getName());
            }

            return value;

        }
    }

    private boolean isDirUploaded(File dir) {

        File[] files = getFiles(dir);

        if (files.length == 0)
            return false;

        // Check if every file contained in the folder is already uploaded:
        for (File file : files) {
            if (!isFileUploaded(file))
                return false;
        }

        return true;

    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        String headerTitle = ((File) getGroup(groupPosition)).getName();
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.checkbox_list_group, null);

            // We have to store the state of the checkbox manually, because the views are recycled.
            // Got this solution from
            // https://stackoverflow.com/questions/35285635/when-click-on-expandable-listview-then-checked-checkbox-is-automatically-uncheck
            CheckBox checkBoxListHeader = (CheckBox) convertView.findViewById(R.id.checkboxListHeader);
            checkBoxListHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) v.getTag();
                    mSelections.put(position, !mSelections.get(position, false));
                    ((CheckBox)v).setChecked(mSelections.get(position, false));

                    mCallback.onSelectionChange();
                }
            });

        }

        CheckBox checkBoxListHeader = (CheckBox) convertView.findViewById(R.id.checkboxListHeader);
        checkBoxListHeader.setTypeface(null, Typeface.BOLD);
        checkBoxListHeader.setText(headerTitle);
        // reassigning checkbox to its ticked state
        checkBoxListHeader.setTag(groupPosition);
        checkBoxListHeader.setChecked(mSelections.get(groupPosition, false));

        // Skip the animation that usually arises after CheckBox.setChecked
        convertView.jumpDrawablesToCurrentState();

        boolean isUploaded = isDirUploaded(groupPosition);

        Drawable d = null;
        if (isUploaded) {
            d = mContext.getResources().getDrawable(R.drawable.ic_cloud_done_black_24dp);
            checkBoxListHeader.setEnabled(false);
        }
        else {
            d = mContext.getResources().getDrawable(R.drawable.ic_cloud_queue_black_24dp);
            checkBoxListHeader.setEnabled(true);
        }

        checkBoxListHeader.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

        Button showImageButton = convertView.findViewById(R.id.show_image_button);
        final String uri = ((File) getGroup(groupPosition)).getAbsolutePath();
        showImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PageSlideActivity.class);
                intent.putExtra("DOCUMENT_FILE_NAME", uri);
                mContext.startActivity(intent);
//                mContext.startActivity();
            }
        });


        return convertView;

    }

    private boolean isDirUploaded(int groupPosition) {

        File dir = getGroupFile(groupPosition);
        File[] files = getFiles(dir);

        if (files.length == 0)
            return false;

        // Check if every file contained in the folder is already uploaded:
        for (File file : files) {
            if (!isFileUploaded(file))
                return false;
        }

        return true;

    }

    private boolean isFileUploaded(File file) {

        for (SyncInfo.FileSync fileSync : mFileSyncList) {
            if ((file.getAbsolutePath().compareTo(fileSync.getFile().getAbsolutePath()) == 0)
                    && fileSync.getState() == SyncInfo.FileSync.STATE_UPLOADED)
                return true;
        }

        return false;
    }

//    @Override
//    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
//
//        final String childText = ((File) getChild(groupPosition, childPosition)).getName();
//
//        if (convertView == null) {
//            LayoutInflater infalInflater = (LayoutInflater) mContext
//                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = infalInflater.inflate(R.layout.list_item, null);
//
////            TODO: open the file on a click. The code below is not working on targetSDK > 24
////            Solution is here: https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
//
////            TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);
////            txtListChild.setOnClickListener(new View.OnClickListener() {
////                @Override
////                public void onClick(View v) {
////
////                    Intent intent = new Intent(Intent.ACTION_VIEW);
////                    File dir = getGroupFile(groupPosition);
////                    intent.setData(Uri.fromFile(getFiles(dir)[childPosition]));
////                    mContext.startActivity(intent);
////
////                }
////            });
//        }
//
//        TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);
//
//        txtListChild.setText(childText);
//
//        return convertView;
//
//    }

    public interface SyncAdapterCallback {
        public void onSelectionChange();
    }
}
