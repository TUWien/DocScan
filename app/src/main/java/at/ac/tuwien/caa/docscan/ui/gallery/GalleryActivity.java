package at.ac.tuwien.caa.docscan.ui.gallery;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

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
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_TYPE;
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

        setContentView(R.layout.activity_gallery);

        mFileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));

        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);

        loadDocument();
        if (mDocument != null) {
            initAdapter();
            initToolbar();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

//        Just reload the files if some file changes happened in the meantime:

        if (sFileDeleted) {
            loadDocument(); // get the files contained in the document:
            initAdapter();
            mAdapter.notifyDataSetChanged();
        }

        if (sFileRotated || sFileCropped) {
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
        }

//        Set the document title in onResume, because it can be edited in EditDocumentActivity:
//        mToolbar.setTitle(mDocument.getTitle());

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "PROGRESS_INTENT_NAME".
        mMessageReceiver = getReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_IMAGE_PROCESS_ACTION));

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DOCUMENT_RENAMING) {
            if (resultCode == RESULT_OK) {
                mFileName = data.getData().toString();
                loadDocument();
                if (mDocument != null) {
                    initAdapter();
                    initToolbar();
                }
            }
//            do nothing if resultCode == RESULT_CANCELED
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

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(CLASS_NAME, "onReceive: " + intent.getIntExtra(INTENT_IMAGE_PROCESS_TYPE, -1));

                if (mAdapter != null) {

                    String fileName = intent.getStringExtra(INTENT_FILE_NAME);

                    int idx = 0;
                    if (mDocument.getPages() != null) {
                        for (Page page : mDocument.getPages()) {
                            if (page.getFile().getAbsolutePath().compareTo(fileName) == 0) {
                                mAdapter.notifyItemChanged(idx);
                                break;
                            }
                            idx++;
                        }
                    }

                    mAdapter.deselectAllItems();
                }


            }
        };

        return receiver;

    }

//    /**
//     * Handles broadcast intents which inform about the upload progress:
//     */
//    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//
//
//        }
//    };

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

    private static int dpToPx(float dp, Context context) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    private void loadDocument() {

//        if (mFileName != null)
//            mDocument = Helper.getDocument(mFileName);

        if (mFileName != null) {
            mDocument = DocumentStorage.getInstance(this).getDocument(mFileName);
        }

    }


    private void initToolbar() {

        if (mDocument == null)
            return;

        mToolbar = findViewById(R.id.main_toolbar);
        mToolbar.setTitle(mDocument.getTitle());

        AppBarLayout appBarLayout = findViewById(R.id.gallery_appbar);

//        Enable back navigation in action bar:
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
//            startActivity(intent);
            startActivityForResult(intent, DOCUMENT_RENAMING);
        }


//        // Check if we have the permission to rotate images:
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // ask for permission:
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_ROTATE);
//        } else
//            renameDocument();

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
        for (int i = 0; i < selectionIdx.length; i++) {
            String fileName = mDocument.getPages().get(selectionIdx[i]).getFile().getAbsolutePath();
            if (!PageDetector.isCropped(fileName))
                uncroppedIdx.add(selectionIdx[i]);
        }

//        if (uncroppedIdx.size() < selectionIdx.length)
//            mAdapter
////        TODO: show an error message if selectionIdx.size != uncroppedIdx.length

        for (int i = 0; i < selectionIdx.length; i++) {
                ImageProcessor.mapFile(mDocument.getPages().get(selectionIdx[i]).getFile());
        }

//        mAdapter.notifyDataSetChanged();
        deselectAllItems();


    }

    private void rotateSelectedItems() {

        if (mDocument == null || mAdapter == null || mDocument.getPages() == null)
            return;

        int[] selections = mAdapter.getSelectionIndices();

        for (int i = 0; i < selections.length; i++) {
            ImageProcessor.rotateFile(mDocument.getPages().get(selections[i]).getFile());
        }
    }

    private void deleteSelections() {

        if (mDocument == null || mAdapter == null || mDocument.getPages() == null)
            return;

        int[] selections = mAdapter.getSelectionIndices();

        Log.d(CLASS_NAME, "deleteSelections: selections.length: " + selections.length);
        for (int i = 0; i < mDocument.getPages().size(); i++) {
            Log.d(CLASS_NAME, "deleteSelections: listing index: " + i + " file: " + mDocument.getPages().get(i).getFile().getName());
        }


        for (int i = selections.length - 1; i >= 0; i--) {

            int selIdx = selections[i];

            Page page = mDocument.getPages().remove(selIdx);
            String fileName = page.getFile().getAbsolutePath();
            Log.d(CLASS_NAME, "deleteSelections: deleting index: " + selIdx + " filename: " + fileName);
            new File(fileName).delete();

            mAdapter.notifyItemRemoved(selIdx);
            mAdapter.notifyItemRangeChanged(selIdx, mDocument.getPages().size());

        }

        mAdapter.deselectAllItems();


    }


    @Override
    public void onSelectionChange(int selectionCount) {

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

        mSelectionToolbar.scrollToolbar(selectionCount);

        mMenu.setGroupVisible(R.id.gallery_menu_main, false);
        mMenu.setGroupVisible(R.id.gallery_menu_selection, true);

    }

    private void fixToolbar() {


        if (mSelectionToolbar != null)
            mSelectionToolbar.fixToolbar();

        //            Set the action bar title:
        if (mToolbar != null)
            mToolbar.setTitle(mDocument.getTitle());
        if (mMenu != null) {
            mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
            mMenu.setGroupVisible(R.id.gallery_menu_main, true);
        }

    }

    private void deleteSelectedItems() {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String title = getResources().getString(R.string.gallery_confirm_delete_title_prefix);
        title += " " + mAdapter.getSelectionCount();
        if (mAdapter.getSelectionCount() == 1)
            title += " " + getResources().getString(R.string.gallery_confirm_delete_title_single_postfix);
        else
            title += " " + getResources().getString(R.string.gallery_confirm_delete_title_multiple_postfix);

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



//    private ArrayList<File> getFileList(String dir) {
//
//        File[] files = getFiles(new File(dir));
//
//        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));
//
//        return fileList;
//
//    }
//
//    private ArrayList<Page> filesToPages(ArrayList<File> files) {
//
//        ArrayList<Page> pages = new ArrayList<>(files.size());
//
//        int idx = 1;
//        for (File file : files) {
//            pages.add(new Page(file, Integer.toString(idx)));
//            idx++;
//        }
//
//        return pages;
//
//    }
//
//    private File[] getFiles(File dir) {
//
//        FileFilter filesFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return !file.isDirectory();
//            }
//        };
//        File[] files = dir.listFiles(filesFilter);
//        Arrays.sort(files);
//
//        return files;
//    }

    //   END of temporary helper methods copied from BaseDocumentAdapter. Replace them.



}
