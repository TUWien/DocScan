package at.ac.tuwien.caa.docscan.ui.document;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

/**
 * Created by fabian on 24.10.2017.
 */

public class CreateSeriesActivity extends BaseNoNavigationActivity {

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
    public static final String DOCUMENT_QR_TEXT = "DOCUMENT_QR_TEXT";

    // Eventually a permission is required for dir creation, hence we store this as member:
    private File mSubDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_series);

        super.initToolbarTitle(R.string.create_series_title);

        initOkButton();

//        Temporarily deactivate the advanced fields:
//        initShowFieldsCheckBox();

//        Debugging: (if you just want to launch the Activity (without CameraActivity)
//        String qrText = "<root><authority>Universitätsarchiv Greifswald</authority><identifier type=\"hierarchy description\">Universitätsarchiv Greifswald/Altes Rektorat/01. Rechtliche Stellung der Universität - 01.01. Statuten/R 1199</identifier><identifier type=\"uri\">https://ariadne-portal.uni-greifswald.de/?arc=1&type=obj&id=5162222</identifier><title>Entwurf neuer Universitätsstatuten </title><date normal=\"1835010118421231\">1835-1842</date><callNumber>R 1199</callNumber><description>Enthält u.a.: Ausführliche rechtshistorische Begründung des Entwurfs von 1835.</description></root>";
//        processQRCode(qrText);

        // Read the information in the QR Code transmitted via the intent:
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String initString = "";
            String qrText = extras.getString(DOCUMENT_QR_TEXT, initString);
            if (!qrText.equals(initString)) {
                Log.d(getClass().getName(), qrText);
                processQRCode(qrText);
            }
        }



    }

    private void processQRCode(String text) {

        // Currently the XML has no root defined (malformed) so we add one manually:
        String qrText = "<root>" + text + "</root>";

        Log.d(getClass().getName(), "parsing document");
        Document document = parseQRCode(qrText);
        Log.d(getClass().getName(), "found document: " + document);
        fillViews(document);

    }

    private Document parseQRCode(String text) {

        return Document.parseXML(text);

    }

//        Temporarily deactivate the advanced fields:
   private void fillViews(Document document) {

        if (document == null)
            return;

        // Title:
        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
        if (document.getTitle() != null)
            titleEditText.setText(document.getTitle());

//        // Description:
//        EditText descriptionEditText = findViewById(R.id.create_series_description_edittext);
//        if (document.getTitle() != null)
//            descriptionEditText.setText(document.getDescription());
//
//        // Signature:
//        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
//        if (document.getSignature() != null)
//            signatureEditText.setText(document.getSignature());
//
//        // Authority:
//        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
//        if (document.getAuthority() != null)
//            authorityEditText.setText(document.getAuthority());
//
//        // Hierarchy:
//        EditText hierarchyEditText = findViewById(R.id.create_series_hierarchy_edittext);
//        if (document.getHierarchy() != null)
//            hierarchyEditText.setText(document.getHierarchy());
//
//        // Uri:
//        EditText uriEditText = findViewById(R.id.create_series_uri_edittext);
//        if (document.getUri() != null)
//            uriEditText.setText(document.getUri());

    }


//    private void fillViews(Document document) {
//
//        // Emulate a click and open up the extended layout (shows the views above):
//        CheckBox checkBox = (CheckBox) findViewById(R.id.create_series_advanced_options_checkbox);
//        checkBox.setChecked(true);
//
//        if (document == null)
//            return;
//
//        // Title:
//        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
//        if (document.getTitle() != null)
//            titleEditText.setText(document.getTitle());
//
////        // Description:
////        EditText descriptionEditText = findViewById(R.id.create_series_description_edittext);
////        if (document.getTitle() != null)
////            descriptionEditText.setText(document.getDescription());
//
//        // Signature:
//        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
//        if (document.getSignature() != null)
//            signatureEditText.setText(document.getSignature());
//
//        // Authority:
//        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
//        if (document.getAuthority() != null)
//            authorityEditText.setText(document.getAuthority());
//
//        // Hierarchy:
//        EditText hierarchyEditText = findViewById(R.id.create_series_hierarchy_edittext);
//        if (document.getHierarchy() != null)
//            hierarchyEditText.setText(document.getHierarchy());
//
////        // Uri:
////        EditText uriEditText = findViewById(R.id.create_series_uri_edittext);
////        if (document.getUri() != null)
////            uriEditText.setText(document.getUri());
//
//    }



    private void initOkButton() {
        final EditText userInput = (EditText) findViewById(R.id.create_series_name_edittext);

        Button okButton = (Button) findViewById(R.id.create_series_done_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateDirResult(userInput.getText().toString());
            }
        });
    }

    private void initShowFieldsCheckBox() {
        final RelativeLayout fieldsLayout = (RelativeLayout) findViewById(R.id.create_series_fields_layout);

        CheckBox showFieldsCheckBox = (CheckBox) findViewById(R.id.create_series_advanced_options_checkbox);
        showFieldsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked)
                    fieldsLayout.setVisibility(View.VISIBLE);
                else
                    fieldsLayout.setVisibility(View.INVISIBLE);

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
