package at.ac.tuwien.caa.docscan.ui.syncui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.LoginActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

import static at.ac.tuwien.caa.docscan.ui.LoginActivity.PARENT_ACTIVITY_NAME;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_ERROR_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_FINISHED_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_OFFLINE_ERROR_ID;

/**
 * Created by fabian on 4/5/2018.
 */

public class UploadActivity extends BaseNavigationActivity implements DocumentAdapter.DocumentAdapterCallback {

    private Context mContext;
    private ListView mListView;
    private DocumentUploadAdapter mAdapter;
    private SelectionToolbar mSelectionToolbar;
    private List<Document> mDocuments;
    private Snackbar mSnackbar;
    private Menu mMenu;

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        mContext = this;

        if (Helper.isOnline(this) && !User.getInstance().isLoggedIn()) {
//            If the user is not online show the corresponding activity and do nothing else:
            showActivityNotLoggedIn();
            return;
        }


        setContentView(R.layout.activity_upload);

//        By default hiding toolbar is not working with ListView in a CoordinatorLayout.
//        Got this from: https://stackoverflow.com/questions/37450660/hiding-toolbar-with-listview-android
        mListView = findViewById(R.id.upload_list_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mListView.setNestedScrollingEnabled(true);
        }

        initToolbar();

        // Read the upload information:
        SyncInfo.readFromDisk(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            // Sets the adapter, note: This fills the list.
            initAdapter();
        }

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "PROGRESS_INTENT_NAME".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("PROGRESS_INTENT_NAME"));

    }

    @Override
    protected void onResume() {
        super.onResume();

//        TODO: check if it is really necessary to reload the documents:
        initAdapter();

        showNoSelectionToolbar();

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
            initAdapter();
        }
        else
            showNoPermissionSnackbar();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                if (getSelectedDocuments().size() > 0) {
//                    Clear the selections:
                    deselectListViewItems();
//                    Show the normal toolbar:
                    showNoSelectionToolbar();
                }

                else {
//                    We need to show the navigation drawer manually in this case:
                    mNavigationDrawer.showNavigation();
                }


        }

        return true;

    }

    private void showActivityNotLoggedIn() {
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
    }

    private void initToolbar() {

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.gallery_appbar);

        mSelectionToolbar = new SelectionToolbar(this, toolbar, appBarLayout);

//        Enable back navigation in action bar:
        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


    /**
     * Updates the list view adapter and causes a new filling of the list view.
     */
    private void initAdapter() {

        if (mContext != null) {

//            mDocuments = Helper.getDocuments(getResources().getString(R.string.app_name));

            List<Document> allDocuments = Helper.getDocuments(getResources().getString(R.string.app_name));
            mDocuments = Helper.getNonEmptyDocuments(allDocuments);

            mAdapter = new DocumentUploadAdapter(mContext, R.layout.rowlayout, mDocuments);

            if (mListView != null) {
                mListView.setAdapter(mAdapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        onSelectionChange();
                    }
                });
            }
        }

//        onSelectionChange();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sync_menu, menu);

        mMenu = menu;

        return true;

    }



    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.UPLOAD;
    }


    @Override
    public void onSelectionChange() {

        ArrayList<Document> selectedDocuments = getSelectedDocuments();

        if (mSelectionToolbar != null) {
            if (selectedDocuments.size() == 0)
                showNoSelectionToolbar();
            else
                showSelectionToolbar(selectedDocuments.size());
        }

    }

    /**
     * Called after the MenuItem for select all is clicked.
     * @param item
     */
    public void selectAllItems(MenuItem item) {

        selectListViewItems();

    }

    /**
     * Called after the MenuItem for delete selected is clicked.
     * @param item
     */
    public void deleteSelectedItems(MenuItem item) {

//        deselectListViewItems();
        showDeleteConfirmationDialog();

    }

    private void showDeleteConfirmationDialog() {

        if (mContext == null)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        String deleteText = getResources().getString(R.string.sync_confirm_delete_prefix_text);
        final ArrayList<Document> documents = getSelectedDocuments();
        deleteText += " " + Integer.toString(documents.size());
        deleteText += " " + Helper.getDocumentSingularPlural(this, documents.size());
        deleteText += " " + getResources().getString(R.string.sync_confirm_delete_postfix_text);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_confirm_delete_title)
                .setPositiveButton(R.string.sync_confirm_delete_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        deleteSelectedDocuments(documents);
                    }
                })
                .setNegativeButton(R.string.sync_cancel_delete_button_text, null)
                .setCancelable(true)
                .setMessage(deleteText);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void deleteSelectedDocuments(ArrayList<Document> documents) {

        boolean isFolderDeleted = true;
        for (Document document : documents) {
            isFolderDeleted = isFolderDeleted && deleteFolder(document.getDir());
        }

        showDocumentsDeletedSnackbar(documents.size());

        initAdapter();
        deselectListViewItems();
        // update the selection display:
//        onSelectionChange();



        if (!isFolderDeleted) {
            // TODO: show an error message here.
        }
    }

    /**
     * Shows a snackbar indicating that documents have been deleted.
     */
    private void showDocumentsDeletedSnackbar(int numDoc) {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_files_deleted_prefix);
        snackbarText += " " + Integer.toString(numDoc) + " ";
        snackbarText += Helper.getDocumentSingularPlural(this, numDoc);
