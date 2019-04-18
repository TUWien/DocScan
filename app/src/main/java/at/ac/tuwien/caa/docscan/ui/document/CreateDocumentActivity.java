package at.ac.tuwien.caa.docscan.ui.document;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
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
import at.ac.tuwien.caa.docscan.logic.TranskribusMetaData;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

/**
 * Created by fabian on 24.10.2017.
 */

public class CreateDocumentActivity extends BaseNoNavigationActivity {

    private static final String CLASS_NAME = "CreateDocumentActivity";
    private TranskribusMetaData mTranskribusMetaData = null;
    public static final String DOCUMENT_QR_TEXT = "DOCUMENT_QR_TEXT";
    private static final String SHOW_TRANSKRIBUS_METADATA_KEY = "SHOW_TRANSKRIBUS_METADATA";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_document);

        super.initToolbarTitle(R.string.create_series_title);

        initOkButton();
        initUrlButton();
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
                mTranskribusMetaData = processQRCode(qrText);
                if (mTranskribusMetaData != null)
                    fillViews(mTranskribusMetaData);
                else
                    showQRCodeErrorAlert();
            }
        }
    }

    private void initUrlButton() {

        AppCompatImageButton linkButton = findViewById(R.id.create_series_link_button);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText urlEditText = findViewById(R.id.create_series_url_edittext);
                String url = urlEditText.getText().toString();
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse(url);
                    browserIntent.setData(uri);
                    startActivity(browserIntent);
                }
                catch (ActivityNotFoundException e) {
                    showUrlNotValidAlert(url);
                }
            }
        });

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


    @Override
    public void onResume() {

        super.onResume();
        Log.d(CLASS_NAME, "onresume");
    }

    private TranskribusMetaData processQRCode(String text) {

        // Currently the XML has no root defined (malformed) so we add one manually:
        String qrText = "<root>" + text + "</root>";
        Log.d(getClass().getName(), "parsing document");
        TranskribusMetaData metaData = parseQRCode(qrText);

        Log.d(getClass().getName(), "found document: " + metaData);

        return metaData;

    }

    private TranskribusMetaData parseQRCode(String text) {

        Log.d(getClass().getName(), "QR code text: " + text);

        return TranskribusMetaData.parseXML(text);

    }

    //        Temporarily deactivate the advanced fields:
    private void fillViews(TranskribusMetaData qrCodeInfo) {

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
                AppCompatImageButton button = findViewById(R.id.create_series_link_button);
                button.setVisibility(View.GONE);
            }
        }
        else
            layout.setVisibility(View.INVISIBLE);

    }

    private void fillAdvancedFields(final TranskribusMetaData metaData, boolean editable) {
        //           // Description:
//           EditText descriptionEditText = findViewById(R.id.create_series_description_edittext);
//           if (document.getTitle() != null)
//               descriptionEditText.setText(document.getDescription());

        if (metaData == null)
            return;

//        Note: The following three fields are not passed via QR code, but we disable them if not
//        editable:
        EditText authorEditText = findViewById(R.id.create_series_author_edittext);
        EditText writerEditText = findViewById(R.id.create_series_writer_edittext);
        EditText genreEditText = findViewById(R.id.create_series_genre_edittext);

        // Signature:
        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
        if (metaData.getSignature() != null)
            signatureEditText.setText(metaData.getSignature());

        // Authority:
        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
        if (metaData.getAuthority() != null)
            authorityEditText.setText(metaData.getAuthority());

        //        URI button:
        EditText urlEditText = findViewById(R.id.create_series_url_edittext);
        if (metaData.getUrl() != null)
            urlEditText.setText(metaData.getUrl());

//        Check if the document is a special archive document created from QR code:

        if (!editable) {
            authorEditText.setKeyListener(null);
            writerEditText.setKeyListener(null);
            genreEditText.setKeyListener(null);
            signatureEditText.setKeyListener(null);
            authorityEditText.setKeyListener(null);
            urlEditText.setKeyListener(null);
        }


    }


    protected void initOkButton() {

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
                if (isDocumentCreated) {
                    Helper.startCameraActivity(context);

                    //        Send back the new file name, so that it can be used in the GalleryActivity:
                    Intent data = new Intent();
                    setResult(RESULT_OK, data);

                    finish();
                }
            }
        });

    }

    private boolean createNewDocument(String title) {

        if (title == null)
            return false;

        boolean isTitleAlreadyAssigned =
                DocumentStorage.getInstance(this).isTitleAlreadyAssigned(title);

        if (isTitleAlreadyAssigned) {
            showDirExistingCreatedAlert(title);
            return false;
        }

//        Fill the metadata with the field values entered by the user:
        readFieldValues();

        boolean isDocumentCreated = DocumentStorage.getInstance(this).createNewDocument(title);
        if (!isDocumentCreated)
            showNoDirCreatedAlert();

        else if (mTranskribusMetaData != null) {
//        Save the metadata:
            Document document = DocumentStorage.getInstance(this).getDocument(title);
            if (document != null) // This should not happen...
                document.setMetaData(mTranskribusMetaData);
        }

        return isDocumentCreated;

    }

    private void readFieldValues() {
        EditText authorEditText = findViewById(R.id.create_series_author_edittext);
        String author = authorEditText.getText().toString();

        EditText writerEditText = findViewById(R.id.create_series_writer_edittext);
        String writer = writerEditText.getText().toString();

        EditText genreEditText = findViewById(R.id.create_series_genre_edittext);
        String genre = genreEditText.getText().toString();

        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
        String signature = signatureEditText.getText().toString();

        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
        String authority = authorityEditText.getText().toString();

        EditText urlEditText = findViewById(R.id.create_series_url_edittext);
        String url = urlEditText.getText().toString();

//            We need at least one field that is set to create meta data here:
        if (!author.isEmpty() || !writer.isEmpty() || !genre.isEmpty() ||
                !signature.isEmpty() || !authority.isEmpty() || !url.isEmpty()){

            if (mTranskribusMetaData == null)
                mTranskribusMetaData = new TranskribusMetaData();

            mTranskribusMetaData.setAuthor(author);
            mTranskribusMetaData.setWriter(writer);
            mTranskribusMetaData.setGenre(genre);
            mTranskribusMetaData.setSignature(signature);
            mTranskribusMetaData.setAuthority(authority);
            mTranskribusMetaData.setUrl(url);
        }
    }

    private void initShowFieldsCheckBox() {
        final RelativeLayout fieldsLayout = findViewById(R.id.create_series_fields_layout);

        CheckBox showFieldsCheckBox = findViewById(R.id.create_series_advanced_options_checkbox);
//        set the check state to its previous state:
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showFields = sharedPref.getBoolean(SHOW_TRANSKRIBUS_METADATA_KEY, false);
        showFieldsCheckBox.setChecked(showFields);

//        Hide or show the the Transkribus fields:
        showTranskribusFields(showFields, fieldsLayout);

        showFieldsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                showTranskribusFields(isChecked, fieldsLayout);

//                Remember the check state:
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(SHOW_TRANSKRIBUS_METADATA_KEY, isChecked);
                editor.apply();
                editor.commit();


            }
        });
    }

    private void showTranskribusFields(boolean isChecked, RelativeLayout fieldsLayout) {
        if (isChecked)
            fieldsLayout.setVisibility(View.VISIBLE);
        else
            fieldsLayout.setVisibility(View.INVISIBLE);
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

    protected void showDirExistingCreatedAlert(String dirName) {

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

    private void showQRCodeErrorAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_qr_parse_error_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.document_qr_parse_error_message);

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
