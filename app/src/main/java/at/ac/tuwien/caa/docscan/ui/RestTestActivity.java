package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.DocumentMetaData;
import at.ac.tuwien.caa.docscan.rest.DocumentRequest;
import at.ac.tuwien.caa.docscan.rest.DocumentsMetaDataRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 22.06.2017.
 */

public class RestTestActivity extends BaseActivity implements CollectionsRequest.CollectionsCallback, DocumentsMetaDataRequest.DocumentsMetaDataCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        Button collectionsButton = (Button) findViewById(R.id.debug_collections_button);
        collectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCollections();
            }
        });

        Button uploadButton = (Button) findViewById(R.id.debug_upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestUpload();
            }
        });

    }

    private void requestCollections() {
        RequestHandler.createRequest(this, RequestHandler.REQUEST_COLLECTIONS);
    }

    private void requestUpload() {
//        RequestHandler.upload
    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
//        return NavigationDrawer.NavigationItemEnum.REST_TEST;
        return NavigationDrawer.NavigationItemEnum.INVALID;
    }

    @Override
    public void onCollections(List<Collection> collections) {

        for (Collection collection : collections) {
//            DocumentsMetaDataRequest request = new DocumentsMetaDataRequest(this, collection);
            if (collection.getName().equals("test")) {
                if (User.getInstance() != null) {
                    User.getInstance().setCollection(collection);
                }
            }
        }

    }

    @Override
    public void onDocumentsMetaData(List<DocumentMetaData> documentMetaDatas) {

        for (DocumentMetaData documentMetaData : documentMetaDatas) {
            DocumentRequest request = new DocumentRequest(this, documentMetaData);

        }
    }
}
