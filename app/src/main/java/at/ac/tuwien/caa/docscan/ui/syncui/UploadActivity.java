package at.ac.tuwien.caa.docscan.ui.syncui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.LoginActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

import static at.ac.tuwien.caa.docscan.ui.LoginActivity.PARENT_ACTIVITY_NAME;

/**
 * Created by fabian on 4/5/2018.
 */

public class UploadActivity extends BaseNavigationActivity implements DocumentAdapter.DocumentAdapterCallback {

    private Context mContext;
    private ListView mListView;
    private DocumentAdapter mAdapter;
    private SelectionToolbar mSelectionToolbar;
    private List<Document> mDocuments;
    private Snackbar mSnackbar;
    private Menu mMenu;

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

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
        SyncInfo.readFromDisk(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            // Sets the adapter, note: This fills the list.
            initAdapter();
        }

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

            mDocuments = Helper.getDocuments(getResources().getString(R.string.app_name));
            mAdapter = new DocumentAdapter(mContext, R.layout.rowlayout, mDocuments);

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
        return NavigationDrawer.NavigationItemEnum.SYNC_2;
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

//        mAdapter.selectAllItems();


    }

    /**
     * Called after the MenuItem for delete selected is clicked.
     * @param item
     */
    public void deleteSelectedItems(MenuItem item) {

//        showDeleteConfirmationDialog();

    }

    /**
     * Called after the MenuItem for upload selected is clicked.
     * @param item
     */
    public void startUpload(MenuItem item) {

//        startUpload();

    }

    private void showNoSelectionToolbar() {

        mSelectionToolbar.fixToolbar();

        getToolbar().setTitle(R.string.sync_item_text);
        mMenu.setGroupVisible(R.id.sync_menu_selection, false);

    }

    private void showSelectionToolbar(int selectionCount) {

        mSelectionToolbar.scrollToolbar(selectionCount);
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
                    boolean val = checkedItems.valueAt(i);
                    mListView.setItemChecked(pos, false);
                }
            }
        }
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

}
