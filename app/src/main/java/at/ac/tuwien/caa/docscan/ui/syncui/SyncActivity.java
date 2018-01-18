package at.ac.tuwien.caa.docscan.ui.syncui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.LoginActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;

import static at.ac.tuwien.caa.docscan.ui.LoginActivity.PARENT_ACTIVITY_NAME;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_ERROR_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_FINISHED_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_OFFLINE_ERROR_ID;

/**
 * Created by fabian on 22.09.2017.
 */

public class SyncActivity extends BaseNavigationActivity implements SyncAdapter.SyncAdapterCallback {

    private ExpandableListView mListView;
    private Context mContext;
    private SyncAdapter mAdapter;
    private int mNumUploadJobs;
    private int mTranskribusUploadCollId;
    private ArrayList<File> mSelectedDirs;
    private TextView mSelectionTextView;
    private RelativeLayout mSelectionLayout;
    private Snackbar mSnackbar;
    private ProgressBar mProgressBar;

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);

        if (isOnline() && !User.getInstance().isLoggedIn()) {

            setContentView(R.layout.activity_not_logged_in);

            Button loginButton = (Button) findViewById(R.id.sync_not_logged_in_button);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.putExtra(PARENT_ACTIVITY_NAME, this.getClass().getName().toString());
                    startActivity(intent);
                }
            });

            return;
        }


        setContentView(R.layout.activity_sync);

        // Read the upload information:
        SyncInfo.readFromDisk(this);

        mListView = (ExpandableListView) findViewById(R.id.sync_list_view);
        mContext = this;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            // Sets the adapter, note: This fills the list.
            updateListViewAdapter();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        }

        addFooter();

        mSelectionTextView = (TextView) findViewById(R.id.sync_selection_textview);
        mSelectionLayout = (RelativeLayout) findViewById(R.id.sync_selection_layout);
        mProgressBar = (ProgressBar) findViewById(R.id.sync_progressbar);

        initUploadButton();
        initDeleteButton();

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "PROGRESS_INTENT_NAME".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("PROGRESS_INTENT_NAME"));

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
            showNoPermissionSnackbar();

    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private void showNoPermissionSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_no_permissions);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    private void initDeleteButton() {

        ImageButton deleteButton = findViewById(R.id.sync_delete_button);
        final Context context = this;
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                new CreateCollectionRequest(mContext, "DocScan");

                ArrayList<File> selectedDirs = mAdapter.getSelectedDirs();

                if (selectedDirs.size() == 0) {
                    showNoDirSelectedAlert();
                }
                else {

                    // TODO: show the confirmation dialog here

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // ask for permission:
                        ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
                    }

                    boolean isFolderDeleted = true;
                    for (File folder : selectedDirs) {
                        isFolderDeleted = isFolderDeleted && deleteFolder(folder);
                    }

                    updateListViewAdapter();
                    // update the selection display:
                    onSelectionChange();

                    showDocumentsDeletedSnackbar(selectedDirs.size());

                    if (!isFolderDeleted) {
                        // TODO: show an error message here.
                    }
                }

            }
        });

    }

    /**
     * Deletes a folder and the contained files. Note that File.delete does not delete non empty
     * folder, hence the function deletes the files before deleting the folder.
     * @param file
     * @return
     */
    private static boolean deleteFolder(File file) {

        if (!file.exists() || !file.isDirectory())
            return false;

        boolean isFolderDeleted = true;
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                isFolderDeleted = isFolderDeleted && deleteFolder(files[i]);
            else
                isFolderDeleted = isFolderDeleted && files[i].delete();
        }

        return isFolderDeleted;


    }

    private void initUploadButton() {

        ImageButton uploadButton = findViewById(R.id.sync_upload_button);
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

                    if (!isOnline()) {
                        showNotOnlineSnackbar();
                        updateListViewAdapter();
                        // update the selection display:
                        onSelectionChange();
                        checkFolderOnlineStatusAndUpload(mSelectedDirs);
//                        startUpload();
                    }
                    else {
                        showUploadingSnackbar();
                        updateListViewAdapter();
//                        mProgressBar.setProgress(0);
//                        displayUploadActive(true);
                        checkFolderOnlineStatusAndUpload(mSelectedDirs);
//                        startUpload();
                    }

                }

            }
        });
    }

    /**
     * Uploads a list of folders if they are not already uploaded.
     * @param selectedDirs
     */
    private void checkFolderOnlineStatusAndUpload(ArrayList<File> selectedDirs) {

        ArrayList<File> uploadDirs = new ArrayList<>();
        for (File file : selectedDirs) {
            // Just add the folder if all files contained are not uploaded:
            File[] files = file.listFiles();
            if (!SyncInfo.getInstance().areFilesUploaded(files))
                uploadDirs.add(file);
        }

        if (uploadDirs.isEmpty()) {
//          TODO: show a snackbar that all documents are already uploaded.
        }
        else
            startUpload(uploadDirs);


    }

    private void displayUploadActive(boolean isUploadActive) {

        // Views that are not visible during upload:
        int nonUploadViewVisibility;
        // Views that are visible during upload:
        int uploadViewVisibility;

        if (isUploadActive) {
            nonUploadViewVisibility = View.INVISIBLE;
            uploadViewVisibility = View.VISIBLE;
        }
        else {
            nonUploadViewVisibility = View.VISIBLE;
            uploadViewVisibility = View.INVISIBLE;
        }


        // Show or hide views that are not visible during upload:
        if (mSelectionLayout != null)
            mSelectionLayout.setVisibility(nonUploadViewVisibility);
        if (mListView != null)
            mListView.setVisibility(nonUploadViewVisibility);

        // Show or hide views that are visible during upload:
        if (mProgressBar != null)
            mProgressBar.setVisibility(uploadViewVisibility);
//            mProgressBar.setProgress(0);

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

//    /**
//     * Shows a snackbar indicating that the device is offline.
//     */
//    private void showNotOnlineSnackbar() {
//
//        String snackbarText =
//                getResources().getString(R.string.sync_snackbar_offline_prefix_text) + " " +
//                        getSelectionText() + " " +
//                        getResources().getString(R.string.sync_snackbar_offline_postfix_text);
//
//        closeSnackbar();
//        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
//                snackbarText, Snackbar.LENGTH_LONG);
//        mSnackbar.show();
//
//    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private void showNotOnlineSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_offline_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    /**
     * Shows a snackbar indicating that documents have been deleted.
     */
    private void showDocumentsDeletedSnackbar(int numDoc) {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_files_deleted_prefix);
        snackbarText += " " + Integer.toString(numDoc) + " ";
        if (numDoc > 1)
            snackbarText += getResources().getString(R.string.sync_selection_many_documents_text);
        else
            snackbarText += getResources().getString(R.string.sync_selection_single_document_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
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
                getSelectionText() + ".";

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.show();

    }

    /**
     * Shows a snackbar indicating that the upload process starts. We need this because we have
     * little control of the time when the upload starts really.
     */
    private void showUploadFinishedSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_finished_upload_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();


    }

    private void showUploadErrorSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_error_upload_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    private void showUploadErrorDialog() {

        if (mContext == null)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_error_upload_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_error_upload_text);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

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
    private void startUpload(ArrayList<File> uploadDirs) {

//        SyncInfo.getInstance().setUploadDirs(mSelectedDirs);

        SyncInfo.getInstance().addUploadDirs(uploadDirs);
        SyncInfo.saveToDisk(this);
        SyncInfo.startSyncJob(this);

    }

    private void showServerChangedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_server_changed_title)
