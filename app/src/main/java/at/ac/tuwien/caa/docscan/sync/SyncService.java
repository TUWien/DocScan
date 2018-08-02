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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.UploadStatusRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;


/**
 * Created by fabian on 18.08.2017.
 * Based on: @see <a href="https://developer.android.com/guide/components/services.html#ExtendingService"/>
 */

public class SyncService extends JobService implements
        LoginRequest.LoginCallback,
        CollectionsRequest.CollectionsCallback,
        CreateCollectionRequest.CreateCollectionCallback,
        StartUploadRequest.StartUploadCallback,
        TranskribusUtils.TranskribusUtilsCallback,
        UploadStatusRequest.UploadStatusCallback {


    public static final String INTENT_UPLOAD_ACTION = "INTENT_UPLOAD_ACTION";

    public static final String UPLOAD_INTEND_TYPE = "UPLOAD_INTEND_TYPE";
    public static final String UPLOAD_FINISHED_ID = "UPLOAD_FINISHED_ID";
    public static final String UPLOAD_OFFLINE_ERROR_ID = "UPLOAD_OFFLINE_ERROR_ID";
    public static final String UPLOAD_FILE_DELETED_ERROR_ID = "UPLOAD_FILE_DELETED_ERROR_ID";
    public static final String UPLOAD_ERROR_ID = "UPLOAD_ERROR_ID";

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
    private static final int NOTIFICATION_FILE_DELETED = 3;

    public static final String SERVICE_ALONE_KEY = "SERVICE_ALONE_KEY";
    private static final String CLASS_NAME = "SyncService";


    private int mFilesNum;
    private int mFilesUploaded;
    private boolean mIsInterrupted;


    @Override
    public boolean onStartJob(JobParameters job) {

        Log.d("SyncService", "================= service starting =================");

        mIsInterrupted = false;

        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "================= service starting =================");

        // Check if the app is active, if not read the physical file about the upload status:
        if (SyncInfo.isInstanceNull()) {
            SyncInfo.readFromDisk(getApplicationContext());
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "loaded SyncInfo from disk");
            Log.d("SyncService", "loaded SyncInfo from disk");
        } else {
            Log.d("SyncService", "SyncInfo is in RAM");
            SyncInfo.getInstance().printUnfinishedUploadIDs();
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "SyncInfo is in RAM");
        }

//        First check if the User is already logged in:
        if (!User.getInstance().isLoggedIn()) {
//            Log in if necessary:
            Log.d(CLASS_NAME, "login...");
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "login...");
            SyncUtils.login(this, this);
        } else {
            Log.d(CLASS_NAME, "user is logged in");
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "user is logged in");
            startUpload();

        }


        return false; // Answers the question: "Is there still work going on?"
    }

    private void startUpload() {

        TranskribusUtils.getInstance().startUpload(this);

//        if (SyncInfo.getInstance().getUploadDirs() != null && !SyncInfo.getInstance().getUploadDirs().isEmpty())
//    //        Start the upload of user selected dirs:
//            TranskribusUtils.getInstance().uploadDocuments(this, SyncInfo.getInstance().getUploadDirs());
//
//        if (SyncInfo.getInstance().getUnfinishedUploadIDs() != null &&
//                !SyncInfo.getInstance().getUnfinishedUploadIDs().isEmpty())
//            TranskribusUtils.getInstance().startFindingUnfinishedUploads(this,
//                    SyncInfo.getInstance().getUnfinishedUploadIDs());


    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.d(getClass().getName(), "onStopJob");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onStopJob");
        return false;
    }

    @Override
    public void onLogin(User user) {

        Log.d(CLASS_NAME, "onLogin");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onLogin");

//        Starts the upload:
//        TranskribusUtils.getInstance().uploadDocuments(this, SyncInfo.getInstance().getUploadDirs());
        startUpload();

    }

    @Override
    public void onLoginError() {

        Log.d(getClass().getName(), "onLoginError");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME, "onLoginError");

    }

    @Override
    public void onCollections(List<Collection> collections) {

        Log.d(CLASS_NAME, "onCollections");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onCollections");

        TranskribusUtils.getInstance().onCollections(collections);

    }

    @Override
    public void onCollectionCreated(String collName) {

        Log.d(CLASS_NAME, "onCollectionCreated");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onCollectionCreated");

        TranskribusUtils.getInstance().onCollectionCreated(collName);

    }

    @Override
    public void onUploadStart(int uploadId, String title) {

        Log.d(CLASS_NAME, "onNewUploadIDReceived");

        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onNewUploadIDReceived");

        TranskribusUtils.getInstance().onNewUploadIDReceived(uploadId, title);

    }

    @Override
    public void onFilesPrepared() {

        Log.d(CLASS_NAME, "onFilesPrepared");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onFilesPrepared");

        uploadFiles();

    }

    @Override
    public void onFilesDeleted() {

        Log.d(CLASS_NAME, "onFilesDeleted");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME, "onFilesDeleted");
        showNotification();
        updateNotification(NOTIFICATION_FILE_DELETED);
        sendFileDeletedErrorIntent();



    }

    private void sendFileDeletedErrorIntent() {

        Log.d(CLASS_NAME, "sendFileDeletedErrorIntent:");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "sendFileDeletedErrorIntent");

        Intent intent = new Intent(INTENT_UPLOAD_ACTION);
        intent.putExtra(UPLOAD_INTEND_TYPE, UPLOAD_FILE_DELETED_ERROR_ID);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

    }


