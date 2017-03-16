package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;

/**
 * Created by fabian on 09.03.2017.
 */

public class LogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_log);

        Button sendButton = (Button) findViewById(R.id.share_log_button);
        final Context context = this;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataLog.getInstance().writeLog(context);
            }
        });

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SHARE_LOG;
    }

}
