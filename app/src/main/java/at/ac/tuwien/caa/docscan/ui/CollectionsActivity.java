package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.RestQuest;

/**
 * Created by fabian on 30.11.2016.
 */
public class CollectionsActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        RestQuest.createLoginRequest(this);

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.COLLECTIONS;
    }
}
