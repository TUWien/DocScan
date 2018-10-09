package at.ac.tuwien.caa.docscan.ui.document;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

/**
 * Created by fabian on 24.10.2017.
 */

public class CreateDocumentActivity extends BaseNoNavigationActivity {

    private static final String CLASS_NAME = "CreateDocumentActivity";
    private QRCodeParser qrCodeParser = null;
    public static final String DOCUMENT_QR_TEXT = "DOCUMENT_QR_TEXT";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_document);


        Log.d(CLASS_NAME, "oncreate");

        super.initToolbarTitle(R.string.create_series_title);

        initOkButton();
        initEditField();

//        Temporarily deactivate the advanced fields:
        initShowFieldsCheckBox();

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
                qrCodeParser = processQRCode(qrText);
                String json = qrCodeParser.toJSON();
                Log.d(CLASS_NAME, "metadata json: " + json);

                fillViews(qrCodeParser);
            }
        }
    }

    @Override
    public void onPause() {

        super.onPause();
        DocumentStorage.saveJSON(this);

    }

    private void initEditField() {

        EditText editText = findViewById(R.id.create_series_name_edittext);
        InputFilter filter = Helper.getDocumentInputFilter();
        if (filter != null)
            editText.setFilters(new InputFilter[] {filter});

    }

//    /**
//     * Called after permission has been given or has been rejected. This is necessary on Android M
//     * and younger Android systems.
//     *
//     * @param requestCode Request code
//     * @param permissions Permission
//     * @param grantResults results
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//
//
//        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
//        switch (requestCode) {
//            case PERMISSION_WRITE_EXTERNAL_STORAGE:
//                if (isPermissionGiven)
//                    createDir();
//                else
//                    showNoPermissionAlert();
//                break;
//        }
//    }

    @Override
    public void onResume() {

        super.onResume();
        Log.d(CLASS_NAME, "onresume");
    }

    private QRCodeParser processQRCode(String text) {

        // Currently the XML has no root defined (malformed) so we add one manually:
        String qrText = "<root>" + text + "</root>";
        Log.d(getClass().getName(), "parsing document");
        QRCodeParser metaData = parseQRCode(qrText);

        Log.d(getClass().getName(), "found document: " + metaData);

        return metaData;

    }

    private QRCodeParser parseQRCode(String text) {

        Log.d(getClass().getName(), "QR code text: " + text);

        return QRCodeParser.parseXML(text);

    }

    //        Temporarily deactivate the advanced fields:
    private void fillViews(QRCodeParser qrCodeInfo) {

        if (qrCodeInfo == null)
            return;

        boolean editable = qrCodeInfo.getRelatedUploadId() == null;

        // Title:
        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
        if (qrCodeInfo.getTitle() != null) {
            titleEditText.setText(qrCodeInfo.getTitle());
            if (!editable)
                titleEditText.setKeyListener(null);
        }

        boolean showAdvancedFields = true;

        RelativeLayout layout = findViewById(R.id.create_series_fields_layout);

        fillAdvancedFields(qrCodeInfo, editable);

        if (showAdvancedFields) {
//           Show the advanced settings:
            layout.setVisibility(View.VISIBLE);
//            Hide the link button if we have no link:
            if (qrCodeInfo.getLink() == null) {
                AppCompatButton button = findViewById(R.id.create_series_link_button);
                button.setVisibility(View.GONE);
            }
        }
        else
            layout.setVisibility(View.INVISIBLE);

    }

    private void fillAdvancedFields(final QRCodeParser qrCodeParser, boolean editable) {
        //           // Description:
//           EditText descriptionEditText = findViewById(R.id.create_series_description_edittext);
//           if (document.getTitle() != null)
//               descriptionEditText.setText(document.getDescription());

        // Signature:
        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
        if (qrCodeParser.getSignature() != null) {
            signatureEditText.setText(qrCodeParser.getSignature());
            if (!editable)
                signatureEditText.setKeyListener(null);
        }

        // Authority:
        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
        if (qrCodeParser.getAuthority() != null) {
            authorityEditText.setText(qrCodeParser.getAuthority());
            if (!editable)
                authorityEditText.setKeyListener(null);
        }

//        // Hierarchy:
//        EditText hierarchyEditText = findViewById(R.id.create_series_hierarchy_edittext);
//        if (qrCodeParser.getHierarchy() != null) {
//            hierarchyEditText.setText(qrCodeParser.getHierarchy());
//            if (!editable)
//                hierarchyEditText.setKeyListener(null);
//        }

//        URI button:
        if (qrCodeParser.getLink() != null) {
            AppCompatButton linkButton = findViewById(R.id.create_series_link_button);
            linkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.parse(qrCodeParser.getLink());
                        browserIntent.setData(uri);
                        startActivity(browserIntent);
                    }
                    catch (ActivityNotFoundException e) {
                        showUrlNotValidAlert(qrCodeParser.getLink());
                    }
                }
            });
        }
