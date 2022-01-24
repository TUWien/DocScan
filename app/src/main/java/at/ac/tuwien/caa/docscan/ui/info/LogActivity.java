package at.ac.tuwien.caa.docscan.ui.info;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.deprecated.DataLog;
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer;

/**
 * Created by fabian on 09.03.2017.
 */
public class LogActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_log);

        Button sendButton = findViewById(R.id.share_log_button);
        final Activity activity = this;
        sendButton.setOnClickListener(v -> DataLog.getInstance().shareLog(activity));
    }

    @NonNull
    @Override
    protected NavigationDrawer.NavigationItem getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItem.SHARE_LOG;
    }

    public static Intent newInstance(Context context) {
        return new Intent(context, LogActivity.class);
    }
}
