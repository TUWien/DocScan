package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 22.06.2017.
 */

public class RestTestActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.REST_TEST;
    }

}
