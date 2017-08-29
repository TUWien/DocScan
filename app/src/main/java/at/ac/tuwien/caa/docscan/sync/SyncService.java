package at.ac.tuwien.caa.docscan.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;
import at.ac.tuwien.caa.docscan.ui.AboutActivity;

/**
 * Created by fabian on 18.08.2017.
 * Based on: @see <a href="https://developer.android.com/guide/components/services.html#ExtendingService"/>
 */

public class SyncService extends JobService {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private int mNotifyID = 68;

    private int mFilesNum;
    private int mFilesUploaded;

    @Override
    public boolean onStartJob(JobParameters job) {

        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
//        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }



    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler implements DropboxUtils.Callback {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            showNotification();

            Log.d(this.getClass().getName(), "Handling message: " + msg);

            if (SyncInfo.isInstanceNull())
                SyncInfo.readFromDisk(getApplicationContext());

            mFilesUploaded = 0;
//            TODO: think about a better way to get the not uploaded images:
            mFilesNum = 0;
            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED)
                    mFilesNum++;
            }

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {

                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED) {
                    Log.d(this.getClass().getName(), "uploading file: " + fileSync.getFile().getName());
                    fileSync.setState(SyncInfo.FileSync.STATE_AWAITING_UPLOAD);

                    if (User.getInstance().getConnection() == User.SYNC_DROPBOX)
                        DropboxUtils.getInstance().uploadFile(this, fileSync);
                }
                else if (fileSync.getState() == SyncInfo.FileSync.STATE_UPLOADED)
                    Log.d(this.getClass().getName(), "already uploaded: " + fileSync.getFile().getName());

            }

        }

        @Override
        public void onUploadComplete(FileMetadata result) {

            mFilesUploaded++;
            int progress = (int) Math.floor(mFilesUploaded / (double) mFilesNum * 100);
            mBuilder.setProgress(100, progress, false);
            mNotificationManager.notify(mNotifyID, mBuilder.build());

            if (mFilesUploaded == mFilesNum) {
                // Show the finished progressbar for a short time:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Notify that the upload is finished:
                mBuilder.setContentText(getString(R.string.sync_notification_uploading_finished_text))
                        // Removes the progress bar
                        .setProgress(0,0,false);
                mNotificationManager.notify(mNotifyID, mBuilder.build());
            }

        }

        @Override
        public void onError(Exception e) {

        }
    }



    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void showNotification() {

        String title = getString(R.string.sync_notification_title);

        String text = getConnectionText();

        mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_statusbar_icon)
                    .setContentTitle(title)
                    .setContentText(text);
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, AboutActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(AboutActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

// mNotificationId is a unique integer your app uses to identify the
// notification. For example, to cancel the notification, you can pass its ID
// number to NotificationManager.cancel().


    }

    private String getConnectionText() {

        String text = "";
        int connection = UserHandler.loadConnection(this);
        if (connection == User.SYNC_TRANSKRIBUS)
            text = getString(R.string.sync_notification_uploading_transkribus_text);
        else if (connection == User.SYNC_DROPBOX)
            text = getString(R.string.sync_notification_uploading_dropbox_text);

        return text;

    }

}
