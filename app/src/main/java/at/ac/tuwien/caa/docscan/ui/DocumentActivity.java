package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.widget.ExpandableListView;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 26.09.2017.
 */

public class DocumentActivity extends BaseNoNavigationActivity  {

    private ExpandableListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);
        super.initToolbarTitle(R.string.document_title);

        mListView = (ExpandableListView) findViewById(R.id.document_list_view);


    }



}
