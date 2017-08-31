package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dropbox.core.v2.files.FileMetadata;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.DropboxUtils;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.sync.SyncService;

/**
 * Created by fabian on 17.08.2017.
 */

public class SyncActivity extends BaseNavigationActivity implements DropboxUtils.DropboxConnectorCallback, DropboxUtils.Callback{


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        Button syncButton = (Button) findViewById(R.id.sync_start_service_button);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSyncService();
            }

        });

        final Context context = getApplicationContext();

        Button loadSyncButton = (Button) findViewById(R.id.sync_load_info_button);
        loadSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncInfo.getInstance().readFromDisk(context);
            }
        });

        Button saveSyncButton = (Button) findViewById(R.id.sync_save_info_button);
        saveSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncInfo.getInstance().saveToDisk(context);
            }
        });

    }

    private void startSyncService() {

        Log.d(this.getClass().getName(), "starting sync service");

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));

        Bundle myExtrasBundle = new Bundle();
        myExtrasBundle.putString("some_key", "some_value");

        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(SyncService.class)
                // uniquely identifies the job
                .setTag("my-unique-tag")
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.FOREVER)
                // start between 0 and 60 seconds from now
                .setTrigger(Trigger.executionWindow(0, 5))
                // don't overwrite an existing job with the same tag
                .setReplaceCurrent(false)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
//                .setConstraints(
//                        // only run on an unmetered network
//                        Constraint.ON_UNMETERED_NETWORK,
//                        // only run when the device is charging
//                        Constraint.DEVICE_CHARGING
//                )
                .setConstraints(
                        // only run on an unmetered network
                        Constraint.ON_UNMETERED_NETWORK
                )
                .setExtras(myExtrasBundle)
                .build();

        dispatcher.mustSchedule(syncJob);

//        ComponentName componentName = new ComponentName(this, SyncService.class);
//        JobInfo jobInfo = new JobInfo.Builder(12, componentName)
//                .setRequiresCharging(true)
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
//                .setPersisted(true)
//                .build();
//
//        JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
//        int resultCode = jobScheduler.schedule(jobInfo);
//        if (resultCode == JobScheduler.RESULT_SUCCESS) {
//            Log.d(this.getClass().getName(), "Job scheduled!");
//        } else {
//            Log.d(this.getClass().getName(), "Job not scheduled");
//        }

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SYNC;
    }


    @Override
    public void onDropboxConnected(User user) {

        super.onLogin(user);
//        User.getInstance().setConnection(SYNC_DROPBOX);

    }

    @Override
    public void onUploadComplete(FileMetadata result) {

    }

    @Override
    public void onError(Exception e) {

    }
}
