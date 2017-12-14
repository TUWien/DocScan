package at.ac.tuwien.caa.docscan.sync;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_ERROR_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_FINISHED_ID;
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_PROGRESS_ID;

/**
 * Created by fabian on 18.08.2017.
 * Based on: @see <a href="https://developer.android.com/guide/components/services.html#ExtendingService"/>
 */

public class SyncService extends JobService implements
        LoginRequest.LoginCallback,
        CollectionsRequest.CollectionsCallback,
        CreateCollectionRequest.CreateCollectionCallback,
        StartUploadRequest.StartUploadCallback,
        TranskribusUtils.TranskribusUtilsCallback {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private int mNotifyID = 68;


    public static final String SERVICE_ALONE_KEY = "SERVICE_ALONE_KEY";
    private static final String TAG = "SyncService";
    private static final String PROGRESS_INTENT_NAME = "PROGRESS_INTENT";


    private int mFilesNum;
    private int mFilesUploaded;

    @Override
    public boolean onStartJob(JobParameters job) {

        Log.d("SyncService", "================= service starting =================");


        // Check if the app is active, if not read the physical file about the upload status:
        if (SyncInfo.isInstanceNull()) {
            SyncInfo.readFromDisk(getApplicationContext());
            Log.d("SyncService", "loaded SyncInfo from disk");
        } else
            Log.d("SyncService", "SyncInfo is in RAM");


//        First check if the User is already logged in:
        if (!User.getInstance().isLoggedIn()) {
//            Log in if necessary:
            Log.d(TAG, "login...");
            SyncUtils.login(this, this);
        } else {
            Log.d(TAG, "user is logged in");
//            Start the upload:
//            Message msg = mServiceHandler.obtainMessage();
//            mServiceHandler.sendMessage(msg);

//            new CollectionsRequest(this);

            TranskribusUtils.getInstance().startUpload(this, SyncInfo.getInstance().getUploadDirs());

        }


        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    @Override
    public void onLogin(User user) {

        Log.d(TAG, "onlogin");

////        Starts the upload:
//        Message m = mServiceHandler.obtainMessage();
//        mServiceHandler.sendMessage(m);
        TranskribusUtils.getInstance().startUpload(this, SyncInfo.getInstance().getUploadDirs());

    }

    @Override
    public void onLoginError() {

    }

    @Override
    public void onCollections(List<Collection> collections) {

        TranskribusUtils.getInstance().onCollections(collections);

    }

    @Override
    public void onCollectionCreated(String collName) {

        TranskribusUtils.getInstance().onCollectionCreated(collName);

    }

    @Override
    public void onUploadStart(int uploadId, String title) {

        TranskribusUtils.getInstance().onUploadStart(uploadId, title);

    }

    @Override
    public void onFilesPrepared() {

//         Start the upload:
        Message msg = mServiceHandler.obtainMessage();
        mServiceHandler.sendMessage(msg);

    }


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler implements SyncInfo.Callback {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            showNotification();

            Log.d(TAG, "handlemessage");

            mFilesUploaded = 0;

//            // Show all files:
            printSyncInfo();

            mFilesNum = getFilesNum();

            if (mFilesNum == 0)
                return;

            User.getInstance().setUploadActive(true);

            // Start with the first file:
            SyncInfo.FileSync fileSync = getNextUpload();
            if (fileSync != null)
                uploadFile(fileSync);

        }

        // Send an Intent with an action named "PROGRESS_INTENT_NAME".
        private void sendProgressIntent(int progress) {

            Log.d("sender", "Broadcasting message");
            Intent intent = new Intent("PROGRESS_INTENT_NAME");
            intent.putExtra(UPLOAD_PROGRESS_ID, progress);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        // Send an Intent with an action named "PROGRESS_INTENT_NAME".
        private void sendFinishedIntent() {

            Log.d("sender", "Broadcasting message");
            Intent intent = new Intent("PROGRESS_INTENT_NAME");
            intent.putExtra(UPLOAD_FINISHED_ID, true);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        // Send an Intent with an action named "PROGRESS_INTENT_NAME".
        private void sendErrorIntent() {

            Log.d("sender", "Broadcasting message");
            Intent intent = new Intent("PROGRESS_INTENT_NAME");
            intent.putExtra(UPLOAD_ERROR_ID, true);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        private void uploadFile(SyncInfo.FileSync fileSync) {

            if (fileSync == null)
                return;

            fileSync.setState(SyncInfo.FileSync.STATE_AWAITING_UPLOAD);

            if (User.getInstance().getConnection() == User.SYNC_DROPBOX)
                DropboxUtils.getInstance().uploadFile(this, fileSync);
            else if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS)
                TranskribusUtils.getInstance().uploadFile(this, getApplicationContext(),
                        (SyncInfo.TranskribusFileSync) fileSync);


        }

        private int getFilesNum() {

            int result = 0;
            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED)
                    result++;
            }

            return result;
        }

        @Override
        public void onUploadComplete(SyncInfo.FileSync fileSync) {

            Log.d("SyncService", "uploaded file: " + fileSync.getFile().getName());
            fileSync.setState(SyncInfo.FileSync.STATE_UPLOADED);

            mFilesUploaded++;
            updateProgressbar();

            SyncInfo.FileSync nextFileSync = getNextUpload();
            if (nextFileSync != null)
                uploadFile(nextFileSync);
            else
                uploadsFinished();

        }

        private void updateProgressbar() {
            int progress = (int) Math.floor(mFilesUploaded / (double) mFilesNum * 100);
            mBuilder.setProgress(100, progress, false);
            mNotificationManager.notify(mNotifyID, mBuilder.build());

            sendProgressIntent(progress);
        }

        private void uploadsFinished() {

            SyncInfo.getInstance().setUploadDirs(null);

            SyncInfo.saveToDisk(getApplicationContext());

            // Show the finished progressbar for a short time:
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//                Notify that the upload is finished:
            mBuilder.setContentText(getString(R.string.sync_notification_uploading_finished_text))
                    // Removes the progress bar
                    .setProgress(0, 0, false);
            mNotificationManager.notify(mNotifyID, mBuilder.build());

            // Notify the SyncActivity:
            sendFinishedIntent();

            User.getInstance().setUploadActive(false);

        }

        @Override
        public void onError(Exception e) {

            TranskribusUtils.getInstance().onError();

            sendErrorIntent();

        }

        private SyncInfo.FileSync getNextUpload() {

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED)
                    return fileSync;
            }

            return null;

        }

        private void printSyncInfo() {

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList())
                Log.d(TAG, "FileSync: " + fileSync);

        }

    }


    @Override
    public void onCreate() {

        Log.d(TAG, "oncreate");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }


    @Override
    public void onDestroy() {
    }

    private void showNotification() {

        String title = getString(R.string.sync_notification_title);

        String text = getConnectionText();

        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_statusbar_icon)
                .setContentTitle(title)
                .setContentText(text);
//// Creates an explicit intent for an Activity in your app
//        Intent resultIntent = new Intent(this, SyncActivity.class);
//
//// The stack builder object will contain an artificial back stack for the
//// started Activity.
//// This ensures that navigating backward from the Activity leads out of
//// your app to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//// Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(SyncActivity.class);
//// Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );
//        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Show the notification every time (just for debugging purposes).
//        TODO: remove
//        mNotificationManager.notify(mNotifyID, mBuilder.build());

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
