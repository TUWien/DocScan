package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 02.10.2017.
 */

public class SyncAdapter extends BaseDocumentAdapter {

    private SparseBooleanArray mSelections;


    public SyncAdapter(Context context) {

        super(context);

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
}
