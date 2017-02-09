package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

/**
 * Created by fabian on 30.11.2016.
 */
public abstract class BaseActivity extends AppCompatActivity implements LoginRequest.LoginCallback {

    private NavigationDrawer mNavigationDrawer;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // put login here

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
        boolean isUserSaved = UserHandler.loadCredentials(this);
        if (isUserSaved && !User.getInstance().isAutoLogInDone()) {
            RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);
            User.getInstance().setAutoLogInDone(true);
        }


    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses of
     * BaseActivity override this to indicate what nav drawer item corresponds to them Return
     * NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.INVALID;
    }

    @Override
    public void onLogin(User user) {

        mNavigationDrawer.setupDrawerHeader();

//        String welcomeText = getResources().getString(R.string.login_welcome_text) + " " + user.getFirstName();
//        Toast.makeText(this, welcomeText, Toast.LENGTH_SHORT).show();
//
////        Save the credentials:
//        UserHandler.saveCredentials(this);
//
////        Finally close the LoginActivity and go back to the CameraActivity:
//        finish();
    }

    @Override
    public void onLoginError() {

//        EditText pwEdit = (EditText) findViewById(R.id.password_edittext);
//        pwEdit.setError(getResources().getString(R.string.login_error_text));

    }



}
