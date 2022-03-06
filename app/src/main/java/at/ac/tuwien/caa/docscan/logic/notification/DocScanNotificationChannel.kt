package at.ac.tuwien.caa.docscan.logic.notification

import androidx.core.app.NotificationManagerCompat
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R

/**
 * Represents all system notification channels which are introduced by this app.
 */
enum class DocScanNotificationChannel(
    val tag: String,
    val channelId: String,
    val channelNameResource: Int,
    val channelDescriptionResource: Int,
    val importance: Int,
    val isAutoCancel: Boolean
) {

    CHANNEL_EXPORT(
        "EXPORT_NOTIFICATION",
        BuildConfig.APPLICATION_ID + ".exportchannel",
        R.string.notification_channel_exports_title,
        R.string.notification_channel_exports_text,
        @Suppress("DEPRECATION")
        NotificationManagerCompat.IMPORTANCE_DEFAULT,
        true
    ),

    CHANNEL_UPLOAD(
        "UPLOAD_NOTIFICATION",
        BuildConfig.APPLICATION_ID + ".uploadchannel",
        R.string.notification_channel_uploads_title,
        R.string.notification_channel_uploads_text,
        @Suppress("DEPRECATION")
        NotificationManagerCompat.IMPORTANCE_DEFAULT,
        false
    );
}

/**
 * Represents all deprecated and unused notification channels.
 */
enum class DeprecatedNotificationChannels(val tag: String, val channelId: String) {
    PDF_CHANNEL("DocScan Pdf", "PDF_CHANNEL_ID")
}
