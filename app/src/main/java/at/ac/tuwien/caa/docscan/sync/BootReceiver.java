package at.ac.tuwien.caa.docscan.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by fabian on 24.10.2017.
 */

public class BootReceiver extends BroadcastReceiver {

    private static final String CLASS_NAME = "BootReceiver";
    /*
     * (non-Javadoc)
     *
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            if (intent.getAction().equalsIgnoreCase(
                    Intent.ACTION_BOOT_COMPLETED)) {

                Log.d(CLASS_NAME, "onReceive");

                SyncInfo.readFromDisk(context);

                if ((SyncInfo.getInstance().getUploadDirs() != null) &&
                        (SyncInfo.getInstance().getUploadDirs().size() > 0)) {
                    Log.d(CLASS_NAME, "upload dirs are not empty. starting sync job.");
                    SyncInfo.startSyncJob(context);
                }
                else if ((SyncInfo.getInstance().getUnfinishedUploadIDs() != null) &&
                    (SyncInfo.getInstance().getUnfinishedUploadIDs().size() > 0)) {
                    Log.d(CLASS_NAME, "unfinished ids are not empty. starting sync job.");
                    SyncInfo.startSyncJob(context);
                }

            }
        }
    }
}