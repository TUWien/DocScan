package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

/**
 * Created by fabian on 08.02.2017.
 */
public class LoginActivity extends BaseNoNavigationActivity implements LoginRequest.LoginCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        super.initToolbarTitle(R.string.login_title);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login();
                    }
                });


    }

//    @Override
//    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
//        return NavigationDrawer.NavigationItemEnum.LOGIN;
//    }

    private void login() {

        EditText emailEdit = (EditText) findViewById(R.id.username_edittext);
        EditText pwEdit = (EditText) findViewById(R.id.password_edittext);

        String email = emailEdit.getText().toString();
        String pw = pwEdit.getText().toString();

        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, R.string.login_check_input_toast, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, welcomeText, Toast.LENGTH_SHORT).show();

//        Save the credentials:
        UserHandler.saveTranskribusCredentials(this);

        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        startActivity(intent);
    }

    @Override
    public void onLoginError() {

        showLoadingLayout(false);

        EditText pwEdit = (EditText) findViewById(R.id.password_edittext);
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
}
