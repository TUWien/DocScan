package at.ac.tuwien.caa.docscan.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.ActivityUtils;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.SyncUtils;
import at.ac.tuwien.caa.docscan.ui.pdf.PdfActivity;
import at.ac.tuwien.caa.docscan.ui.settings.PreferenceActivity;
import at.ac.tuwien.caa.docscan.ui.syncui.UploadActivity;

import static at.ac.tuwien.caa.docscan.ui.NavigationDrawer.NavigationItemEnum.ACCOUNT_EDIT;

/**
 * This file contains parts of this source file:
 * https://github.com/google/iosched/blob/e8c61e7e23f74aa6786696dad22e5136b423a334/android/src/main/java/com/google/samples/apps/iosched/navigation/AppNavigationViewAsDrawerImpl.java
 * Created by fabian on 30.11.2016.
 *
 */
public class NavigationDrawer implements NavigationView.OnNavigationItemSelectedListener {

    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private NavigationItemEnum mSelfItem;
    private Handler mHandler;
    private ArrayList<NavigationItemEnum> mAccountItems;
    private static final int NAVDRAWER_LAUNCH_DELAY = 250; // Delay to launch nav drawer item, to allow close animation to play
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 50; // Fade in and fade out durations for the main content when switching between different Activities of the app through the Nav Drawer
    private boolean mAccountGroupVisible = false;

    private static final String CLASS_NAME = "NavigationDrawer";

    public NavigationDrawer(Activity activity, NavigationItemEnum selfItem) {

        mActivity = activity;
        mSelfItem = selfItem;

        initAccountItems();
        setupNavigationDrawer();
        setupDrawerHeader();

    }

    private void initAccountItems() {
        mAccountItems = new ArrayList<>();
        mAccountItems.add(ACCOUNT_EDIT);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        NavigationItemEnum item = NavigationItemEnum.getById(menuItem.getItemId());
        onNavDrawerItemClicked(item);
        return true;
    }

    public void showNavigation() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    private void setupNavigationDrawer() {

        mDrawerLayout = mActivity.findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // Rotate the button to its initial state:
                View headerLayout = mNavigationView.getHeaderView(0);
                ImageButton button = (ImageButton) headerLayout.findViewById(R.id.navigation_view_header_account_setting);
                button.setRotation(0);
                // Hide the account items:
                setAccountGroupVisible(false);
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(drawerToggle);

        mNavigationView = mActivity.findViewById(R.id.left_drawer);
        mNavigationView.setNavigationItemSelectedListener(this);

        createNavDrawerItems(NavigationItemEnum.values());

        mHandler = new Handler();

    }

