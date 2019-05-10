package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.android.volley.VolleyError;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.LogoutRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.DropboxUtils;

/**
 * Abstract class used for inherited activities whose properties are partially defined in
 * NavigationDrawer.NavigationItemEnum (for example title shown in the Actionbar).
 * Steps to create a child class:
 * 1: Create the XML layout.
 * 2: Implement the Activity. Take care to implement getSelfNavDrawerItem properly.
 * 3: Put the menu item in drawer_menu.xml
 * 4: Define the NavigationItemEnum (in NavigationDrawer)
 * 5: Define the Activity in AndroidManifest.xml
 */
public abstract class BaseNavigationActivity extends AppCompatActivity implements
        LoginRequest.LoginCallback, LogoutRequest.LogoutCallback {

    private static final String CLASS_NAME = "BaseNavigationActivity";

    protected NavigationDrawer mNavigationDrawer;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        The FLAG_KEEP_SCREEN_ON is set if the CameraActivity is in the series mode state. Turn it off, for the other activities:
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        mToolbar = getToolbar();
        setToolbarForNavigation();
    }

    public Toolbar getToolbar() {

        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
            if (mToolbar != null) {
//                mToolbar.setNavigationContentDescription(getResources().getString(R.string
//                        .navdrawer_description_a11y));
                setSupportActionBar(mToolbar);
                getSupportActionBar().setTitle(getSelfNavDrawerItem().getTitleResource());
            }
        }
        return mToolbar;

    }

    private void setToolbarForNavigation() {

        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_menu);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNavigationDrawer.showNavigation();
                }
            });
        }

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        mNavigationDrawer = new NavigationDrawer(this, getSelfNavDrawerItem());

        //        Check if a user has logged in in a previous session and use the credentials for login:
        // TODO: put this in the StartActivity (here it is just used for debugging purposes).

        if (User.getInstance().isLoggedIn())
            return;

        boolean isUserSaved = UserHandler.loadCredentials(this);
        if (isUserSaved && !User.getInstance().isAutoLogInDone()) {

            if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS) {
                RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);
                User.getInstance().setAutoLogInDone(true);
            }
            else if (User.getInstance().getConnection() == User.SYNC_DROPBOX) {
                if (UserHandler.loadDropboxToken(this)) {
                    DropboxUtils.getInstance().loginToDropbox(this, User.getInstance().getDropboxToken());
                    User.getInstance().setAutoLogInDone(true);
                }
            }
        }

    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses of
     * BaseNavigationActivity override this to indicate what nav drawer item corresponds to them Return
     * NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.INVALID;
    }

    @Override
    public void onLogin(User user) {

        User.getInstance().setLoggedIn(true);
        UserHandler.saveUserName(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNavigationDrawer.setupDrawerHeader();
            }
        });

    }

    @Override
    public void onLoginError() {

        User.getInstance().setLoggedIn(false);

    }

    @Override
    public void onLogout() {

        User.getInstance().setLoggedIn(false);
        mNavigationDrawer.setupDrawerHeader();

    }

    @Override
    public void handleRestError(RestRequest request, VolleyError error) {
        Log.d(CLASS_NAME, "handleRestError: " + error.toString());
    }






    }