//                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_server_changed_text);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();



        // show it
        alertDialog.show();

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

    /**
     * Handles broadcast intents which inform about the upload progress:
      */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            boolean error = intent.getBooleanExtra(UPLOAD_ERROR_ID, false);

            if (error) {

//                showUploadErrorSnackbar();
                showUploadErrorDialog();
                updateListViewAdapter();
                displayUploadActive(false);

                // update the selection display:
                onSelectionChange();


                return;
            }

            boolean offlineError = intent.getBooleanExtra(UPLOAD_OFFLINE_ERROR_ID, false);

            if (offlineError) {

                showNotOnlineSnackbar();
                updateListViewAdapter();
                displayUploadActive(false);

                // update the selection display:
                onSelectionChange();


                return;
            }

            boolean finished = intent.getBooleanExtra(UPLOAD_FINISHED_ID, false);

            if (finished) {

                showUploadFinishedSnackbar();
                updateListViewAdapter();
                displayUploadActive(false);

                // update the selection display:
                onSelectionChange();

            }
            else {

//                displayUploadActive(true);
//                final int progress = intent.getIntExtra(UPLOAD_PROGRESS_ID, 0);
////                mProgressBar.setProgress(progress);
//
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mProgressBar.setProgress(progress);
//                    }
//                });

            }



        }
    };

    /**
     * Updates the list view adapter and causes a new filling of the list view.
     */
    private void updateListViewAdapter() {

        if (mContext != null) {
            mAdapter = new SyncAdapter(mContext, SyncInfo.getInstance().getSyncList());
            if (mListView != null)
                mListView.setAdapter(mAdapter);
        }

    }
}
