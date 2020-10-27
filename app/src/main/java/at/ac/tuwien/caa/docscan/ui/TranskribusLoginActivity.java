package at.ac.tuwien.caa.docscan.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity;
import me.drakeet.support.toast.ToastCompat;

/**
 * Created by fabian on 08.02.2017.
 */
public class TranskribusLoginActivity extends BaseNoNavigationActivity implements LoginRequest.LoginCallback {

    private Class mParentClass;
    public static final String PARENT_ACTIVITY_NAME = "PARENT_ACTIVITY_NAME";
    private static final String CLASS_NAME = "TranskribusLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        super.initToolbarTitle(R.string.login_title);

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login();
                    }
                });

        // Check what is the parent class that should be shown after the login is done:
        Bundle extras = getIntent().getExtras();

        // The string extra look something like this:
//        at.ac.tuwien.caa.docscan.ui.syncui.SyncActivity$1
//        So we have to use String.startsWith instead of String.compareTo
        if (extras != null && extras.getString(PARENT_ACTIVITY_NAME, "").startsWith(DocumentViewerActivity.class.getName()))
            mParentClass = DocumentViewerActivity.class;
        else
            mParentClass = CameraActivity.class;


    }

    private void login() {

//        Check out this for com.android.volley.NoConnectionError: javax.net.ssl.SSLHandshakeException: Handshake failed
//        https://stackoverflow.com/questions/31269425/how-do-i-tell-the-tls-version-in-android-volley

        EditText emailEdit = findViewById(R.id.username_edittext);
        EditText pwEdit = findViewById(R.id.password_edittext);

        String email = emailEdit.getText().toString();
        String pw = pwEdit.getText().toString();

        if (email.isEmpty() || pw.isEmpty()) {

            ToastCompat.makeText(this, R.string.login_check_input_toast, Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        User.getInstance().setUserName(email);
        User.getInstance().setPassword(pw);

//        Show the loading screen and hide the input fields:
        showLoadingLayout(true);
        RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);

    }

    @Override
    public void onLogin(User user) {

        String welcomeText = getResources().getString(R.string.login_welcome_text) + " " + user.getFirstName();
        ToastCompat.makeText(this, welcomeText, Toast.LENGTH_SHORT).show();
//        Save the credentials:
        UserHandler.saveTranskribusCredentials(this);
        UserHandler.saveUserName(this);

        // Start the parent activity and remove everything from the back stack:
        Intent intent = new Intent(getApplicationContext(), mParentClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onLoginError() {

        showLoadingLayout(false);

        EditText pwEdit = findViewById(R.id.password_edittext);
        pwEdit.setError(getResources().getString(R.string.login_auth_error_text));

    }

    @Override
    public void onLoginError(VolleyError error) {

        showLoadingLayout(false);
//            AuthFailureError, NetworkError, ParseError, ServerError, TimeoutError
        if (error instanceof AuthFailureError) {
            EditText pwEdit = findViewById(R.id.password_edittext);
            pwEdit.setError(getResources().getString(R.string.login_auth_error_text));
        }
        else if (error instanceof NoConnectionError) {
            showErrorDialog(getString(R.string.login_no_connection_error_title),
                    getString(R.string.login_no_connection_error_text));
            Crashlytics.log("login error: " + "login_no_connection_error");
        }
        else if (error instanceof NetworkError) {
            showErrorDialog(getString(R.string.login_network_error_title),
                    getString(R.string.login_network_error_text));
            Crashlytics.log("login error: " + "login_network_error");
        }
        else if (error instanceof ParseError) {
            showErrorDialog(getString(R.string.login_parse_error_title),
                    getString(R.string.login_parse_error_text));
            Crashlytics.log("login error: " + "login_parse_error");
        }
        else if (error instanceof ServerError) {
            showErrorDialog(getString(R.string.login_server_error_title),
                    getString(R.string.login_server_error_text));
            Crashlytics.log("login error: " + "login_server_error");
        }
        else if (error instanceof TimeoutError) {
            showErrorDialog(getString(R.string.login_timeout_error_title),
                    getString(R.string.login_timeout_error_text));
            Crashlytics.log("login error: " + "login_timeout_error");
        }



    }

    private void showErrorDialog(String title, String msg) {

//        If file(s) have been uploaded, warn the user, before the SyncStorage is deleted:
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(title)
                .setPositiveButton(R.string.button_ok, null)
//                .setNegativeButton(R.string.sync_cancel_login_button_text, null)
                .setCancelable(true)
                .setMessage(msg);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void showLoadingLayout(boolean showLoading) {

        View loginFieldsLayout = findViewById(R.id.login_fields_layout);
        View loginLoadingLayout = findViewById(R.id.login_loading_layout);

        if (showLoading) {
            loginFieldsLayout.setVisibility(View.INVISIBLE);
            loginLoadingLayout.setVisibility(View.VISIBLE);
        }
        else {
            loginFieldsLayout.setVisibility(View.VISIBLE);
            loginLoadingLayout.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void handleRestError(RestRequest request, VolleyError error) {

    }
}
