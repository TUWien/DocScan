package at.ac.tuwien.caa.docscan.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

/**
 * Created by fabian on 24.10.2017.
 */

public class CreateSeriesActivity extends BaseNoNavigationActivity  {

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0;

    // Eventually a permission is required for dir creation, hence we store this as member:
    private File mSubDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_series);

        super.initToolbarTitle(R.string.create_series_title);

        final EditText userInput = (EditText) findViewById(R.id.create_series_name_edittext);

        Button okButton = (Button) findViewById(R.id.create_series_done_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateDirResult(userInput.getText().toString());
            }
        });

    }

    private void onCreateDirResult(String result) {

        File mediaStorageDir = Helper.getMediaStorageDir(getResources().getString(R.string.app_name));
        File subDir = new File(mediaStorageDir.getAbsolutePath(), result);

        if (subDir.exists()) {
            showDirExistingCreatedAlert(subDir.getName());
            return;
        }

        if (isWriteExternalStoragePermitted())
            createSubDir(subDir);
        else {
            mSubDir = subDir;
            // ask for permission:
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        }

    }

    private void createSubDir(File subDir) {

        boolean dirCreated = false;
        if (subDir != null) {
            dirCreated = subDir.mkdir();
        }

        if (!dirCreated)
            showNoDirCreatedAlert();
        else {
            User.getInstance().setDocumentName(subDir.getName());
            UserHandler.saveSeriesName(this);

            Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY, true);
            Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_PAUSED_KEY, false);

            Helper.startCameraActivity(this);
        }

    }

    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode Request code
     * @param permissions Permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (isPermissionGiven && mSubDir != null)
                    createSubDir(mSubDir);
                break;
        }
    }



    /**
     * Check if we have the permission to write to the external storage.
     * @return
     */
    private boolean isWriteExternalStoragePermitted() {

        boolean isPermitted =
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        return isPermitted;
    }

    private void showDirExistingCreatedAlert(String dirName) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String msg = getResources().getString(R.string.document_dir_existing_prefix_message)+
                " " + dirName + " " +
                getResources().getString(R.string.document_dir_existing_postfix_message);
        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_no_dir_created_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(msg);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void showNoDirCreatedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_no_dir_created_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.document_no_dir_created_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

}
