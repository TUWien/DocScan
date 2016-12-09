package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 30.11.2016.
 */
public class CollectionsActivity extends BaseActivity implements LoginRequest.LoginCallback, CollectionsRequest.CollectionsCallback {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        RequestHandler.createRequest(this, RequestHandler.REQUEST_LOGIN);

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {

        return NavigationDrawer.NavigationItemEnum.COLLECTIONS;

    }

    @Override
    public void onLogin(User user) {

        RequestHandler.createRequest(this, RequestHandler.REQUEST_COLLECTIONS);

    }

    @Override
    public void onCollections(User user) {

    }
}
