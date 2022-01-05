package at.ac.tuwien.caa.docscan.logic.notification

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R

/**
 * Represents all system notification channels which are introduced by this app.
 */
enum class DocScanNotificationChannel(
        val channelId: String,
        val channelNameResource: Int,
        val channelDescriptionResource: Int,
        val priority: Int,
        val importance: Int, // is used for channels at api lvl 26
        val isAutoCancel: Boolean,
        val isActive: Boolean
) {

    CHANNEL_EXPORT(BuildConfig.APPLICATION_ID + ".exportchannel",
            R.string.notification_channel_exports_title,
            R.string.notification_channel_exports_text,
            @Suppress("DEPRECATION")
            Notification.PRIORITY_DEFAULT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationManager.IMPORTANCE_DEFAULT else -1,
            true,
            true),

    CHANNEL_UPLOAD(BuildConfig.APPLICATION_ID + ".uploadchannel",
            R.string.notification_channel_uploads_title,
            R.string.notification_channel_uploads_text,
            @Suppress("DEPRECATION")
            Notification.PRIORITY_DEFAULT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationManager.IMPORTANCE_HIGH else -1,
            false,
            false);

}
