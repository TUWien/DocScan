package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.sync.TranskribusUtils;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;

import static at.ac.tuwien.caa.docscan.sync.TranskribusUtils.TRANSKRIBUS_UPLOAD_COLLECTION_NAME;

/**
 * Created by fabian on 22.09.2017.
 */

public class SyncActivity extends BaseNavigationActivity implements
        StartUploadRequest.StartUploadCallback, CollectionsRequest.CollectionsCallback,
        CreateCollectionRequest.CreateCollectionCallback, SyncAdapter.SyncAdapterCallback {

    private ExpandableListView mListView;
    private Context mContext;
    private SyncAdapter mAdapter;
    private int mNumUploadJobs;
    private int mTranskribusUploadCollId;
    private ArrayList<File> mSelectedDirs;
    private TextView mSelectionTextView;
    private Snackbar mSnackbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (User.getInstance().isUploadActive()) {
            Intent intent = new Intent(getApplicationContext(), UploadingActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_sync);

        // Read the upload information:
        SyncInfo.readFromDisk(this);

        mListView = (ExpandableListView) findViewById(R.id.sync_list_view);
        mContext = this;

        View footer = new View(this);
        // TODO: set device independent units (DP):

        int footerHeight = (int) getResources().getDimension(R.dimen.sync_footer_height);
        footer.setMinimumHeight(footerHeight);

        mListView.addFooterView(footer);

        mAdapter = new SyncAdapter(this, SyncInfo.getInstance().getSyncList());
        mListView.setAdapter(mAdapter);

        mSelectionTextView = (TextView) findViewById(R.id.sync_selection_textview);

        ImageButton uploadButton = (ImageButton) findViewById(R.id.start_upload_button);
//        Button uploadButton = (ImageButton) findViewById(R.id.start_upload_button);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                new CreateCollectionRequest(mContext, "DocScan");

                ArrayList<File> selectedDirs = mAdapter.getSelectedDirs();

                if (selectedDirs.size() == 0) {
                    showNoDirSelectedAlert();
                }
                else {
                    mSelectedDirs = selectedDirs;
                    showSnackbarInfo();
                    startUpload();
                }

            }
        });
    }

    private void showSnackbarInfo() {

        if (isOnline())
            showUploadingSnackbar();
        else
            showNotOnlineSnackbar();

    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private void showNotOnlineSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_offline_prefix_text) + " " +
                        getSelectionText() + " " +
                        getResources().getString(R.string.sync_snackbar_offline_postfix_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_selection_layout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    /**
     * Shows a snackbar indicating that the upload process starts. We need this because we have
     * little control of the time when the upload starts really.
     */
    private void showUploadingSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_uploading_prefix_text) + " " +
                getSelectionText() +
                getResources().getString(R.string.sync_snackbar_check_notifications_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_selection_layout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    private void closeSnackbar() {

        if (mSnackbar != null) {
            if (mSnackbar.isShown())
                mSnackbar.dismiss();
        }

    }

    private boolean isOnline() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }


    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    private void startUpload() {

        SyncInfo.getInstance().setUploadDirs(mSelectedDirs);
        SyncInfo.saveToDisk(this);

        SyncInfo.startSyncJob(this);
//        new CollectionsRequest(this);

    }



    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     * @param uploadId
     */
    @Override
    public void onUploadStart(int uploadId, String title) {

        File selectedDir = getMatchingDir(title);

        if (selectedDir != null) {
            for (File file : TranskribusUtils.getFiles(selectedDir))
                SyncInfo.getInstance().addTranskribusFile(mContext, file, uploadId);
        }

        mNumUploadJobs++;

        // For each directory the upload request is finished and all files are added to the sync list.
        // Now start the job:
        if (mNumUploadJobs == mSelectedDirs.size())
            SyncInfo.startSyncJob(this);

//        TranskribusUtils.getInstance().setUploadId(uploadId);
//        SyncInfo.startSyncJob(this);

    }

    /**
     * Find the directory assigned to its title:
     * @param title
     * @return
     */
    @Nullable
    private File getMatchingDir(String title) {
        File selectedDir = null;

        // Find the
        for (File dir : mSelectedDirs) {
            if (dir.getName().compareTo(title) == 0) {
                selectedDir = dir;
                break;
            }
        }
        return selectedDir;
    }

    private void uploadDirs(ArrayList<File> dirs) {

        mSelectedDirs = dirs;
        mNumUploadJobs = 0;

        for (File dir : mSelectedDirs) {

            // Get the image files contained in the directory:
            File[] imgFiles = TranskribusUtils.getFiles(dir);
            if (imgFiles == null)
                return;
            else if (imgFiles.length == 0)
                return;

//            Create the JSON object for the directory:
            JSONObject jsonObject = TranskribusUtils.getJSONObject(dir.getName(), imgFiles);
//            Start the upload request:
            if (jsonObject != null) {
                new StartUploadRequest(this, jsonObject, mTranskribusUploadCollId);
            }
        }
//
//
////        TODO: do this for multiple items:
//        File dir = dirs.get(0);
//
//        File[] imgFiles = TranskribusUtils.getFiles(dir);
//        if (imgFiles == null)
//            return;
//        else if (imgFiles.length == 0)
//            return;
//
//        SyncInfo.getInstance().createSyncList(imgFiles);




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
    public void onCollections(List<Collection> collections) {

        for (Collection collection : collections) {
            if (collection.getName().compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
                docScanCollectionFound(collection);
                return;
            }
        }

        createDocScanCollection();


    }

    private void createDocScanCollection() {

        new CreateCollectionRequest(this, TRANSKRIBUS_UPLOAD_COLLECTION_NAME);

    }

    private void docScanCollectionFound(Collection collection) {

//        User.getInstance().setTranskribusUploadCollId(collection.getID());
        mTranskribusUploadCollId = collection.getID();
        uploadDirs(mSelectedDirs);

    }

    @Override
    public void onCollectionCreated(String collName) {
        if (collName.compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0)
            new CollectionsRequest(this);

    }

    @Override
    public void onSelectionChange() {

        if (mSelectionTextView != null) {
            String text = getSelectionText();
            if (text != null)
                mSelectionTextView.setText(text);
        }
    }

    private String getSelectionText() {

        String selectionText = null;

        if (mAdapter != null) {

            int selCnt = mAdapter.getSelectedDirs().size();
            String postFix;
            if (selCnt == 1)
                postFix = getResources().getString(R.string.sync_selection_single_document_text);
            else
                postFix = getResources().getString(R.string.sync_selection_many_documents_text);
            selectionText = selCnt + " " + postFix;

        }

        return selectionText;
    }
}
