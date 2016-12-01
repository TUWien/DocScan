package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 30.11.2016.
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ABOUT;
    }
}
