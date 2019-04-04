package at.ac.tuwien.caa.docscan.ui.gallery;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.gallery.GalleryAdapter;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_ACTION;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_FILE_NAME;


/**
 * Created by fabian on 01.02.2018.
 */

public class GalleryActivity extends AppCompatActivity implements
        GalleryAdapter.GalleryAdapterCallback {

    private Toolbar mToolbar;
    private Document mDocument;
    private Menu mMenu;
    private GalleryAdapter mAdapter;

    private RecyclerView mRecyclerView;
    private String mFileName;
    private SelectionToolbar mSelectionToolbar;
    private BroadcastReceiver mMessageReceiver;

    private static final int PERMISSION_ROTATE = 0;
    private static final int PERMISSION_DELETE = 1;
    private static final int PERMISSION_CROP = 2;
    private static final String CLASS_NAME = "GalleryActivity";
    private static final int DOCUMENT_RENAMING = 0;


//    This is used to determine if some file changes (rotation or deletion) happened outside of the
//    GalleryActivity (i.e. in the ImageViewerFragment). If something changed we need to reload the
//    images in onResume.
    private static boolean sFileDeleted, sFileRotated, sFileCropped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(CLASS_NAME, "onCreate");

        setContentView(R.layout.activity_gallery);

        mFileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));

        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);

        loadDocument();
        if (mDocument != null) {
            initAdapter();
            initToolbar();
            checkFocus();
        }
        else
            Helper.crashlyticsLog(CLASS_NAME, "onCreate", "mDocument == null");
    }

    /**
     * Checks if the document is focused and shows a snackbar if not.
     */
    private void checkFocus() {

        if (mDocument != null && !Helper.isDocumentFocused(mDocument)) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.gallery_images_recyclerview),
                    R.string.gallery_unfocused_snackbar_text, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.gallery_unfocused_info,
                    new InfoButtonListener(this));
            snackbar.show();
        }

    }

    public class InfoButtonListener implements View.OnClickListener{

        AlertDialog mAlertDialog;

        public InfoButtonListener(Context context) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            // set dialog message
            alertDialogBuilder
                    .setTitle(R.string.gallery_unfocused_title)
                    .setPositiveButton("OK", null)
                    .setMessage(R.string.gallery_unfocused_text);
            // create alert dialog
            mAlertDialog = alertDialogBuilder.create();

        }

        @Override
        public void onClick(View v) {

            mAlertDialog.show();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(CLASS_NAME, "onResume");

//        Just reload the files if some file changes happened in the meantime:

        if (sFileDeleted) {
            Log.d(CLASS_NAME, "onResume: sFileDeleted");
            loadDocument(); // get the files contained in the document:
            initAdapter();
            if (mAdapter != null) {
                Log.d(CLASS_NAME, "onResume: notifyDataSetChanged");
                mAdapter.notifyDataSetChanged();
            }
            else
                Helper.crashlyticsLog(CLASS_NAME, "onResume", "mAdapater == null");
        }

        if (sFileRotated || sFileCropped) {
            Log.d(CLASS_NAME, "onResume: sFileRotated || sFileCropped");
            if (mAdapter != null) {
                Log.d(CLASS_NAME, "onResume: notifyDataSetChanged");
                mAdapter.notifyDataSetChanged();
            }
            else {
                loadDocument();
                initAdapter();
            }

        }

        mMessageReceiver = getReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_IMAGE_PROCESS_ACTION));

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(CLASS_NAME, "onActivityResult");

        if (requestCode == DOCUMENT_RENAMING) {

            Log.d(CLASS_NAME, "onActivityResult: DOCUMENT_RENAMING");

            if (resultCode == RESULT_OK) {
                Log.d(CLASS_NAME, "onActivityResult: RESULT_OK");
                if (data.getData() != null) {
                    mFileName = data.getData().toString();
                    Log.d(CLASS_NAME, "onActivityResult: mFileName: " + mFileName);
                    loadDocument();
                    if (mDocument != null) {
                        Log.d(CLASS_NAME, "onActivityResult: mDocument != null");
                        initAdapter();
                        initToolbar();
                    }
                }
                else
                    Helper.crashlyticsLog(CLASS_NAME, "onActivityResult",
                            "data.getData() == null");

            }
        }
    }


    @Override
    public void onPause() {

        super.onPause();

        DocumentStorage.saveJSON(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        mMessageReceiver = null;

    }


    private BroadcastReceiver getReceiver() {

        return new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (mAdapter != null) {

                    String fileName = intent.getStringExtra(INTENT_FILE_NAME);

                    int idx = 0;
                    if (mDocument != null && mDocument.getPages() != null) {
                        for (Page page : mDocument.getPages()) {
                            if (page.getFile().getAbsolutePath().compareTo(fileName) == 0) {
                                mAdapter.notifyItemChanged(idx);
                                break;
                            }
                            idx++;
                        }
                    }

//                    mAdapter.deselectAllItems();
                }
                else
                    Helper.crashlyticsLog(CLASS_NAME, "getReceiver",
                            "mAdapter == null");


            }
        };

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {


        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);

        switch (requestCode) {

            case PERMISSION_ROTATE:
                if (isPermissionGiven)
                    rotateSelectedItems();
                break;
            case PERMISSION_DELETE:
                if (isPermissionGiven)
                    deleteSelectedItems();
                break;
            case PERMISSION_CROP:
                if (isPermissionGiven)
                    showOverwriteImageAlert();
                break;

        }
    }

    public static void resetFileManipulation() {
        sFileDeleted = false;
        sFileRotated = false;
        sFileCropped = false;
    }

    public static void fileDeleted() {
        sFileDeleted = true;
    }

    public static void fileCropped() {
        sFileCropped = true;
    }

    public static void fileRotated() {
        sFileRotated = true;
    }

    private void initAdapter() {

        if (mDocument == null) {
            Helper.crashlyticsLog(CLASS_NAME, "initAdapter", "mDocument == null");
            return;
        }

        mAdapter = new GalleryAdapter(this, mDocument);
        mAdapter.setFileName(mFileName);
        mRecyclerView.setAdapter(mAdapter);

        int columnCount = 2;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            columnCount = 4;

        GridLayoutManager layoutManager = new GridLayoutManager(this, columnCount);
        mAdapter.setColumnCount(columnCount);

        mRecyclerView.setLayoutManager(layoutManager);


    }

    private void loadDocument() {

//        if (mFileName != null)
//            mDocument = Helper.getDocument(mFileName);

        if (mFileName != null) {
            mDocument = DocumentStorage.getInstance(this).getDocument(mFileName);
            if (mDocument == null)
                mDocument = DocumentStorage.getInstance(this).getActiveDocument();
        }
        else
            mDocument = DocumentStorage.getInstance(this).getActiveDocument();

    }


    private void initToolbar() {

        if (mDocument == null) {
            Helper.crashlyticsLog(CLASS_NAME, "initToolbar", "mDocument == null");
            return;
        }

        mToolbar = findViewById(R.id.main_toolbar);
        mToolbar.setTitle(mDocument.getTitle());

        AppBarLayout appBarLayout = findViewById(R.id.gallery_appbar);

//        Enable back navigation in action bar:
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        else
            Helper.crashlyticsLog(CLASS_NAME, "initToolbar", "getSupportActionBar == null");


//        Note initialize SelectionToolbar just after setting setDisplayHomeAsUpEnabled, because
//        SelectionToolbar needs a navigation icon (i.e. back button):
        mSelectionToolbar = new SelectionToolbar(this, mToolbar, appBarLayout);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                // did the user cancel the renaming?
                if (mAdapter.getSelectionCount() == 0) {
                    onBackPressed();
                    return true;
                }
                // did the user cancel the selection?
                else {
                    mAdapter.deselectAllItems();
//                    force a redraw of the checkboxes:
                    mAdapter.notifyDataSetChanged();
                    return true;
                }
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_menu, menu);

        mMenu = menu;
        return true;

    }

    public void selectAllItems(MenuItem item) {

        if (areAllItemsSelected())
            deselectAllItems();
        else
            selectAllItems();

     }

    private boolean areAllItemsSelected() {

        return mAdapter.getItemCount() == mAdapter.getSelectionCount();

    }

    private void selectAllItems() {

        mAdapter.selectAllItems();

    }

    private void deselectAllItems() {

        mAdapter.deselectAllItems();

    }

    public void cropSelectedItems(MenuItem item) {

        // Check if we have the permission to rotate images:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_ROTATE);
        } else
            showOverwriteImageAlert();


    }

    public void editDocument(MenuItem item) {

        if (mDocument != null && mDocument.getTitle() != null) {
            Intent intent = new Intent(getApplicationContext(), EditDocumentActivity.class);
            intent.putExtra(EditDocumentActivity.DOCUMENT_NAME_KEY, mDocument.getTitle());
            startActivityForResult(intent, DOCUMENT_RENAMING);
        }
    }


    public void deleteSelectedItems(MenuItem item) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_DELETE);
        } else
            deleteSelectedItems();

    }

    public void rotateSelectedItems(MenuItem item) {

        // Check if we have the permission to rotate images:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CROP);
        } else
            rotateSelectedItems();

    }


    private void showOverwriteImageAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.gallery_confirm_overwrite_title)
                .setPositiveButton(R.string.dialog_ok_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cropSelectedItems();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
                .setMessage(R.string.gallery_confirm_overwrite_text);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    private void cropSelectedItems() {

        if (mDocument == null || mAdapter == null || mDocument.getPages() == null)
            return;

        int[] selectionIdx = mAdapter.getSelectionIndices();
        ArrayList<Integer> uncroppedIdx = new ArrayList<>();

//        Check if some selected files are already cropped, deselect these:
        for (int aSelectionIdx1 : selectionIdx) {
            String fileName = mDocument.getPages().get(aSelectionIdx1).getFile().getAbsolutePath();
            if (!PageDetector.isCropped(fileName))
                uncroppedIdx.add(aSelectionIdx1);
        }

//        if (uncroppedIdx.size() < selectionIdx.length)
//            mAdapter
////        TODO: show an error message if selectionIdx.size != uncroppedIdx.length

        for (int aSelectionIdx : selectionIdx)
            ImageProcessor.mapFile(mDocument.getPages().get(aSelectionIdx).getFile());


//        mAdapter.notifyDataSetChanged();
        deselectAllItems();


    }

    private void rotateSelectedItems() {

        if (mDocument == null || mAdapter == null || mDocument.getPages() == null) {
            Helper.crashlyticsLog(CLASS_NAME, "rotateSelectedItems", "null pointer");
            return;
        }

        int[] selections = mAdapter.getSelectionIndices();

        for (int selection : selections)
            ImageProcessor.rotateFile(mDocument.getPages().get(selection).getFile());

    }

    private void deleteSelections() {

        if (mDocument == null || mAdapter == null || mDocument.getPages() == null) {
            Helper.crashlyticsLog(CLASS_NAME, "rotateSelectedItems",
                    "mDocument == null || mAdapter == null || mDocument.getPages() == null || " +
                    "                mAdapter == null");
            return;
        }

        int[] selections = mAdapter.getSelectionIndices();

//        Log.d(CLASS_NAME, "deleteSelections: selections.length: " + selections.length);
//        for (int i = 0; i < mDocument.getPages().size(); i++) {
//            Log.d(CLASS_NAME, "deleteSelections: listing index: " + i + " file: " + mDocument.getPages().get(i).getFile().getName());
//        }


        for (int i = selections.length - 1; i >= 0; i--) {

            int selIdx = selections[i];

            Page page = mDocument.getPages().remove(selIdx);
            String fileName = page.getFile().getAbsolutePath();
//            Log.d(CLASS_NAME, "deleteSelections: deleting index: " + selIdx + " filename: " + fileName);

            boolean isFileDeleted = new File(fileName).delete();
            if (!isFileDeleted)
                Helper.crashlyticsLog(CLASS_NAME, "deleteSelections",
                        "file not deleted");

            mAdapter.notifyItemRemoved(selIdx);
            mAdapter.notifyItemRangeChanged(selIdx, mDocument.getPages().size());

        }

        mAdapter.deselectAllItems();


    }


    @Override
    public void onSelectionChange(int selectionCount) {

        if (mAdapter == null) {
            Helper.crashlyticsLog(CLASS_NAME, "onSelectionChange",
                    "mAdapter == null");
            return;
        }

        // No selection - let the toolbar disappear, after scrolling down:
        if (selectionCount == 0) {
            mAdapter.setSelectionMode(false);
            fixToolbar();
        }
        // One or more items are selected - the toolbar stays:
        else {
            mAdapter.setSelectionMode(true);
            scrollToolbar(selectionCount);
        }

    }


    private void scrollToolbar(int selectionCount) {

        if (mSelectionToolbar == null || mMenu == null) {
            Helper.crashlyticsLog(CLASS_NAME, "scrollToolbar", "null pointer");
            return;
        }

        mSelectionToolbar.scrollToolbar(selectionCount);

        mMenu.setGroupVisible(R.id.gallery_menu_main, false);
        mMenu.setGroupVisible(R.id.gallery_menu_selection, true);

    }

    private void fixToolbar() {


        if (mSelectionToolbar != null)
            mSelectionToolbar.fixToolbar();
        else
            Helper.crashlyticsLog(CLASS_NAME, "fixToolbar",
                    "mSelectionToolbar == null");

        //            Set the action bar title:
        if (mToolbar != null && mDocument != null)
            mToolbar.setTitle(mDocument.getTitle());
        else
            Helper.crashlyticsLog(CLASS_NAME, "fixToolbar",
                    "mToolbar or mDocument == null");

        if (mMenu != null) {
            mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
            mMenu.setGroupVisible(R.id.gallery_menu_main, true);
        }
        else
            Helper.crashlyticsLog(CLASS_NAME, "fixToolbar", "mMenu == null");

    }

    private void deleteSelectedItems() {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String title = getResources().getString(R.string.gallery_confirm_delete_title_prefix);
        if (mAdapter != null) {
            title += " " + mAdapter.getSelectionCount();

            if (mAdapter.getSelectionCount() == 1)
                title += " " + getResources().getString(R.string.gallery_confirm_delete_title_single_postfix);
            else
                title += " " + getResources().getString(R.string.gallery_confirm_delete_title_multiple_postfix);
        }

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.gallery_confirm_delete_text)
                .setTitle(title)
                .setPositiveButton(R.string.gallery_confirm_delete_confirm_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        deleteSelections();
                    }
                })
                .setNegativeButton(R.string.gallery_confirm_delete_cancel_button_text, null)
                .setCancelable(true);
//                .setMessage(deleteText);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


}
