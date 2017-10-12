package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;

/**
 * Created by fabian on 02.10.2017.
 */

public class SyncAdapter extends BaseDocumentAdapter {

    private SparseBooleanArray mSelections;
    private ArrayList<SyncInfo.FileSync> mFileSyncList;


    public SyncAdapter(Context context, ArrayList<SyncInfo.FileSync> fileSyncList) {

        super(context);

        mFileSyncList = fileSyncList;

        // Stores the checkbox states
        mSelections = new SparseBooleanArray();

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
        }
        else {
            d = mContext.getResources().getDrawable(R.drawable.ic_cloud_queue_black_24dp);
        }

        checkBoxListHeader.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);


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
}
