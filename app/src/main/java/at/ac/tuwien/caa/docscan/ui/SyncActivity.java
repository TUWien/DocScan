package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.sync.TranskribusUtils;
import at.ac.tuwien.caa.docscan.ui.syncui.SyncAdapter;

/**
 * Created by fabian on 22.09.2017.
 */

public class SyncActivity extends BaseNavigationActivity implements StartUploadRequest.StartUploadCallback {

    private ExpandableListView mListView;
    private Context mContext;
    private SyncAdapter mAdapter;
    // Describes at which index the checkbox is added in checkbox_list_group.xml to its parent layout:
    private static int CHECK_BOX_IDX = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        mListView = (ExpandableListView) findViewById(R.id.sync_list_view);
        mContext = this;

        mAdapter = new SyncAdapter(this);
        mListView.setAdapter(mAdapter);

        FloatingActionButton uploadButton = (FloatingActionButton) findViewById(R.id.upload_fab);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ArrayList<File> selectedDirs = new ArrayList<File>();

//                Iterate over the group items and find the checked items. I did not find a better
//                way to get the selected items...
                for (int i = 0; i < mListView.getChildCount(); i++) {
                    LinearLayout layout = (LinearLayout) mListView.getChildAt(i);
                    CheckBox checkBox = (CheckBox) layout.getChildAt(CHECK_BOX_IDX);
                    // If the checkBox is null a group item is expanded and the child is visited:
                    if (checkBox == null)
                        continue;

                    if (checkBox.isChecked())
                        selectedDirs.add(mAdapter.getGroupFile(i));

                }

                if (selectedDirs.size() == 0)
                    showNoDirSelectedAlert();
                else
                    uploadDirs(selectedDirs);
            }
        });
    }

    private void uploadDirs(ArrayList<File> dirs) {

//        TODO: do this for multiple items:
        File dir = dirs.get(0);
        JSONObject jsonObject = TranskribusUtils.getJSONObject(dir);
        if (jsonObject != null) {
            new StartUploadRequest(this, jsonObject);
        }


    }

    private void showNoDirSelectedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_no_dir_selected_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_no_dir_selected_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SYNC;
    }

    @Override
    public void onUploadStart(int uploadId) {

    }
}
