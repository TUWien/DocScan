package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 08.02.2017.
 */
public class LoginActivity extends BaseActivity implements LoginRequest.LoginCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login();
                    }
                });


    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.LOGIN;
    }

    private void login() {

        EditText emailEdit = (EditText) findViewById(R.id.username_edittext);
        EditText pwEdit = (EditText) findViewById(R.id.password_edittext);

        String email = emailEdit.getText().toString();
        String pw = pwEdit.getText().toString();

        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, R.string.login_text, Toast.LENGTH_SHORT).show();
            return;
        }

        User.getInstance().setUserName(email);
        User.getInstance().setPassword(pw);

        RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);

    }

    @Override
    public void onLogin(User user) {

    }
}
