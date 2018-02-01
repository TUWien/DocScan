package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 01.02.2018.
 */

public class DocumentViewActivity extends BaseNoNavigationActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_document_view);
    }
}
