package at.ac.tuwien.caa.docscan.ui.document;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.syncui.DocumentAdapter;
import at.ac.tuwien.caa.docscan.ui.syncui.DocumentUploadAdapter;
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar;

public class SelectDocumentActivity extends BaseNoNavigationActivity implements
        DocumentAdapter.DocumentAdapterCallback {

    private static final String CLASS_NAME = "SelectDocumentActivity";

    private Context mContext;
    private ArrayList<Document> mDocuments;
    private DocumentAdapter mAdapter;
    private ListView mListView;
    private Document mSelectedDocument;
    private Button mSelectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_select_document);
        mListView = findViewById(R.id.upload_list_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mListView.setNestedScrollingEnabled(true);
        }

        initAdapter();
        initSelectButton();
        initToolbarTitle(R.string.select_document_title);


    }

    @Override
    public void onPause() {

        super.onPause();
        DocumentStorage.saveJSON(this);

    }

    private void initSelectButton() {

        mSelectButton = findViewById(R.id.document_select_button);
        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedDocument != null) {
                    DocumentStorage.getInstance(mContext).setTitle(mSelectedDocument.getTitle());
                    Helper.startCameraActivity(mContext);
                }

            }
        });

    }

    private void initAdapter() {

        if (mContext != null) {

            mDocuments = DocumentStorage.getInstance(this).getDocuments();
            mAdapter = new DocumentAdapter(mContext, R.layout.rowlayout, mDocuments);
            if (mListView != null) {
                mListView.setAdapter(mAdapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        onSelectionChange();
                    }
                });
            }

        }

    }



    @Override
    public void onSelectionChange() {

        ArrayList<Document> documents = getSelectedDocuments();

        if (documents.isEmpty()) {
            mSelectedDocument = null;
            mSelectButton.setVisibility(View.INVISIBLE);
        }
        else {
            mSelectedDocument = documents.get(0);
            mSelectButton.setVisibility(View.VISIBLE);
        }

    }


    private ArrayList<Document> getSelectedDocuments() {

        ArrayList<Document> documents = new ArrayList<>();

        if (mDocuments != null && mListView != null) {
            SparseBooleanArray checkedItems = mListView.getCheckedItemPositions();
            if (checkedItems != null) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    if (checkedItems.valueAt(i)) {
                        documents.add(mDocuments.get(checkedItems.keyAt(i)));
                    }
                }
            }
        }

        return documents;

    }

}
