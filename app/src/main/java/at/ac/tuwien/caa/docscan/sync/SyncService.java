package at.ac.tuwien.caa.docscan.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.VolleyError;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.io.File;
import java.util.ArrayList;
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
import static at.ac.tuwien.caa.docscan.ui.syncui.UploadingActivity.UPLOAD_OFFLINE_ERROR_ID;
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
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private int mNotifyID = 68;

    private static final String CHANNEL_ID = "docscan_channel";
    private static final CharSequence CHANNEL_NAME = "DocScan Channel";// The user-visible name of the channel.

    // constants for the notifications:
    private static final int NOTIFICATION_PROGRESS_UPDATE = 0;
    private static final int NOTIFICATION_ERROR = 1;
    private static final int NOTIFICATION_SUCCESS = 2;

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
            TranskribusUtils.getInstance().startUpload(this, SyncInfo.getInstance().getUploadDirs());

        }


        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.d(getClass().getName(), "onStopJob");
        return false;
    }

    @Override
    public void onLogin(User user) {

        Log.d(TAG, "onLogin");

//        Starts the upload:
        TranskribusUtils.getInstance().startUpload(this, SyncInfo.getInstance().getUploadDirs());

    }

    @Override
    public void onLoginError() {

    }

    @Override
    public void onCollections(List<Collection> collections) {

        Log.d(TAG, "onCollections");

        TranskribusUtils.getInstance().onCollections(collections);

    }

    @Override
    public void onCollectionCreated(String collName) {

        Log.d(TAG, "onCollectionCreated");

        TranskribusUtils.getInstance().onCollectionCreated(collName);

    }

    @Override
    public void onUploadStart(int uploadId, String title) {

        Log.d(TAG, "onUploadStart");

        TranskribusUtils.getInstance().onUploadStart(uploadId, title);

    }

    @Override
    public void onFilesPrepared() {

        Log.d(TAG, "onFilesPrepared");

//         Start the upload:
        Message msg = mServiceHandler.obtainMessage();
        mServiceHandler.sendMessage(msg);

    }

    @Override
    public void handleRestError(VolleyError error) {

        handleRestUploadError();

    }

    /**
     * Handles errors that occur before the first file is uploaded.
     */
    private void handleRestUploadError() {

//        Log.d(getClass().getName(), "onError");
//
//        //        Collect the files that are not uploaded yet and get their paths:
//        ArrayList<File> unfinishedDirs = getUnfinishedUploadDirs();
//
//        // In the case of an error this directory list should not be empty:
//        if (unfinishedDirs == null || unfinishedDirs.isEmpty())
//            return;
//
//        SyncInfo.getInstance().setUploadDirs(unfinishedDirs);

        SyncInfo.saveToDisk(getApplicationContext());
//        Schedule the upload job:
        SyncInfo.startSyncJob(getApplicationContext());

        updateNotification(NOTIFICATION_ERROR);

        sendOfflineErrorIntent();

    }

//    private void updateNotification(int notificationID) {
//
//        switch (notificationID) {
//
//            case NOTIFICATION_ERROR:
//                mNotificationBuilder
//                        .setContentTitle(getString(R.string.sync_notification_error_title))
//                        .setContentText(getString(R.string.sync_notification_error_text))
//                        // Removes the progress bar
//                        .setProgress(0, 0, false);
//                break;
//            case NOTIFICATION_PROGRESS_UPDATE:
//                int progress = (int) Math.floor(mFilesUploaded / (double) mFilesNum * 100);
//                mNotificationBuilder
//                        .setContentTitle(getString(R.string.sync_notification_title))
//                        .setContentText(getString(R.string.sync_notification_uploading_transkribus_text))
//                        .setProgress(100, progress, false);
//                break;
//            case NOTIFICATION_SUCCESS:
//                mNotificationBuilder
//                        .setContentTitle(getString(R.string.sync_notification_uploading_finished_title))
//                        .setContentText(getString(R.string.sync_notification_uploading_finished_text))
//                        // Removes the progress bar
//                        .setProgress(0, 0, false);
//                break;
//
//        }
//
//        // show the new notification:
//        mNotificationManager.notify(mNotifyID, mNotificationBuilder.build());
//
//    }

    // Send an Intent with an action named "PROGRESS_INTENT_NAME".
    private void sendOfflineErrorIntent() {

        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("PROGRESS_INTENT_NAME");
        intent.putExtra(UPLOAD_OFFLINE_ERROR_ID, true);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Searches for the unfinished uploads and returns a list of unique directory paths that could
     * not be uploaded.
     * @return
     */
    protected ArrayList<File> getUnfinishedUploadDirs() {

        ArrayList<File> unfinishedDirs = new ArrayList<>();

        for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
            if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED) {
                File parentPath = fileSync.getFile().getParentFile();
                if (!unfinishedDirs.contains(parentPath))
                    unfinishedDirs.add(parentPath);
            }
        }

        return unfinishedDirs;

    }



