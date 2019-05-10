package at.ac.tuwien.caa.docscan.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.LogoutRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.sync.DropboxUtils;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;

/**
 * Created by fabian on 22.08.2017.
 */

public class AccountActivity extends BaseNavigationActivity implements DropboxUtils.DropboxCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        initButtons();

    }

    @Override
    protected void onResume() {

        super.onResume();

        showAccountDetails();
        showButtonsLayout(Helper.isOnline(this));

    }

    private void showButtonsLayout(boolean show) {

        LinearLayout buttonsLayout = findViewById(R.id.account_button_layout);
        TextView offlineTextView = findViewById(R.id.account_offline_textview);
        if (show) {
            buttonsLayout.setVisibility(View.VISIBLE);
            offlineTextView.setVisibility(View.GONE);
        }
        else {
            buttonsLayout.setVisibility(View.GONE);
            offlineTextView.setVisibility(View.VISIBLE);
        }

    }

    private void initButtons() {

        Button transkribusButton = findViewById(R.id.sync_switcher_transkribus_button);
        transkribusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteSyncInfoAlert(TranskribusLoginActivity.class);
            }
        });

        Class t = DropboxLoginActivity.class;

        Button dropboxButon = findViewById(R.id.sync_switcher_dropbox_button);
        dropboxButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteSyncInfoAlert(DropboxLoginActivity.class);
            }
        });
    }


    private void showDeleteSyncInfoAlert(final Class proceedingActivity) {

//        Proceed if no file has been uploaded:
        if (SyncStorage.getInstance(this).getSyncList() == null ||
                SyncStorage.getInstance(this).getSyncList().isEmpty()) {
            clearUserAndLogin(proceedingActivity);
            return;
        }

//        If file(s) have been uploaded, warn the user, before the SyncStorage is deleted:
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.account_delete_sync_storage_title)
                .setPositiveButton(R.string.sync_confirm_login_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        clearUserAndLogin(proceedingActivity);
                    }
                })
                .setNegativeButton(R.string.sync_cancel_login_button_text, null)
                .setCancelable(true)
                .setMessage(R.string.account_delete_sync_storage_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void clearUserAndLogin(Class proceedingActivity) {

        if (proceedingActivity == null)
            return;

        User.getInstance().setLoggedIn(false);

//        This deletes the sync information:
        SyncStorage.clearInstance();
        UserHandler.clearUser(this);

//        startActivity(proceedingActivity);

//        Clear the dropbox token (if it is set). This is done online and might take some time so,
//        we wait for an event, because otherwise the ui might be blocked:
        if (proceedingActivity == DropboxLoginActivity.class)
            DropboxUtils.getInstance().clearAccessToken(this);
        else {
            new LogoutRequest(this);
            startActivity(proceedingActivity);
        }

    }

    private void startActivity(Class proceedingActivity) {
        //        Start the proceeding activity:
        Intent intent = new Intent(getApplicationContext(), proceedingActivity);
        startActivity(intent);
    }

    private void showAccountDetails() {

        // Show the first and last name of the user:
        TextView userTextView = findViewById(R.id.account_user_textview);
        String userText = getString(R.string.account_user_textview_text) + " " +
                User.getInstance().getFirstName() + " " + User.getInstance().getLastName();
        userTextView.setText(userText);

        // Show the cloud service:
        TextView cloudTextView = findViewById(R.id.account_cloud_textview);
        String cloudText = getString(R.string.account_cloud_textview_text) + " ";
        if (User.getInstance().getConnection() == User.SYNC_DROPBOX)
            cloudText += getString(R.string.sync_dropbox_text);
        else if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS)
            cloudText += getString(R.string.sync_transkribus_text);
        cloudTextView.setText(cloudText);

    }

    private void hideAccountDetails() {
//            Remove the view from the parent layout. If the view is just hidden, the layout below
//            is not centered.
        View v = findViewById(R.id.current_account_layout);
        if (v != null)
            ((ViewManager)v.getParent()).removeView(v);
    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ACCOUNT_EDIT;
    }


    @Override
    public void onAccesTokenCleared(boolean isCleared) {

        if (isCleared)
            startActivity(DropboxLoginActivity.class);

    }
}
