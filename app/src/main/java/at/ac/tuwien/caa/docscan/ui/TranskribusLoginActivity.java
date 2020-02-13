package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.VolleyError;

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
        pwEdit.setError(getResources().getString(R.string.login_error_text));

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
