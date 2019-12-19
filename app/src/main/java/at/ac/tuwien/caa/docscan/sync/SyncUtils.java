package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

import static at.ac.tuwien.caa.docscan.rest.User.SYNC_DROPBOX;
import static at.ac.tuwien.caa.docscan.rest.User.SYNC_TRANSKRIBUS;

/**
 * Created by fabian on 31.08.2017.
 */

public class SyncUtils {

    private static final String CLASS_NAME = "SyncUtils";
    private static final String JOB_TAG = "sync_job";

    /**
     * Starts a new sync job. Note that a current job will not be overwritten. If restart is true
     * the time window for the starting time is increased.
     * @param context
     */
    public static void startSyncJob(Context context, boolean restart) {

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Log.d(CLASS_NAME, "startSyncJob");
        DataLog.getInstance().writeUploadLog(context, CLASS_NAME, "startSyncJob");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean useMobileConnection = sharedPref.getBoolean(context.getResources().getString(
                R.string.key_upload_mobile_data), true);
        int[] constraints;
        if (useMobileConnection) {
            constraints = new int[]{Constraint.ON_ANY_NETWORK};
            Log.d(CLASS_NAME, "startSyncJob: using mobile connection");
        }
        else {
            constraints = new int[]{Constraint.ON_UNMETERED_NETWORK};
            Log.d(CLASS_NAME, "startSyncJob: using just wifi");
        }

        JobTrigger.ExecutionWindowTrigger timeWindow;
        if (!restart)
            timeWindow = Trigger.executionWindow(5, 10);
        else
            timeWindow = Trigger.executionWindow(30, 50);

        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
//                .setService(SyncService.class)
                .setService(UploadService.class)
                // uniquely identifies the job
                .setTag(JOB_TAG)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTrigger(timeWindow)
                // overwrite an existing job with the same tag - this assures that just one job is running at a time:
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        constraints
                )
                .build();




        dispatcher.mustSchedule(syncJob);

    }

    public static void cancel(Context context) {

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancelAll();

    }


    public static void login(Context context, LoginRequest.LoginCallback loginCallback) {

        if (User.getInstance().isLoggedIn())
            return;

        boolean isUserSaved = UserHandler.loadCredentials(context);
        if (isUserSaved) {

            if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS) {
                RequestHandler.createRequest(context, RequestHandler.REQUEST_LOGIN);
                User.getInstance().setAutoLogInDone(true);
            }
            else if (User.getInstance().getConnection() == SYNC_DROPBOX) {
                DropboxUtils.getInstance().loginToDropbox(loginCallback, User.getInstance().getDropboxToken());
                User.getInstance().setAutoLogInDone(true);
            }
        }


    }

    public static String getConnectionText(Context context, int connection) {

        switch (connection) {
            case SYNC_TRANSKRIBUS:
                return context.getResources().getString(R.string.sync_transkribus_text);
            case SYNC_DROPBOX:
                return context.getResources().getString(R.string.sync_dropbox_text);
        }

        return null;

    }
}
