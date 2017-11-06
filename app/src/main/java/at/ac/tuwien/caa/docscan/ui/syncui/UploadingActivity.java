package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ProgressBar;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

/**
 * Created by fabian on 12.10.2017.
 * Taken LocalBroadCastManager example taken from:
 * https://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager
 */

public class UploadingActivity extends BaseNoNavigationActivity {

    public static String UPLOAD_PROGRESS_ID = "UPLOAD_PROGRESS_ID";
    public static String UPLOAD_FINISHED_ID = "UPLOAD_FINISHED_ID";
    public static String UPLOAD_ERROR_ID = "UPLOAD_ERROR_ID";

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading);

        mProgressBar = (ProgressBar) findViewById(R.id.uploading_progressbar);

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("PROGRESS_INTENT_NAME"));

    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
//            String message = intent.getStringExtra("message");
//            Log.d("receiver", "Got message: " + message);
            int progress = intent.getIntExtra(UPLOAD_PROGRESS_ID, 0);

            mProgressBar.setProgress(progress);

        }
    };
}
