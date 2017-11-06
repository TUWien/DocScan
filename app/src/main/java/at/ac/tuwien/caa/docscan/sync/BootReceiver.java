package at.ac.tuwien.caa.docscan.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by fabian on 24.10.2017.
 */

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "Boot Receiver:::";
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

                SyncInfo.readFromDisk(context);

                if (SyncInfo.getInstance().getUploadDirs() != null && SyncInfo.getInstance().getUploadDirs().size() > 0)
                    SyncInfo.startSyncJob(context);

                //Boot Receiver Called
            }
        }
    }
}