//    @Override
//    public void onSelectedFilesPrepared() {
//
//        Log.d(CLASS_NAME, "onSelectedFilesPrepared");
//        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onSelectedFilesPrepared");
//
//        mAreSelectedFilesPrepared = true;
//        if (mAreUnfinishedFilesPrepared)
//            // Start the upload:
//            uploadFiles();
//
//    }
//
//    @Override
//    public void onUnfinishedFilesPrepared() {
//
//        Log.d(CLASS_NAME, "onUnfinishedFilesPrepared");
//        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onUnfinishedFilesPrepared");
//
//        mAreUnfinishedFilesPrepared = true;
//        if (mAreSelectedFilesPrepared)
//            // Start the upload:
//            uploadFiles();
//
//    }

    private void uploadFiles() {

        mIsInterrupted = false;

        Message msg = mServiceHandler.obtainMessage();
        mServiceHandler.sendMessage(msg);

    }

    @Override
    public void handleRestError(RestRequest request, VolleyError error) {

//        Log the error:

//        Log error.getMessage: (but this is usually null)
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                "handleRestError: RestRequest: " + request);
        Log.d(CLASS_NAME, "handleRestError: RestRequest: " + request);

//        Log error.getMessage: (but this is usually null)
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                "handleRestError: error.getMessage: " + error.getMessage());
        Log.d(CLASS_NAME, "handleRestError: error.getMessage: " + error.getMessage());

//        Log the status code:
        String statusCode = String.valueOf(error.networkResponse.statusCode);
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                "handleRestError: statusCode: " + statusCode);
        Log.d(CLASS_NAME, "handleRestError: statusCode: " + statusCode);

//        Log the network response:
        String networkResponse = getNetworkResponse(error);
        if (networkResponse != null) {
            DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                    "handleRestError: networkResponse: " + networkResponse);
            Log.d(CLASS_NAME, "handleRestError: networkResponse: " + networkResponse);
        }

        if (request instanceof UploadStatusRequest) {
//            TODO: remove maybe?
        }


        handleRestUploadError();

    }

    private String getNetworkResponse(VolleyError error) {

        try {
            String body = new String(error.networkResponse.data, "UTF-8");
            return body;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
//            This should not be null, because we should use the proper encoding:
            return null;
        }

    }

    /**
     * Handles errors that occur before the first file is uploaded.
     */
    private void handleRestUploadError() {



        User.getInstance().setLoggedIn(false);

        Log.d(getClass().getName(), "handleRestUploadError");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME, "handleRestUploadError");

        updateNotification(NOTIFICATION_ERROR);
        sendOfflineErrorIntent();


        SyncInfo.saveToDisk(getApplicationContext());
//        Schedule the upload job:
//        SyncInfo.startSyncJob(getApplicationContext());
        SyncInfo.restartSyncJob(getApplicationContext());


    }


    // Send an Intent:
    private void sendOfflineErrorIntent() {

        Log.d("sender", "Broadcasting message");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "Broadcasting message");

        Intent intent = new Intent(INTENT_UPLOAD_ACTION);
        intent.putExtra(UPLOAD_INTEND_TYPE, UPLOAD_OFFLINE_ERROR_ID);
