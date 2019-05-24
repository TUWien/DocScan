package at.ac.tuwien.caa.docscan.ui.syncui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;
import at.ac.tuwien.caa.docscan.sync.SyncUtils;
import at.ac.tuwien.caa.docscan.ui.AccountActivity;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.TranskribusLoginActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_ACTION;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_TYPE;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_FINISHED;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_FILE_NAME;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_PDF_PROCESS_FINISHED;
import static at.ac.tuwien.caa.docscan.sync.UploadService.UPLOAD_ERROR_ID;
import static at.ac.tuwien.caa.docscan.sync.UploadService.UPLOAD_FILE_DELETED_ERROR_ID;
import static at.ac.tuwien.caa.docscan.sync.UploadService.UPLOAD_FINISHED_ID;
import static at.ac.tuwien.caa.docscan.sync.UploadService.UPLOAD_INTEND_TYPE;
import static at.ac.tuwien.caa.docscan.sync.UploadService.INTENT_UPLOAD_ACTION;
import static at.ac.tuwien.caa.docscan.sync.UploadService.UPLOAD_OFFLINE_ERROR_ID;
import static at.ac.tuwien.caa.docscan.ui.TranskribusLoginActivity.PARENT_ACTIVITY_NAME;


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
    private boolean mIsUserCredentialsKnown = true;

    private static final String CLASS_NAME = "UploadActivity";
    private static final int PERMISSION_READ_WRITE_EXTERNAL_STORAGE = 0;

    private BroadcastReceiver mMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        Log.d(CLASS_NAME, "onCreate");

        mContext = this;

//        if (Helper.isOnline(this) && !User.getInstance().isLoggedIn()) {
////            If the user is not online show the corresponding activity and do nothing else:
//            showActivityNotLoggedIn();
//            return;
//        }


        setContentView(R.layout.activity_upload);

//        By default hiding toolbar is not working with ListView in a CoordinatorLayout.
//        Got this from: https://stackoverflow.com/questions/37450660/hiding-toolbar-with-listview-android
        mListView = findViewById(R.id.upload_list_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mListView.setNestedScrollingEnabled(true);
        }

        initToolbar();

        // Read the upload information:
