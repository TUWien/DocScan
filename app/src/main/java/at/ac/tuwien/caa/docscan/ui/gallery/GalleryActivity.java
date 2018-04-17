package at.ac.tuwien.caa.docscan.ui.gallery;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.fivehundredpx.greedolayout.GreedoLayoutManager;
import com.fivehundredpx.greedolayout.GreedoSpacingItemDecoration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.GalleryAdapter;
import at.ac.tuwien.caa.docscan.gallery.GalleryLayoutManager;
import at.ac.tuwien.caa.docscan.gallery.InnerItemDecoration;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

import static android.support.v7.widget.DividerItemDecoration.HORIZONTAL;
import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

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


//    This is used to determine if some file changes (rotation or deletion) happened outside of the
//    GalleryActivity (i.e. in the ImageViewerFragment). If something changed we need to reload the
//    images in onResume.
    private static boolean mFileDeleted, mFileRotated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);

        //        dummy document - start
        mFileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));
//        if (mFileName == null)
//            mFileName = "/storage/emulated/0/Pictures/DocScan/Untitled document";

        mRecyclerView = findViewById(R.id.gallery_images_recyclerview);

        loadDocument();

        initAdapter();
        initToolbar();

    }

    @Override
    protected void onResume() {
        super.onResume();

//        Just reload the files if some file changes happened in the meantime:

        if (mFileDeleted) {
            loadDocument(); // get the files contained in the document:
            initAdapter();
        }

        if (mFileRotated) {
            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
        }


//        fixToolbar();

    }

    public static void resetFileManipulation() {
        mFileDeleted = false;
        mFileRotated = false;
    }

    public static void fileDeleted() {
        mFileDeleted = true;
    }

    public static void fileRotated() {
        mFileRotated = true;
    }

    private void initAdapter() {

        mAdapter = new GalleryAdapter(this, mDocument);
        mAdapter.setFileName(mFileName);
        mRecyclerView.setAdapter(mAdapter);

//        GreedoLayoutManager layoutManager = new GreedoLayoutManager(mAdapter);
        GalleryLayoutManager layoutManager = new GalleryLayoutManager(mAdapter);
        layoutManager.setMaxRowHeight(1200);

        int spacing = dpToPx(1, this);
        mRecyclerView.addItemDecoration(new InnerItemDecoration(spacing));

//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
//                HORIZONTAL);
//        DividerItemDecoration verticalDividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
//                VERTICAL);
//        mRecyclerView.addItemDecoration(dividerItemDecoration);
//        mRecyclerView.addItemDecoration(verticalDividerItemDecoration);

        mAdapter.setSizeCalculator(layoutManager.getSizeCalculator());



        mRecyclerView.setLayoutManager(layoutManager);

    }

    private static int dpToPx(float dp, Context context) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    private void loadDocument() {

        if (mFileName != null)
            mDocument = Helper.getDocument(mFileName);

    }

//    private void loadDocument() {
//
//        DocumentMetaData document = new DocumentMetaData();
//        ArrayList<File> fileList = getFileList(mFileName);
//        ArrayList<Page> pages = filesToPages(fileList);
//        document.setPages(pages);
//
//        File file = new File(mFileName);
//        document.setTitle(file.getName());
//
//        mDocument = document;
//
//    }

    private void initToolbar() {

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
                if (mAdapter.getSelectionCount() == 0) {
                    onBackPressed();
                    return true;
                }
                else {
                    mAdapter.deselectAllItems();
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

        mAdapter.selectAllItems();


     }

    public void deleteSelectedItems(MenuItem item) {

        showDeleteConfirmationDialog();

    }

    public void rotateSelectedItems(MenuItem item) {

        if (mDocument == null || mAdapter == null)
            return;

        int[] selections = mAdapter.getSelectionIndices();

        for (int i = 0; i < selections.length; i++) {
            try {
                Helper.rotateExif(mDocument.getPages().get(selections[i]).getFile());
//                mAdapter.notifyItemChanged(selections[i]);
//              We need to update ALL items because the layout of the neighboring items probably will change:
                mAdapter.notifyDataSetChanged();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void deleteSelections() {

        if (mDocument == null || mAdapter == null)
            return;

        int[] selections = mAdapter.getSelectionIndices();

        for (int i = selections.length - 1; i >= 0; i--) {
            mDocument.getPages().get(selections[i]).getFile().delete();
            mDocument.getPages().remove(selections[i]);
        }

        mAdapter.clearSelection();
        mAdapter.notifyDataSetChanged();

    }


    @Override
    public void onSelectionChange(int selectionCount) {

        // No selection - let the toolbar disappear, after scrolling down:
        if (selectionCount == 0)
            fixToolbar();
        // One or more items are selected - the toolbar stays:
        else
            scrollToolbar(selectionCount);

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

    private void showDeleteConfirmationDialog() {


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