    public void setupDrawerHeader() {


        View headerLayout = mNavigationView.getHeaderView(0);
        TextView userTextView = headerLayout.findViewById(R.id.navigation_view_header_user_textview);
        TextView connectionTextView = headerLayout.findViewById(R.id.navigation_view_header_sync_textview);
        ImageView userImageView = headerLayout.findViewById(R.id.navigation_view_header_user_image_view);

        // Set up the account name field:
        if (User.getInstance().isLoggedIn()) {
            // The user is logged in, show the name:
            userTextView.setText(User.getInstance().getFirstName() + " " + User.getInstance().getLastName());

//            Show the connection type:
            String cloudText = SyncUtils.getConnectionText(mActivity, User.getInstance().getConnection());
            connectionTextView.setText(cloudText);

//            set the user image:
            showUserImage(userImageView);

        }

        // The user is not logged in, but was logged in some time before, show the name:
        else if (UserHandler.loadUserNames(mActivity)) {
            userTextView.setText(User.getInstance().getFirstName() + " " + User.getInstance().getLastName());
            connectionTextView.setText(mActivity.getResources().getText(R.string.sync_not_connected_text));
        }
        else {
            userTextView.setText(mActivity.getResources().getText(R.string.account_not_logged_in));
            connectionTextView.setText("");
        }

        // Add a callback to the account settings layout:
        final ImageButton button = headerLayout.findViewById(R.id.navigation_view_header_account_setting);
        RelativeLayout layout = headerLayout.findViewById(R.id.account_layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAccountGroupVisible) {
                    setAccountGroupVisible(true);
                    button.setRotation(180);
                }
                else {
                    setAccountGroupVisible(false);
                    button.setRotation(0);
                }
            }
        });

    }

    private void showUserImage(ImageView userImageView) {

//        If the activity is not active anymore, do nothing, otherwise Glide will raise an
//        IllegalArgumentException

        try {

            if (mActivity == null || mActivity.isFinishing())
                return;

            switch (User.getInstance().getConnection()) {
                case User.SYNC_DROPBOX:
                    String photoUrl = User.getInstance().getPhotoUrl();
                    if (photoUrl != null) {
                        GlideApp.with(mActivity)
                                .load(photoUrl)
                                //                        Make the image view circular:
                                .apply(RequestOptions.circleCropTransform())
                                .into(userImageView);
                    }
                    break;
                case User.SYNC_TRANSKRIBUS:
                    GlideApp.with(mActivity)
                            .load(R.drawable.transkribus)
                            //                        Make the image view circular:
                            .apply(RequestOptions.circleCropTransform())
                            .into(userImageView);
                    break;
            }
        }
        catch (Exception e) {
            Helper.crashlyticsLog(CLASS_NAME, "showUserImage", e.toString());
        }
    }

    private void onNavDrawerItemClicked(final NavigationItemEnum item) {

        if (item == mSelfItem) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

//                // Launch the target Activity after a short delay, to allow the close animation to play
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                itemSelected(item);
//            }
//        });

        // Launch the target Activity after a short delay, to allow the close animation to play
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                itemSelected(item);
            }
        }, NAVDRAWER_LAUNCH_DELAY);

        // Change the active item on the list so the user can see the item changed
//        TODO: uncomment this:
//        setSelectedNavDrawerItem(item);
        // Fade out the main content
//        View mainContent = mActivity.findViewById(R.id.main_frame_layout);
//        if (mainContent != null) {
//            mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
//        }


        // The navigation menu is accessible from some screens, via swiping, without a drawer
        // layout, eg MapActivity when accessed from SessionDetailsActivity.
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

    }

    public void itemSelected(final NavigationItemEnum item) {
        switch (item) {
//            case SIGN_IN:
//                mLoginStateListener.onSignInOrCreateAccount();
//                break;
            case HELP:
                openHelpPDF();
                break;

            default:
                if (item.getClassToLaunch() != null) {
                    ActivityUtils.createBackStack(mActivity,
                            new Intent(mActivity, item.getClassToLaunch()));
                    if (item.finishCurrentActivity()) {
                        mActivity.finish();
                    }
                }
                break;
        }
    }

    private void openHelpPDF() {

        String pdfUrl = "https://transkribus.eu/wiki/images/e/ed/How_to_use_DocScan_and_ScanTent.pdf";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(pdfUrl));
        try {
            mActivity.startActivity(browserIntent);
        }
        catch (ActivityNotFoundException e) {
            Crashlytics.logException(e);
            Helper.showActivityNotFoundAlert(mActivity.getApplicationContext());
        }

    }