//        SyncStorage.loadJSON(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_WRITE_EXTERNAL_STORAGE);
        } else {
            // Sets the adapter, note: This fills the list.
            initAdapter();
        }

        initFAB();

    }

    private void checkNetworkStatus() {

        //        The user has not logged in yet:
        if (!User.getInstance().isLoggedIn()) {
//            But has internet access:
            if (Helper.isOnline(this)) {
//                We know the user credentials so log in automatically:
                if (UserHandler.loadCredentials(this))
//                    So do the auto login:
                    RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);
                else {
//                    We show here nothing, just if the user clicks on the upload button
                    mIsUserCredentialsKnown = false;
//                    showNotLoggedInDialog();
                }

            }

        }
    }

    private void showNotLoggedInDialog() {

        if (mContext == null)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_not_logged_in_title)
                .setPositiveButton(R.string.sync_confirm_login_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
//                        Start the TranskribusLoginActivity
//                        Intent intent = new Intent(getApplicationContext(), TranskribusLoginActivity.class);
//                        intent.putExtra(PARENT_ACTIVITY_NAME, this.getClass().getName().toString());
                        Intent intent = new Intent(getApplicationContext(), AccountActivity.class);
                        intent.putExtra(PARENT_ACTIVITY_NAME, this.getClass().getName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.sync_cancel_login_button_text, null)
                .setCancelable(true)
                .setMessage(R.string.sync_not_logged_in_text);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        if (requestCode == REQUEST_LOGIN) {
//            if (resultCode == Activity.RESULT_CANCELED) {
//                showNotConnectedSnackbar();
//            }
//        }
//    }

    private void initFAB() {
        FloatingActionButton fab = findViewById(R.id.camera_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // close the activity
            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            initAdapter();

        // Register to receive messages.
        mMessageReceiver = getReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_UPLOAD_ACTION);
        filter.addAction(INTENT_IMAGE_PROCESS_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

//        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
//                new IntentFilter(INTENT_UPLOAD_ACTION));
//        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
//                new IntentFilter(INTENT_IMAGE_PROCESS_ACTION));

        showNoSelectionToolbar();

//        update the navigation drawer, because the user might have logged in in the meantime:
        mNavigationDrawer.setupDrawerHeader();

//        See if the user is not logged in but is online:
        checkNetworkStatus();

//        Just select the active document for the first time:
        selectActiveDocument();

    }

    private void selectActiveDocument() {

        if (getIntent().hasExtra("DONE")) {
            if (getIntent().getBooleanExtra("DONE", false))
                return;
        }
        String fileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));
//        We just want the selection for the first time the Activity is started, but not for
//        multiple resumes:
        getIntent().putExtra("DONE", true);



        if (fileName != null) {
            final Document document = DocumentStorage.getInstance(this).getDocument(fileName);
            if (document != null && mDocuments != null && mListView != null)
                mListView.post(new Runnable() {
                    @Override
                    public void run() {

                        int position = mDocuments.indexOf(document);
                        if (position != -1) {
                            mSelectionToolbar.fixToolbar();
                            // Open the selection toolbar and show that one element is selected:
                            showSelectionToolbar(1);
                            mListView.setItemChecked(position, true);
                            mListView.smoothScrollToPosition(position);

                        }
                    }
                });
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        Log.d(CLASS_NAME, "onPause: ");

        DocumentStorage.saveJSON(this);
        SyncStorage.saveJSON(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.d(CLASS_NAME, "unregisterReceiver: ");
        mMessageReceiver = null;

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

        if (requestCode == PERMISSION_READ_WRITE_EXTERNAL_STORAGE && isPermissionGiven) {
            // Sets the adapter, note: This fills the list.
            initAdapter();
        }

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

        Button loginButton = findViewById(R.id.sync_not_logged_in_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TranskribusLoginActivity.class);
                intent.putExtra(PARENT_ACTIVITY_NAME, this.getClass().getName());
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

            DocumentStorage.getInstance(this).updateStatus(this);
            ArrayList<Document> allDocuments = DocumentStorage.getInstance(this).getDocuments();
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

        if (areAllItemsSelected())
            deselectListViewItems();
        else
            selectListViewItems();

    }

    private boolean areAllItemsSelected() {

        return mListView.getCount() == getSelectedDocuments().size();

    }

    public void createPdfFromSelectedItem(MenuItem item) {

//        Check if the play services are installed first:
        if (!Helper.checkPlayServices(this))
            showNoPlayServicesDialog();
        else
            showOCRAlertDialog();

    }

    private void showNoPlayServicesDialog() {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.gallery_confirm_no_ocr_available_title)
                .setPositiveButton(R.string.dialog_yes_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createPdfFromSelectedItem(false);
                        deselectListViewItems();
                    }
                })
                .setNegativeButton(R.string.dialog_no_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setCancelable(true)
                .setMessage(R.string.gallery_confirm_no_ocr_available_text);

        final AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text),
//                new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                alertDialog.cancel();
//            }
//        });
        alertDialog.show();

    }

    private void showOCRAlertDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.gallery_confirm_ocr_title)
                .setPositiveButton(R.string.dialog_yes_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createPdfFromSelectedItem(true);
                        deselectListViewItems();
                    }
                })
                .setNegativeButton(R.string.dialog_no_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createPdfFromSelectedItem(false);
                        deselectListViewItems();
                    }
                })
                .setCancelable(true)
                .setMessage(R.string.gallery_confirm_ocr_text);

        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                    }
                });
        alertDialog.show();

    }

    private void createPdfFromSelectedItem(boolean withOCR) {

        if (mContext == null)
            return;

        final ArrayList<Document> documents = getSelectedDocuments();
        for (Document document : documents){
            if (withOCR) {
                ImageProcessor.createPdfWithOCR(document);
            } else {
                ImageProcessor.createPdf(document);
            }
        }

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

        for (Document document : documents)
            DocumentStorage.getInstance(this).getDocuments().remove(document);

//        update the UI:
        showDocumentsDeletedSnackbar(documents.size());
        initAdapter();
        deselectListViewItems();

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

        if (!mIsUserCredentialsKnown) {
            showNotLoggedInDialog();
            return;
        }

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

        ArrayList<String> uploadDirs = new ArrayList<>();
        for (Document document : documents) {
            // Just add the folder if all files contained are not uploaded:
            if (!document.isUploaded())
                uploadDirs.add(document.getTitle());

        }

        if (uploadDirs.isEmpty()) {
            showAlreadyUploadedSnackbar(documents.size());
        }
        else {

            if (Helper.isOnline(this))
                showUploadingSnackbar(documents.size()); // tell the user that the uploaded started
            else
                showOfflineSnackbar();

            startUpload(uploadDirs);

        }


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


    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    private void startUpload(ArrayList<String> uploadDirs) {

//        SyncInfo.getInstance().setUploadDirs(mSelectedDirs);

        SyncStorage.getInstance(this).addUploadDirs(uploadDirs);
        SyncUtils.startSyncJob(this, false);

    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private void showNotConnectedSnackbar() {

        String snackbarText =
                getResources().getString(R.string.sync_snackbar_disconnected_text);

        closeSnackbar();
        mSnackbar = Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG);
        mSnackbar.show();

    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private void showOfflineSnackbar() {

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
    private void showUploadingSnackbar(int selCnt) {

        String selText = selCnt + " " + Helper.getDocumentSingularPlural(this, selCnt);

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
    private void showAlreadyUploadedSnackbar(int selCnt) {

        String snackbarText = getResources().getString(R.string.sync_snackbar_already_uploaded_prefix_text);

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

        showSelectionToolbar(getSelectedDocuments().size());

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

    private void showFileDeletedErrorDialog() {

        if (mContext == null)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_file_deleted_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_file_deleted_text);

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
    private BroadcastReceiver getReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(CLASS_NAME, "onReceive: " + intent);

                if (intent == null || intent.getAction() == null || context == null)
                    return;

                if (intent.getAction().equals(INTENT_UPLOAD_ACTION)) {

                    //            update the list view:
                    initAdapter();
                    String intentMessage = intent.getStringExtra(UPLOAD_INTEND_TYPE);

                    switch (intentMessage) {
                        case UPLOAD_ERROR_ID:
                            showUploadErrorDialog();
                            break;
                        case UPLOAD_OFFLINE_ERROR_ID:
                            showOfflineSnackbar();
                            break;
                        case UPLOAD_FINISHED_ID:
                            showUploadFinishedSnackbar();
                            break;
                        case UPLOAD_FILE_DELETED_ERROR_ID:
                            showFileDeletedErrorDialog();
                            break;
                    }
                }
                else if (intent.getAction().equals(INTENT_IMAGE_PROCESS_ACTION)) {

                    int defValue = -1;
                    int cropType = intent.getIntExtra(INTENT_IMAGE_PROCESS_TYPE, defValue);

//                    we just handle cases where there is an operation finished:
                    if (cropType != defValue) {

                        if (cropType == INTENT_IMAGE_PROCESS_FINISHED) {

//                            find the corresponding document:
                            String fileName = intent.getStringExtra(INTENT_FILE_NAME);
                            if (fileName != null) {

                                if (mDocuments == null)
                                    return;

                                boolean isAdapterUpdateRequired = false;

                                for (Document document : mDocuments) {

                                    if (document.isCropped()) {
                                        boolean isCropped = Helper.areFilesCropped(document);
                                        if (!isCropped) {
                                            document.setIsCropped(false);
                                            isAdapterUpdateRequired = true;
                                        }
                                    }
                                if (isAdapterUpdateRequired)
//                                    initAdapter();
                                    mAdapter.notifyDataSetChanged();
                                }
                            }

                        } else if (cropType == INTENT_PDF_PROCESS_FINISHED){
                            View parentLayout = findViewById(android.R.id.content);
                            final String documentName = intent.getStringExtra(INTENT_FILE_NAME);
                            Snackbar.make(parentLayout, "Document created at: " + documentName, Snackbar.LENGTH_LONG    )
                                    .setAction("OPEN", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {

                                            Uri path = FileProvider.getUriForFile(getApplicationContext(), "at.ac.tuwien.caa.fileprovider", new File(documentName));
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            intent.setDataAndType(path, "application/pdf");
                                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            try {
                                                startActivity(intent);
                                            }
                                            catch (ActivityNotFoundException e) {
                                                Crashlytics.logException(e);
                                                Helper.showActivityNotFoundAlert(mContext);
                                            }
                                        }
                                    })
                                    .show();
                        }
                    }

                }
            }
        };

        return receiver;
    }


}
