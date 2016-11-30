package at.ac.tuwien.caa.docscan;

import android.os.Bundle;

/**
 * Created by fabian on 30.11.2016.
 */
public class CollectionsActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.COLLECTIONS;
    }
}