//           // Uri:
//           EditText uriEditText = findViewById(R.id.create_series_uri_edittext);
//           if (document.getUri() != null)
//               uriEditText.setText(document.getUri());
    }


    private void initOkButton() {

        Button okButton = findViewById(R.id.create_series_done_button);
        final Context context = this;
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Retrieve the entered text:
                EditText editText = findViewById(R.id.create_series_name_edittext);
                String title = editText.getText().toString();

//                The error handling is done in createNewDocument
                boolean isDocumentCreated = createNewDocument(title);
                if (isDocumentCreated)
                    Helper.startCameraActivity(context);
            }
        });

    }

    private boolean createNewDocument(String title) {

        if (title == null)
            return false;

        boolean isTitleAlreadyAssigned =
                DocumentStorage.getInstance().isTitleAlreadyAssigned(title);

        if (isTitleAlreadyAssigned) {
            showDirExistingCreatedAlert(title);
            return false;
        }
        boolean isDocumentCreated = DocumentStorage.getInstance().createNewDocument(title);
        if (!isDocumentCreated)
            showNoDirCreatedAlert();
//        Save the metadata:
        else if (qrCodeParser != null) {
            Document document = DocumentStorage.getInstance().getDocument(title);
            document.setMetaData(qrCodeParser);
        }


        return isDocumentCreated;

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

//    /**
//     * This is called once the user enters a text for the subdir. The function should only be called
//     * after user interaction.
//     */
//    private void askForPermissionCreateDir() {
//
////        No permission given:
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED)
//            // ask for permission:
//            ActivityCompat.requestPermissions(this, new String[]{
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
//        else
//            createDir();
//
//    }

//    /**
//     * Creates a directory for the images. Should only be called if the required permission is
//     * given by the user.
//     */
//    private void createDir() {
//
////        Note: This check is just done for safety, but this point should not be reachable if the
////        permission is not given.
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            // ask for permission:
//            ActivityCompat.requestPermissions(this, new String[]{
//                    Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
//            return;
//        }
//
//
//        EditText editText = findViewById(R.id.create_series_name_edittext);
//        String subDirText = editText.getText().toString();
//
//        File mediaStorageDir = Helper.getMediaStorageDir(getResources().getString(R.string.app_name));
//        File subDir = new File(mediaStorageDir.getAbsolutePath(), subDirText);
//
//        if (subDir.exists()) {
//            showDirExistingCreatedAlert(subDir.getName());
//            return;
//        }
//
//
//        createSubDir(subDir);
//
//    }

//    private void createSubDir(File subDir) {
//
//        boolean dirCreated = false;
//        if (subDir != null) {
//            dirCreated = subDir.mkdir();
//        }
//
//        if (!dirCreated)
//            showNoDirCreatedAlert();
//        else {
//            User.getInstance().setDocumentName(subDir.getName());
//            UserHandler.saveSeriesName(this);
//
////            Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_ACTIVE_KEY, true);
////            Settings.getInstance().saveKey(this, Settings.SettingEnum.SERIES_MODE_PAUSED_KEY, false);
////
//            Helper.startCameraActivity(this);
//        }
//
//    }
//
//    private void showNoPermissionAlert() {
//
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//
//        // set dialog message
//        alertDialogBuilder
//                .setTitle(R.string.document_no_permission_title)
//                .setCancelable(true)
//                .setPositiveButton("OK", null)
//                .setMessage(R.string.document_no_permission_message);
//
//        // create alert dialog
//        AlertDialog alertDialog = alertDialogBuilder.create();
//
//        // show it
//        alertDialog.show();
//
//    }

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

    private void showUrlNotValidAlert(String url) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String text = getResources().getString(R.string.document_invalid_url_message) + " " + url;
        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_invalid_url_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(text);

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
