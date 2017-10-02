package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 24.08.2017.
 */

public class LogoutActivity extends BaseNavigationActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        Button confirmButton = (Button) findViewById(R.id.logout_confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: think about logout here:
                User.getInstance().setLoggedIn(false);
                // Close the activity:
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.logout_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close the activity:
                finish();
            }
        });

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ACCOUNT_LOGOUT;
    }

}
