package at.ac.tuwien.caa.docscan.ui.document;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.TranskribusMetaData;

public class EditDocumentActivity extends CreateDocumentActivity{

    public static final String DOCUMENT_NAME_KEY = "DOCUMENT_NAME";

    private static final String CLASS_NAME = "EditDocumentActivity";

    private String mDocumentTitle;
    private Document mDocument;
    private boolean mIsActiveDocument = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.initToolbarTitle(R.string.edit_series_title);

    }

    public void onResume() {

        super.onResume();
        // Read the document name transmitted via the intent:
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDocumentTitle = extras.getString(DOCUMENT_NAME_KEY, null);
            if (mDocumentTitle != null) {
                mDocument = DocumentStorage.getInstance(this).getDocument(mDocumentTitle);
                if (mDocument != null) {
                    if (mDocument == DocumentStorage.getInstance(this).getActiveDocument())
                        mIsActiveDocument = true;

                    fillViews(mDocument);
                }
                else
                    finish(); // Nothing to do here, but this should not happen...

            }
            else
                finish(); // Nothing to do here, but this should not happen...
        }

    }


    @Override
    protected void initOkButton() {

        Button okButton = findViewById(R.id.create_series_done_button);
        okButton.setText(getString(R.string.edit_series_done_button_text));
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveChanges();

            }
        });

    }

    private void saveChanges() {

        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
        String title = titleEditText.getText().toString();

        Log.d(CLASS_NAME, "saveChanges: mDocumentTitle: " + mDocumentTitle + " title: " + title);
        Log.d(CLASS_NAME, "saveChanges: mDocument.getTitle: " + mDocument.getTitle() + " title: " + title);

//        if (mDocumentTitle == null) {
////                This should not happen:
//            finish();
//            Crashlytics.logException(new Throwable(CLASS_NAME + "saveChanges: mDocumentTitle == null"));
//            return;
//        }
        if (mDocument == null || mDocument.getTitle() == null) {
//                This should not happen:
            finish();
            Crashlytics.logException(new Throwable(CLASS_NAME + "saveChanges: mDocument == null || mDocument.getTitle() == null"));
            return;
        }

//        Document document = DocumentStorage.getInstance(this).getDocument(mDocumentTitle);
//        if (document == null) {
////            This should not happen:
//            finish();
//            Log.d(CLASS_NAME, "document == null");
//            Crashlytics.logException(new Throwable(CLASS_NAME + "saveChanges: document == null"));
//            return;
//        }

        if (mDocument == null) {
//            This should not happen:
            finish();
            Log.d(CLASS_NAME, "document == null");
            Crashlytics.logException(new Throwable(CLASS_NAME + "saveChanges: mDocument == null"));
            return;
        }

////        Check if the new title is not already assigned to the remaining documents
////        (exclude the active one):
//        if (DocumentStorage.getInstance(this).isTitleAlreadyAssigned(title) &&
//                (mDocumentTitle.compareToIgnoreCase(title) != 0)) {
//            showDirExistingCreatedAlert(title);
//            return;
//        }

//        Check if the new title is not already assigned to the remaining documents
//        (exclude the active one):
        if (mDocument.getTitle().compareToIgnoreCase(title) != 0) {
            if (DocumentStorage.getInstance(this).isTitleAlreadyAssigned(title)) {
                showDirExistingCreatedAlert(title);
                return;
            }

        }
////        Check if the edited document is the active document (in the CameraActivity):
//        if (mDocumentTitle.compareToIgnoreCase(
//                DocumentStorage.getInstance(this).getActiveDocument().getTitle()) == 0)
//            DocumentStorage.getInstance(this).setTitle(title);


//        This is necessary to handle double clicks, which occurred on Firebase devices and caused
//        NullPointerExceptions, because the saveChanges method was called twice, but the result
//        type was different, because mDocumentTitle was not altered in the meantime.
//        mDocumentTitle = title;

        mDocument.setTitle(title);

        TranskribusMetaData metaData = mDocument.getMetaData();
        if (metaData == null) {
            metaData = new TranskribusMetaData();
            mDocument.setMetaData(metaData);
        }

        EditText authorEditText = findViewById(R.id.create_series_author_edittext);
        metaData.setAuthor(authorEditText.getText().toString());

        EditText writerEditText = findViewById(R.id.create_series_writer_edittext);
        metaData.setWriter(writerEditText.getText().toString());

        EditText genreEditText = findViewById(R.id.create_series_genre_edittext);
        metaData.setGenre(genreEditText.getText().toString());

        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
        metaData.setSignature(signatureEditText.getText().toString());

        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
        metaData.setAuthority(authorityEditText.getText().toString());

        EditText urlEditText = findViewById(R.id.create_series_url_edittext);
        metaData.setUrl(urlEditText.getText().toString());

//        DocumentStorage.getInstance(this).replaceDocument(document, mDocumentTitle);
        DocumentStorage.saveJSON(this);

        //        Check if the edited document is the active document (in the CameraActivity):
        if (mIsActiveDocument) {
            Log.d(CLASS_NAME, "setting active document title: " + title);
            DocumentStorage.getInstance(this).setTitle(title);
        }

//        Send back the new file name, so that it can be used in the DocumentViewerActivity:
        Intent data = new Intent();
        data.setData(Uri.parse(mDocument.getTitle()));
        setResult(RESULT_OK, data);

        finish();

        Log.d(CLASS_NAME, "mDocumentTitle: " + mDocumentTitle);


    }

    private void fillViews(Document document) {

        String title = document.getTitle();
        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
        titleEditText.setText(title);

        TranskribusMetaData metaData = document.getMetaData();
        if (metaData == null)
            return;


        EditText authorEditText = findViewById(R.id.create_series_author_edittext);
        authorEditText.setText(metaData.getAuthor());

        EditText writerEditText = findViewById(R.id.create_series_writer_edittext);
        writerEditText.setText(metaData.getWriter());

        EditText genreEditText = findViewById(R.id.create_series_genre_edittext);
        genreEditText.setText(metaData.getGenre());

        EditText signatureEditText = findViewById(R.id.create_series_signature_edittext);
        signatureEditText.setText(metaData.getSignature());

        EditText authorityEditText = findViewById(R.id.create_series_authority_edittext);
        authorityEditText.setText(metaData.getAuthority());

        EditText urlEditText = findViewById(R.id.create_series_url_edittext);
        urlEditText.setText(metaData.getUrl());

//        Check if the document is a special archive document created from QR code:
        boolean editable = metaData.getRelatedUploadId() == null;
        if (!editable) {

            titleEditText.setKeyListener(null);
            authorEditText.setKeyListener(null);
            writerEditText.setKeyListener(null);
            genreEditText.setKeyListener(null);
            signatureEditText.setKeyListener(null);
            authorityEditText.setKeyListener(null);
            urlEditText.setKeyListener(null);

        }



    }

}