//        if (numDoc > 1)
//            snackbarText += getResources().getString(R.string.sync_selection_many_documents_text);
//        else
//            snackbarText += getResources().getString(R.string.sync_selection_single_document_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

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


    /**
     * Called after the MenuItem for upload selected is clicked.
     * @param item
     */
    public void startUpload(MenuItem item) {

        startUpload();

    }

    private void startUpload() {

        ArrayList<Document> documents = getSelectedDocuments();

        if (documents.size() == 0) {
            showNoDirSelectedAlert();
        }

        else {
            deselectListViewItems();
            checkFolderOnlineStatusAndUpload(documents);
            initAdapter();
        }


    }

    /**
     * Uploads a list of folders if they are not already uploaded.
     * @param documents
     */
    private void checkFolderOnlineStatusAndUpload(ArrayList<Document> documents) {

        ArrayList<File> uploadDirs = new ArrayList<>();
        for (Document document : documents) {
            // Just add the folder if all files contained are not uploaded:
            if (!document.isUploaded()) {
                if (document.getDir() != null)
                    uploadDirs.add(document.getDir());
            }

        }

        if (uploadDirs.isEmpty()) {
            showAlreadyUploadedSnackbar();
        }
        else {

            if (Helper.isOnline(this))
                showUploadingSnackbar(); // tell the user that the uploaded started
            else
                showNotOnlineSnackbar();

            startUpload(uploadDirs);

        }


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
     * Shows a snackbar indicating that the upload process starts. We need this because we have
     * little control of the time when the upload starts really.
     */
    private void showUploadingSnackbar() {

        int selCnt = getSelectionCount();
        String selText = selCnt + " " + Helper.getDocumentSingularPlural(this, selCnt);
        if (selText == null)
            return;

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_uploading_prefix_text) + " " +
                        selText + ".";

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.show();

    }

    /**
     * Shows a snackbar indicating that all selected files are already uploaded and nothing is done.
     */
    private void showAlreadyUploadedSnackbar() {

        String snackbarText = getResources().getString(R.string.sync_snackbar_already_uploaded_prefix_text);

        int selCnt = getSelectionCount();

        snackbarText += " " + Helper.getDocumentSingularPlural(this, selCnt);
        if (selCnt == 1)
            snackbarText += " " + getResources().getString(R.string.sync_snackbar_already_uploaded_singular_text);
        else
            snackbarText += " " + getResources().getString(R.string.sync_snackbar_already_uploaded_plural_text);
        snackbarText += " " + getResources().getString(R.string.sync_snackbar_already_uploaded_postfix_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.show();

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

    private void showNoSelectionToolbar() {

        if (mSelectionToolbar != null)
            mSelectionToolbar.fixToolbar();

        getToolbar().setTitle(R.string.sync_item_text);
        if (mMenu != null)
            mMenu.setGroupVisible(R.id.sync_menu_selection, false);

    }

    private void showSelectionToolbar(int selectionCount) {

        if (mSelectionToolbar != null)
            mSelectionToolbar.scrollToolbar(selectionCount);
        if (mMenu != null)
            mMenu.setGroupVisible(R.id.sync_menu_selection, true);

    }

    private int getSelectionCount() {

        if (mListView != null)
            return mListView.getCheckedItemPositions().size();
        else
            return -1;

    }

    private ArrayList<Document> getSelectedDocuments() {

        ArrayList<Document> documents = new ArrayList<>();

        if (mDocuments != null && mListView != null) {
            SparseBooleanArray checkedItems = mListView.getCheckedItemPositions();
            if (checkedItems != null) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    if (checkedItems.valueAt(i)) {
                        documents.add(mDocuments.get(checkedItems.keyAt(i)));
                    }
                }
            }
        }

        return documents;

    }

    private void deselectListViewItems() {

        if (mListView != null) {

            SparseBooleanArray checkedItems = mListView.getCheckedItemPositions();
            if (checkedItems != null) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    int pos = checkedItems.keyAt(i);
                    mListView.setItemChecked(pos, false);
                }
            }
        }

        showNoSelectionToolbar();

    }

    private void selectListViewItems() {

        if (mListView != null) {
            for (int i = 0; i < mListView.getCount(); i++)
                mListView.setItemChecked(i, true);
        }

        showSelectionToolbar(getSelectionCount());

    }

    /**
     * Shows a snackbar indicating that the we have no permission for file reading.
     */
    private void showNoPermissionSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_no_permissions);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    private void closeSnackbar() {

        if (mSnackbar != null && mSnackbar.isShown())
                mSnackbar.dismiss();

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


    /**
     * Handles broadcast intents which inform about the upload progress:
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            boolean error = intent.getBooleanExtra(UPLOAD_ERROR_ID, false);

            if (error) {

                showUploadErrorDialog();
                initAdapter();
//                displayUploadActive(false);

                // update the selection display:
//                onSelectionChange();


                return;
            }

            boolean offlineError = intent.getBooleanExtra(UPLOAD_OFFLINE_ERROR_ID, false);

            if (offlineError) {

                showNotOnlineSnackbar();


                // update the selection display:
//                onSelectionChange();


                return;
            }

            boolean finished = intent.getBooleanExtra(UPLOAD_FINISHED_ID, false);

            if (finished) {

                initAdapter();
                showUploadFinishedSnackbar();

            }

        }
    };


}
