package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;

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

    public void selectAllItems() {

//        setAllSelections(true);

    }

    public void deselectAllItems() {

//        setAllSelections(false);

    }

//    private void setAllSelections(boolean isSelected) {
//
//        for (int i = 0; i < mDirs.size(); i++) {
//            mSelections.put(i, isSelected);
//        }
//
////        We need to redraw the check boxes:
//        this.notifyDataSetChanged();
//
////        We need to inform the parent activity that the selection has changed:
////        mCallback.onSelectionChange(mDirs.size());
//        mCallback.onSelectionChange();
//    }

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

//    class FileComparator implements Comparator<File>
//    {
//        @Override public int compare(File file1, File file2)
//        {
//            int value;
//            if (isDirUploaded(file1) && !isDirUploaded(file2))
//                value = 1;
//            else if (!isDirUploaded(file1) && isDirUploaded(file2))
//                value = -1;
//            else {
//                value = file1.getName().compareToIgnoreCase(file2.getName());
//            }
//
//            return value;
//
//        }
//    }

//    private boolean isDirUploaded(File dir) {
//
//        File[] files = getFiles(dir);
//
//        return SyncInfo.getInstance().areFilesUploaded(files);
//
////        if (files.length == 0)
////            return false;
////
////        // Check if every file contained in the folder is already uploaded:
////        for (File file : files) {
////            if (!isFileUploaded(file))
////                return false;
////        }
////
////        return true;
//
//    }

//    private boolean isDirUploaded(int groupPosition) {
//
//        File dir = getGroupFile(groupPosition);
//        File[] files = getFiles(dir);
//
//        return SyncInfo.getInstance().areFilesUploaded(files);
//
////        if (files.length == 0)
////            return false;
////
////        // Check if every file contained in the folder is already uploaded:
////        for (File file : files) {
////            if (!isFileUploaded(file))
////                return false;
////        }
////
////        return true;
//
//    }

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

            ImageView imageView = convertView.findViewById(R.id.checkbox_list_group_image_view);


        }

        CheckBox checkBoxListHeader = (CheckBox) convertView.findViewById(R.id.checkboxListHeader);
        checkBoxListHeader.setTypeface(null, Typeface.BOLD);
        // reassigning checkbox to its ticked state
        checkBoxListHeader.setTag(groupPosition);
        checkBoxListHeader.setChecked(mSelections.get(groupPosition, false));

        // Skip the animation that usually arises after CheckBox.setChecked
        convertView.jumpDrawablesToCurrentState();

//        boolean isUploaded = isDirUploaded(groupPosition);

        boolean isUploaded = false;
        String headerText = headerTitle;
        boolean isCheckBoxEnabled;
        //        We need to use AppCompatResources for drawables from vector files for pre lollipop devices:
        Drawable d;
        if (isUploaded) {
            d = AppCompatResources.getDrawable(mContext, R.drawable.ic_cloud_done_gray_24dp);
//            isCheckBoxEnabled = false;
//            The checkbox is now (18.01.2018) checkable after upload, because it can be deleted:
            isCheckBoxEnabled = true;
        }
        else if (isDirAwaitingUpload(groupPosition)) {
            d = AppCompatResources.getDrawable(mContext, R.drawable.ic_cloud_upload_gray_24dp);
            headerText += " " + mContext.getResources().getString(R.string.sync_dir_pending_text);
            isCheckBoxEnabled = false;
        }
        else {
            d = AppCompatResources.getDrawable(mContext, R.drawable.ic_cloud_queue_gray_24dp);
            isCheckBoxEnabled = true;
        }

        checkBoxListHeader.setText(headerText);
        checkBoxListHeader.setEnabled(isCheckBoxEnabled);
        checkBoxListHeader.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

        Button showImageButton = convertView.findViewById(R.id.show_image_button);
        final String uri = ((File) getGroup(groupPosition)).getAbsolutePath();
        showImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GalleryActivity.class);
                intent.putExtra(mContext.getString(R.string.key_document_file_name), uri);
                mContext.startActivity(intent);
//                mContext.startActivity();
            }
        });

        ImageView imageView = convertView.findViewById(R.id.checkbox_list_group_image_view);

        GlideApp.with(mContext)
                .load(mFiles.get(groupPosition)[0].getPath())
                .into(imageView);

        return convertView;

    }


    private boolean isDirAwaitingUpload(int groupPosition) {

        File dir = getGroupFile(groupPosition);
        File[] files = getFiles(dir);

        return SyncInfo.getInstance().isDirAwaitingUpload(dir, files);

//        if (files.length == 0)
//            return false;
//
//        // Is the dir already added to the upload list:
//        if ((SyncInfo.getInstance().getUploadDirs() != null) && (SyncInfo.getInstance().getUploadDirs().contains(dir))) {
////            Check if all files in the dir are added to the awaiting upload list:
//            for (File file : files) {
//                if (!SyncInfo.getInstance().getAwaitingUploadFile().contains(file))
//                    return false;
//            }
//
//            return true;
//        }
//
//        return false;

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
        void onSelectionChange();
    }
}
