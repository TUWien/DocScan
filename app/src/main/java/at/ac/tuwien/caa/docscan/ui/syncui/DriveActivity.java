package at.ac.tuwien.caa.docscan.ui.syncui;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.dropbox.core.android.Auth;
//import com.google.android.gms.drive.Drive;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.DriveUtils;
import at.ac.tuwien.caa.docscan.sync.DropboxUtils;
import at.ac.tuwien.caa.docscan.sync.OneDriveUtils;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;

/**
 * Created by fabian on 23.08.2017.
 */

public class DriveActivity extends BaseNoNavigationActivity implements LoginRequest.LoginCallback{

    private boolean mIsActitivyJustCreated;

    private static final int REQUEST_CODE_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        super.initToolbarTitle(R.string.dropbox_title);

        mIsActitivyJustCreated = true;

        Button authenticateButton = findViewById(R.id.drive_authenticate_button);
        final Application app = getApplication();
        final Activity activity = this;
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //        TODO: handle cases in which the user rejects the authentication
//                }

//                DriveUtils.getInstance().authenticate(getApplicationContext());
//                startActivityForResult(DriveUtils.getInstance().getSignInClient().getSignInIntent(),
//                        REQUEST_CODE_SIGN_IN);

//                OneDriveUtils.getInstance().startClient(activity);
//                OneDriveUtils.getInstance().startGraphSDK(getApplication(), activity);
            }
        });

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
//                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {

//                    DriveUtils.getInstance().initClient(this);

                }
                break;

        }
    }

    private void showNotAuthenticatedDialog() {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.dropbox_not_auth_title)
                .setCancelable(true)
                .setMessage(R.string.dropbox_not_auth_text);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // The dropbox login is done in the onResume method, because if the user authenticates the
        // dropbox access via a web browser intent, this is the entry to the app after accepting in the
        // browser. However onResume is also called if the activity is created by an internal intent.
        if (!mIsActitivyJustCreated)
            loginToDropbox();

        mIsActitivyJustCreated = false;

    }

    @Override
    public void onLogin(User user) {

        String welcomeText = getResources().getString(R.string.login_welcome_text) + " " + user.getFirstName();
        Toast.makeText(this, welcomeText, Toast.LENGTH_SHORT).show();

        // Start the CameraActivity and remove everything from the back stack:
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    @Override
    public void onLoginError() {
        showLoadingLayout(false);
    }

    private void loginToDropbox() {

        String accessToken = Auth.getOAuth2Token();
//            Save the access token:
        if (accessToken != null) {
            showLoadingLayout(true);
            User.getInstance().setDropboxToken(accessToken);
            UserHandler.saveDropboxToken(this);
            DropboxUtils.getInstance().loginToDropbox(this, accessToken);
        }

//        showLoadingLayout(false);

    }

    private void showLoadingLayout(boolean showLoading) {

        View authenticationLayout = findViewById(R.id.drive_authentication_layout);
        View loadingLayout = findViewById(R.id.drive_loading_layout);


        if (showLoading) {
            authenticationLayout.setVisibility(View.INVISIBLE);
            loadingLayout.setVisibility(View.VISIBLE);
        }
        else {
            authenticationLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.INVISIBLE);
        }

    }


    @Override
    public void handleRestError(RestRequest request, VolleyError error) {

    }
}
