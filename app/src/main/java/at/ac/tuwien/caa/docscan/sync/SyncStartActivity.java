package at.ac.tuwien.caa.docscan.sync;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

/**
 * Created by fabian on 31.08.2017.
 */

public class SyncStartActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Toast.makeText(this, "SyncStartActivity started", Toast.LENGTH_LONG);
        Log.d(this.getClass().getName(), "SyncStartActivity created");


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
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
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



    }

}