//        intent.putExtra(UPLOAD_OFFLINE_ERROR_ID, true);

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

    @Override
    public void onStatusReceived(int uploadID, String title, ArrayList<String> unfinishedFileNames) {

        TranskribusUtils.getInstance().onUnfinishedUploadStatusReceived(getApplicationContext(),
                uploadID, title, unfinishedFileNames);

    }

    /**
     * This is just called if an upload was already finished before the SyncService job was started.
     * Hence we need to take a look if TranskribusUtils is waiting for this upload id, because
     * otherwise it will starve.
     * @param uploadID
     */
    @Override
    public void onUploadAlreadyFinished(int uploadID) {

//        TranskribusUtils.getInstance().removeFromUnprocessedList(uploadID);
        TranskribusUtils.getInstance().removeFromUnfinishedListAndCheckJob(uploadID);

    }


    // Handler that receives messages from the thread
    protected final class ServiceHandler extends Handler implements SyncInfo.Callback {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            showNotification();

            Log.d(CLASS_NAME, "handlemessage");
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "handlemessage");

            mFilesUploaded = 0;

//            // Show all files:
            printSyncInfo();

            mFilesNum = getFilesNum();

            Log.d(CLASS_NAME, "handleMessage: mFilesNum: " + mFilesNum);
            DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME, "handleMessage: mFilesNum: " + mFilesNum);
            if (mFilesNum == 0)
                return;

            // Start with the first file:
            SyncInfo.FileSync fileSync = getNextUpload();
            if (fileSync != null)
                uploadFile(fileSync);

        }


        // Send an Intent.
        private void sendFinishedIntent() {

            Log.d(CLASS_NAME, "sendFinishedIntent:");
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "finish intend");

            Intent intent = new Intent(INTENT_UPLOAD_ACTION);
            intent.putExtra(UPLOAD_INTEND_TYPE, UPLOAD_FINISHED_ID);
//            intent.putExtra(UPLOAD_FINISHED_ID, true);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }


        // Send an Intent
        private void sendOfflineErrorIntent() {

            Log.d("sender", "sendOfflineErrorIntent");
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "sendOfflineErrorIntent");

            Intent intent = new Intent(INTENT_UPLOAD_ACTION);
            intent.putExtra(UPLOAD_INTEND_TYPE, UPLOAD_OFFLINE_ERROR_ID);
//            intent.putExtra(UPLOAD_OFFLINE_ERROR_ID, true);

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

//            Log.d("SyncService", "uploaded file: " + fileSync.getFile().getPath());
            DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "uploaded file: " + fileSync.getFile().getPath());

            fileSync.setState(SyncInfo.FileSync.STATE_UPLOADED);

            SyncInfo.getInstance().addToUploadedList(fileSync);

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

        }

        /**
         * This occurs during file upload and is thrown by TranskribusUtils.uploadFile.
         * @param e
         */
        @Override
        public void onError(Exception e) {

            Log.d(CLASS_NAME, "onError: unfinished upload ids size: "
                    + SyncInfo.getInstance().getUnfinishedUploadIDs().size());

            DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                    "onError: unfinished upload ids size: "
                            + SyncInfo.getInstance().getUnfinishedUploadIDs().size());

            TranskribusUtils.getInstance().onError();

            SyncInfo.restartSyncJob(getApplicationContext());

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

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {
                DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", fileSync.toString());
            }

        }

    }


    @Override
    public void onCreate() {

        Log.d(CLASS_NAME, "oncreate");
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "oncreate");

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
        DataLog.getInstance().writeUploadLog(getApplicationContext(), "SyncService", "onDestroy");

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


        Log.d(CLASS_NAME, "updateNotification: unfinished upload ids size: "
                + SyncInfo.getInstance().getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                "updateNotification: unfinished upload ids size: "
                + SyncInfo.getInstance().getUnfinishedUploadIDs().size());

        if (mNotificationBuilder == null)
            return;

        switch (notificationID) {

            case NOTIFICATION_ERROR:
                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_error_title))
                        .setContentText(getString(R.string.sync_notification_error_text))
                        // Removes the progress bar
                        .setProgress(0, 0, false);
                mIsInterrupted = true;
                break;
            case NOTIFICATION_PROGRESS_UPDATE:
                int progress = (int) Math.floor(mFilesUploaded / (double) mFilesNum * 100);

                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_title))
                        .setContentText(getString(R.string.sync_notification_uploading_transkribus_text))
                        .setProgress(100, progress, false);
                break;
            case NOTIFICATION_FILE_DELETED:
                mNotificationBuilder
                        .setContentTitle(getString(R.string.sync_notification_stoped_title))
                        .setContentText(getString(R.string.sync_notification_stoped_text))
                        // Removes the progress bar
                        .setProgress(0, 0, false);
                break;
            case NOTIFICATION_SUCCESS:
                Log.d(CLASS_NAME, "updateNotification: NOTIFICATION_SUCCESS");
                DataLog.getInstance().writeUploadLog(getApplicationContext(), CLASS_NAME,
                        "updateNotification: NOTIFICATION_SUCCESS");
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
