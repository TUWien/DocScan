package at.ac.tuwien.caa.docscan.ui.document;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.TranskribusMetaData;

public class EditDocumentActivity extends CreateDocumentActivity{

    public static final String DOCUMENT_NAME_KEY = "DOCUMENT_NAME";

    private String mDocumentTitle;

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
                Document document = DocumentStorage.getInstance(this).getDocument(mDocumentTitle);
                if (document != null)
                    fillViews(document);
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

        if (mDocumentTitle == null)
//                This should not happen:
            finish();

        Document document = DocumentStorage.getInstance(this).getDocument(mDocumentTitle);
        if (document == null)
//            This should not happen:
            finish();

        EditText titleEditText = findViewById(R.id.create_series_name_edittext);
        String title = titleEditText.getText().toString();

//        Check if the new title is not already assigned to the remaining documents
//        (exclude the active one):
        if (DocumentStorage.getInstance(this).isTitleAlreadyAssigned(title) &&
                (mDocumentTitle.compareToIgnoreCase(title) != 0)) {
            showDirExistingCreatedAlert(title);
            return;
        }

//        Check if the edited document is the active document (in the CameraActivity):
        if (mDocumentTitle.compareToIgnoreCase(
                DocumentStorage.getInstance(this).getActiveDocument().getTitle()) == 0)
            DocumentStorage.getInstance(this).setTitle(title);

        document.setTitle(title);

        TranskribusMetaData metaData = document.getMetaData();
        if (metaData == null) {
            metaData = new TranskribusMetaData();
            document.setMetaData(metaData);
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

//        Send back the new file name, so that it can be used in the GalleryActivity:
        Intent data = new Intent();
        data.setData(Uri.parse(document.getTitle()));
        setResult(RESULT_OK, data);

        finish();


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
