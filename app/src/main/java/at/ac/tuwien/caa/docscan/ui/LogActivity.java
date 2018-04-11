package at.ac.tuwien.caa.docscan.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;

/**
 * Created by fabian on 09.03.2017.
 */

public class LogActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_log);

        Button sendButton = (Button) findViewById(R.id.share_log_button);
        final Activity activity = this;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                DataLog.getInstance().shareLog(activity);
                DataLog.getInstance().shareUploadLog(activity);
            }
        });

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SHARE_LOG;
    }

}
