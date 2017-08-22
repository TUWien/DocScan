package at.ac.tuwien.caa.docscan.sync;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 18.08.2017.
 * Based on: @see <a href="https://developer.android.com/guide/components/services.html#ExtendingService"/>
 */

public class SyncService extends JobService {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

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

            Log.d(this.getClass().getName(), "Handling message: " + msg);

            for (SyncInfo.FileSync fileSync : SyncInfo.getInstance().getSyncList()) {

                if (fileSync.getState() == SyncInfo.FileSync.STATE_NOT_UPLOADED) {
                    Log.d(this.getClass().getName(), "uploading file: " + fileSync.getFile().getName());
                    fileSync.setState(SyncInfo.FileSync.STATE_AWAITING_UPLOAD);

                    if (User.getInstance().getConnection() == User.SYNC_DROPBOX)
                        DropboxUtils.getInstance().uploadFile(this, fileSync.getFile());
                }

            }

//            // Normally we would do some work here, like download a file.
//            // For our sample, we just sleep for 5 seconds.
//            try {
////                TODO: perform here the upload
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                // Restore interrupt status.
//                Thread.currentThread().interrupt();
//            }
//            // Stop the service using the startId, so that we don't stop
//            // the service in the middle of handling another job
//            stopSelf(msg.arg1);
        }

        @Override
        public void onUploadComplete(FileMetadata result) {
            Log.d(this.getClass().getName(), "uploaded file: " + result.getName());
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

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
//
//        // For each start request, send a message to start a job and deliver the
//        // start ID so we know which request we're stopping when we finish the job
//        Message msg = mServiceHandler.obtainMessage();
//        msg.arg1 = startId;
//        mServiceHandler.sendMessage(msg);
//
//        // If we get killed, after returning from here, restart
//        return START_STICKY;
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // We don't provide binding, so return null
//        return null;
//    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

}