//    private void showNotOnlineAlert(Context context) {
//
//        new AlertDialog.Builder(context)
//                .setTitle(R.string.navigation_offline_text)
//                .setPositiveButton("OK", null)
//                .setMessage(R.string.navigation_offline_message)
//                .create()
//                .show();
//
//    }


    private void setAccountGroupVisible(boolean isVisible) {

        mAccountGroupVisible = isVisible;
        showHideNavigationGroups();

    }

    private void showHideNavigationGroups() {

        if (mNavigationView != null) {

            Menu menu = mNavigationView.getMenu();
            if (menu != null) {
                menu.setGroupVisible(R.id.navigation_main_items_group, !mAccountGroupVisible);
                menu.setGroupVisible(R.id.navigation_settings_items_group, !mAccountGroupVisible);
                menu.setGroupVisible(R.id.account_group, mAccountGroupVisible);

                // Show the logout button only if the user is logged in (and the menu group is visible):
                if (mAccountGroupVisible) {
                    MenuItem item = menu.findItem(R.id.account_logout_item);
                    if (item != null) {
                        item.setVisible(User.getInstance().isLoggedIn());
                    }
                }
            }
        }

    }

    private void createNavDrawerItems(NavigationItemEnum[] items) {

        if (mNavigationView != null) {
            Menu menu = mNavigationView.getMenu();
            for (int i = 0; i < items.length; i++) {
                MenuItem item = menu.findItem(items[i].getId());
                if (item != null) {
                    item.setVisible(true);
                    item.setIcon(items[i].getIconResource());
                    item.setTitle(items[i].getTitleResource());
                } else {
                    Log.d(getClass().getName(), "Menu Item for navigation item with title " +
                            (items[i].getTitleResource() != 0 ? mActivity.getResources().getString(
                                    items[i].getTitleResource()) : "") + "not found");
                }
            }

            mNavigationView.setNavigationItemSelectedListener(this);

            showHideNavigationGroups();

//            menu.setGroupVisible(R.id.navigation_main_items_group, !mAccountGroupVisible);
//            menu.setGroupVisible(R.id.navigation_settings_items_group, !mAccountGroupVisible);
//            menu.setGroupVisible(R.id.account_group, mAccountGroupVisible);
        }
    }
    /**
     * Taken from: https://github.com/google/iosched
     * List of all possible navigation items.
     * Howto remove menu items: Comment the item below and change the return value in the
     * corresponding getSelfNavDrawerItem() function.
     */
    public enum NavigationItemEnum {


        CAMERA(R.id.camera_item, R.string.camera_item_text,
                R.drawable.ic_camera_alt_black_24dp, CameraActivity.class),
//        LOGIN(R.id.login_item, R.string.login_item_text,
//                R.drawable.ic_arrow_forward_black_24dp, TranskribusLoginActivity.class),
        ABOUT(R.id.about_item, R.string.about_item_text,
                R.drawable.ic_info_black_24dp, AboutActivity.class),
        SHARE_LOG(R.id.share_log_item, R.string.share_log_item_text,
                R.drawable.ic_share_black_24dp, LogActivity.class),
        ACCOUNT_EDIT(R.id.account_edit_item, R.string.account_edit_text,
                R.drawable.ic_account_box_black_24dp, AccountActivity.class),
        ACCOUNT_LOGOUT(R.id.account_logout_item, R.string.account_logout,
                R.drawable.ic_remove_circle_outline_black_24dp, LogoutActivity.class),
        SETTINGS(R.id.settings_item, R.string.settings_item_text,
                R.drawable.ic_settings_black_24dp, PreferenceActivity.class),

        UPLOAD(R.id.sync_item, R.string.upload_item_text,
                R.drawable.ic_cloud_upload_black_24dp, UploadActivity.class),
        HELP(R.id.help_item, R.string.help_item_text,
                R.drawable.ic_help_black_24dp, null),
        PDF(R.id.pdf_item, R.string.pdf_item_text,
                R.drawable.ic_baseline_picture_as_pdf_24px, PdfActivity.class),
//        REST_TEST(R.id.rest_item, R.string.rest_item_text,
//                R.drawable.ic_weekend_black_24dp, RestTestActivity.class),
        INVALID(-1, 0, 0, null);

        private int id;

        private int titleResource;

        private int iconResource;

        private Class classToLaunch;

        private boolean finishCurrentActivity;

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch) {
            this(id, titleResource, iconResource, classToLaunch, false);
        }

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch,
                           boolean finishCurrentActivity) {
            this.id = id;
            this.titleResource = titleResource;
            this.iconResource = iconResource;
            this.classToLaunch = classToLaunch;
            this.finishCurrentActivity = finishCurrentActivity;
        }

        public int getId() {
            return id;
        }

        public int getTitleResource() {
            return titleResource;
        }

        public int getIconResource() {
            return iconResource;
        }

        public Class getClassToLaunch() {
            return classToLaunch;
        }

        public boolean finishCurrentActivity() {
            return finishCurrentActivity;
        }

        public static NavigationItemEnum getById(int id) {
            NavigationItemEnum[] values = NavigationItemEnum.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getId() == id) {
                    return values[i];
                }
            }
            return INVALID;
        }

    }
}