//
//    @Override
//    public void handleRestError(VolleyError error) {
//
//        Log.d(getClass().getName(), "onError");
//
//        //        Collect the files that are not uploaded yet and get their paths:
//        ArrayList<File> unfinishedDirs = getUnfinishedUploadDirs();
//
//        // In the case of an error this directory list should not be empty:
//        if (unfinishedDirs == null || unfinishedDirs.isEmpty())
//            return;
//
//        SyncInfo.getInstance().setUploadDirs(unfinishedDirs);
//        SyncInfo.startSyncJob(getApplicationContext());
//
//        updateNotification(NOTIFICATION_ERROR);
//
//        sendOfflineErrorIntent();
//
//    }


    // Handler that receives messages from the thread
    protected final class ServiceHandler extends Handler implements SyncInfo.Callback {

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
        private void sendOfflineErrorIntent() {

            Log.d("sender", "Broadcasting message");
            Intent intent = new Intent("PROGRESS_INTENT_NAME");
            intent.putExtra(UPLOAD_OFFLINE_ERROR_ID, true);

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

            Log.d("SyncService", "uploaded file: " + fileSync.getFile().getPath());
            fileSync.setState(SyncInfo.FileSync.STATE_UPLOADED);

            mFilesUploaded++;
//            updateProgressbar();

            updateNotification(NOTIFICATION_PROGRESS_UPDATE);

            SyncInfo.FileSync nextFileSync = getNextUpload();
            if (nextFileSync != null)
                uploadFile(nextFileSync);
            else
                uploadsFinished();

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

            updateNotification(NOTIFICATION_SUCCESS);

            // Notify the SyncActivity:
            sendFinishedIntent();

            User.getInstance().setUploadActive(false);

        }


        /**
         * This occurs during file upload and is thrown by TranskribusUtils.uploadFile.
         * @param e
         */
        @Override
        public void onError(Exception e) {

//            handleRestUploadError();

            Log.d(getClass().getName(), "onError");

            //        Collect the files that are not uploaded yet and get their paths:
            ArrayList<File> unfinishedDirs = getUnfinishedUploadDirs();

            // In the case of an error this directory list should not be empty:
            if (unfinishedDirs == null || unfinishedDirs.isEmpty())
                return;

            SyncInfo.getInstance().setUploadDirs(unfinishedDirs);
            SyncInfo.startSyncJob(getApplicationContext());

            updateNotification(NOTIFICATION_ERROR);

            sendOfflineErrorIntent();

        }

        /**
         * Searches for the unfinished uploads and returns a list of unique directory paths that could
         * not be uploaded.
         * @return
         */
        protected ArrayList<File> getUnfinishedUploadDirs() {

            ArrayList<File> unfinishedDirs = new ArrayList<>();

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED) {
                    File parentPath = fileSync.getFile().getParentFile();
                    if (!unfinishedDirs.contains(parentPath))
                        unfinishedDirs.add(parentPath);
                }
            }

            return unfinishedDirs;

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

        Log.d(getClass().getName(), "onDestroy");

    }

    private void showNotification() {

        String title = getString(R.string.sync_notification_title);

        String text = getConnectionText();
        String CHANNEL_ID = "docscan_notification_channel";// The id of the channel.

        mNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_docscan_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setChannelId(CHANNEL_ID);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        // On Android O we need a NotificationChannel, otherwise the notification is not shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_LOW disables the notification sound:
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

    }

    private void updateNotification(int notificationID) {

        if (mNotificationBuilder == null)
            return;

        switch (notificationID) {

            case NOTIFICATION_ERROR:
                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_error_title))
                        .setContentText(getString(R.string.sync_notification_error_text))
                        // Removes the progress bar
                        .setProgress(0, 0, false);
                break;
            case NOTIFICATION_PROGRESS_UPDATE:
                int progress = (int) Math.floor(mFilesUploaded / (double) mFilesNum * 100);
                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_title))
                        .setContentText(getString(R.string.sync_notification_uploading_transkribus_text))
                        .setProgress(100, progress, false);
                break;
            case NOTIFICATION_SUCCESS:
                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_uploading_finished_title))
                        .setContentText(getString(R.string.sync_notification_uploading_finished_text))
                        // Removes the progress bar
                        .setProgress(0, 0, false);
                break;

        }

        // show the new notification:
        mNotificationManager.notify(mNotifyID, mNotificationBuilder.build());

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
