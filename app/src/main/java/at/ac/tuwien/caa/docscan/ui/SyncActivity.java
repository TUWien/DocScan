package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.os.Bundle;
import android.widget.ExpandableListView;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.syncui.SyncAdapter;

/**
 * Created by fabian on 22.09.2017.
 */

public class SyncActivity extends BaseNavigationActivity {

    private ExpandableListView mListView;
    private Context mContext;
    private SyncAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        mListView = (ExpandableListView) findViewById(R.id.sync_list_view);
        mContext = this;

        mAdapter = new SyncAdapter(this);
        mListView.setAdapter(mAdapter);


    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SYNC;
    }
}
