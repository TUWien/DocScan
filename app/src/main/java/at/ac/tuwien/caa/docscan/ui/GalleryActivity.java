package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;

/**
 * Created by fabian on 01.02.2018.
 */

public class GalleryActivity extends BaseNoNavigationActivity implements GalleryAdapter.GalleryAdapterCallback {

    private Toolbar mToolbar;
    private AppBarLayout.LayoutParams mAppBarLayoutParams;
    private CoordinatorLayout.LayoutParams mCoordinatorLayoutParams;
    private AppBarLayout mAppBarLayout;
    private Document mDocument;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        RecyclerView recyclerView = findViewById(R.id.gallery_images_recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

//        dummy document - start
//        String fileName = getIntent().getStringExtra("DOCUMENT_FILE_NAME");
        String fileName = "/storage/emulated/0/Pictures/DocScan/default";
        Document document = new Document();
        ArrayList<File> fileList = getFileList(fileName);
        ArrayList<Page> pages = filesToPages(fileList);
        document.setPages(pages);

        File file = new File(fileName);
        document.setTitle(file.getName());

        mDocument = document;
//        dummy document - end

//        initToolbarTitle(document.getTitle());
        initToolbarTitle(mDocument.getTitle());

        mToolbar = findViewById(R.id.main_toolbar);
        mAppBarLayout = findViewById(R.id.gallery_appbar);

        mAppBarLayoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        mCoordinatorLayoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();


//
//        CollapsingToolbarLayout layout = findViewById(R.id.gallery_toolbar_layout);
//        layout.setCollapsedTitleGravity();



//        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);

        GalleryAdapter adapter = new GalleryAdapter(this, document);
        adapter.setFileName(fileName);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_main_menu, menu);

//        mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
//        mMenu.setGroupVisible(R.id.gallery_menu_main, true);

        mMenu = menu;

        return true;

    }

    @Override
    public void onSelectionChange(int selectionCount) {

        // No selection - let the toolbar disappear, after scrolling down:
        if (selectionCount == 0) {

// This is based on post of Denny Weinberg:
// https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
            mAppBarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            mCoordinatorLayoutParams.setBehavior(new AppBarLayout.Behavior());
            mToolbar.setTitle(mDocument.getTitle());
            mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));


            mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
//            final Drawable navIcon = getResources().getDrawable(R.styleable.Toolbar_navigationIcon);


            mMenu.setGroupVisible(R.id.gallery_menu_selection, false);
            mMenu.setGroupVisible(R.id.gallery_menu_main, true);

        }
        // One or more items are selected - the toolbar stays:
        else {

            mAppBarLayoutParams.setScrollFlags(0);
            mCoordinatorLayoutParams.setBehavior(null);
            mToolbar.setTitle(Integer.toString(selectionCount));
            mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_clear_material);//add custom home buton

//            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mMenu.setGroupVisible(R.id.gallery_menu_main, false);
            mMenu.setGroupVisible(R.id.gallery_menu_selection, true);
            mToolbar.setBackgroundColor(getResources().getColor(R.color.colorAccent));

        }

        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);

    }


//    TODO: temporary helper methods copied from BaseDocumentAdapter. Replace them.

    private ArrayList<File> getFileList(String dir) {

        File[] files = getFiles(new File(dir));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<File> getFileList(Context context) {

        File mediaStorageDir = Helper.getMediaStorageDir(context.getResources().getString(R.string.app_name));

        if (mediaStorageDir == null)
            return null;

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] dirs = mediaStorageDir.listFiles(directoryFilter);
        File[] files = getFiles(dirs[27]);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    private File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }

    //   END of temporary helper methods copied from BaseDocumentAdapter. Replace them.



}
