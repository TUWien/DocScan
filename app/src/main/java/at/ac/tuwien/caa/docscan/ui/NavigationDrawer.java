package at.ac.tuwien.caa.docscan.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.ActivityUtils;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * This file contains parts of this source file:
 * https://github.com/google/iosched/blob/e8c61e7e23f74aa6786696dad22e5136b423a334/android/src/main/java/com/google/samples/apps/iosched/navigation/AppNavigationViewAsDrawerImpl.java
 * Created by fabian on 30.11.2016.
 */
public class NavigationDrawer implements NavigationView.OnNavigationItemSelectedListener {

    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private NavigationItemEnum mSelfItem;
    private Handler mHandler;
    private static final int NAVDRAWER_LAUNCH_DELAY = 250; // Delay to launch nav drawer item, to allow close animation to play
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 50; // Fade in and fade out durations for the main content when switching between different Activities of the app through the Nav Drawer

    public NavigationDrawer(Activity activity, NavigationItemEnum selfItem) {

        mActivity = activity;
        mSelfItem = selfItem;

        setupNavigationDrawer();
        setupDrawerHeader();

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

        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(drawerToggle);

        mNavigationView = (NavigationView) mActivity.findViewById(R.id.left_drawer);
        mNavigationView.setNavigationItemSelectedListener(this);

        createNavDrawerItems(NavigationItemEnum.values());

        mHandler = new Handler();

    }

    public void setupDrawerHeader() {


        View headerLayout = mNavigationView.getHeaderView(0);
        TextView tv = (TextView) headerLayout.findViewById(R.id.navigation_view_header_user_textview);

        if (User.getInstance().isLoggedIn())
            tv.setText(User.getInstance().getFirstName() + " " + User.getInstance().getLastName());
        else
            tv.setText(mActivity.getResources().getText(R.string.app_name));

    }

    private void onNavDrawerItemClicked(final NavigationItemEnum item) {

        if (item == mSelfItem) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

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


    private void createNavDrawerItems(NavigationItemEnum[] items) {
        if (mNavigationView != null) {
            Menu menu = mNavigationView.getMenu();
            for (int i = 0; i < items.length; i++) {
                MenuItem item = menu.findItem(items[i].getId());
                if (item != null) {
                    // TODO: this removes the login item and the share log item temporary:
//                    if (item.getItemId() == R.id.login_item || item.getItemId() == R.id.share_log_item)
//                        continue;

                    if (item.getItemId() == R.id.login_item)
                        continue;
//                    if (item.getItemId() == R.id.share_log_item)
//                        continue;

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
        }
    }

    /**
     * Taken from: https://github.com/google/iosched
     * List of all possible navigation items.
     */
    public enum NavigationItemEnum {

//        CAMERA(R.id.camera_item, R.string.camera_item_text,
//                R.drawable.ic_camera, CameraActivity.class),
//        COLLECTIONS(R.id.collections_item, R.string.collections_item_text,
//                R.drawable.ic_camera, CollectionsActivity.class),
//        ABOUT(R.id.about_item, R.string.about_item_text,
//                R.drawable.ic_camera, AboutActivity.class),
//        SETTINGS(R.id.settings_item, R.string.setttings_item_text,
//                R.drawable.ic_camera, SettingsActivity.class),
//        INVALID(12, 0, 0, null);

        CAMERA(R.id.camera_item, R.string.camera_item_text,
                R.drawable.ic_camera_alt_black_24dp, CameraActivity.class),
        LOGIN(R.id.login_item, R.string.login_item_text,
                R.drawable.ic_arrow_forward_black_24dp, LoginActivity.class),
        ABOUT(R.id.about_item, R.string.about_item_text,
                R.drawable.ic_info_black_24dp, AboutActivity.class),
        SHARE_LOG(R.id.share_log_item, R.string.share_log_item_text,
                R.drawable.ic_share_black_24dp, LogActivity.class),
        INVALID(12, 0, 0, null);

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
