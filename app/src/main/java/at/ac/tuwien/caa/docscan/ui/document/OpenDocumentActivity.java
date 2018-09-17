package at.ac.tuwien.caa.docscan.ui.document;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DocumentStore;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.syncui.SeriesAdapter;

/**
 * Created by fabian on 26.09.2017.
 */

public class OpenDocumentActivity extends BaseNoNavigationActivity {

    private ExpandableListView mListView;
    private Context mContext;
    private File mSelectedDir = null;
    private SeriesAdapter mAdapter;

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_document);
        super.initToolbarTitle(R.string.document_title);

        mListView = findViewById(R.id.document_list_view);
        mContext = this;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            // Sets the adapter, note: This fills the list.
            updateListViewAdapter();
        }

        addFooter();

        final Context c = this;
        final Activity a = this;
        Button selectSeries = findViewById(R.id.document_select_button);
        selectSeries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedDir != null) {
                    User.getInstance().setDocumentName(mSelectedDir.getName());
                    UserHandler.saveSeriesName(c);

//                    DocumentStore.getInstance().createNewDocument(mSelectedDir.getName());

//                    Settings.getInstance().saveKey(a, Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY, true);
//                    Settings.getInstance().saveKey(a, Settings.SettingEnum.SERIES_MODE_PAUSED_KEY, false);

                    Helper.startCameraActivity(c);

                }
                else
                    showNoFileSelectedAlert();
            }
        });



        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long l) {
                if (mAdapter!= null)
                    mSelectedDir = mAdapter.getGroupFile(groupPosition);
                return false;
            }
        });
    }

    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode Request code
     * @param permissions Permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);

        if (requestCode == PERMISSION_READ_EXTERNAL_STORAGE && isPermissionGiven) {
            // Sets the adapter, note: This fills the list.
            updateListViewAdapter();
        }
        else
            showNoPermissionAlert();

    }

    private void showNoPermissionAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_no_permission_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.document_no_permission_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    /**
     * Adds some space to the end of the list, so that the overlapping RelativeLayout is not
     * overlapping in case the user scrolls to the end of the list.
     */
    private void addFooter() {
        View footer = new View(this);
        int footerHeight = (int) getResources().getDimension(R.dimen.sync_footer_height);
        footer.setMinimumHeight(footerHeight);
        mListView.addFooterView(footer);
    }



    private void showNoFileSelectedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_no_dir_selected_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.document_no_dir_selected_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    /**
     * Updates the list view adapter and causes a new filling of the list view.
     */
    private void updateListViewAdapter() {

        if (mContext != null) {
            mAdapter = new SeriesAdapter(this);
            if (mListView != null)
                mListView.setAdapter(mAdapter);
        }

    }


}
