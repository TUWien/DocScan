package at.ac.tuwien.caa.docscan.ui.syncui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.volley.VolleyError;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.DriveUtils;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

public class OneDriveActivity extends BaseNoNavigationActivity implements LoginRequest.LoginCallback{

    private boolean mIsActitivyJustCreated;

    private static final int REQUEST_CODE_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        super.initToolbarTitle(R.string.dropbox_title);

        mIsActitivyJustCreated = true;

        Button authenticateButton = findViewById(R.id.drive_authenticate_button);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //        TODO: handle cases in which the user rejects the authentication
//                if (!DropboxUtils.getInstance().startAuthentication(getApplicationContext())) {
//                    showNotAuthenticatedDialog();
//                }

//                DriveUtils.getInstance().authenticate(getApplicationContext());
//                startActivityForResult(DriveUtils.getInstance().getSignInClient().getSignInIntent(),
//                        REQUEST_CODE_SIGN_IN);

            }
        });

    }


    @Override
    public void onLogin(User user) {

    }

    @Override
    public void onLoginError() {

    }

    @Override
    public void handleRestError(RestRequest request, VolleyError error) {

    }
}
