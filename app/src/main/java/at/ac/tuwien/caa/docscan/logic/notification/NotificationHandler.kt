package at.ac.tuwien.caa.docscan.logic.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.numberOfFinishedExports
import at.ac.tuwien.caa.docscan.db.model.numberOfFinishedUploads
import at.ac.tuwien.caa.docscan.logic.DocumentViewerLaunchViewType
import at.ac.tuwien.caa.docscan.logic.getMessage
import at.ac.tuwien.caa.docscan.receiver.NotificationButton
import at.ac.tuwien.caa.docscan.receiver.NotificationWrapper
import at.ac.tuwien.caa.docscan.receiver.NotificationsActionReceiver
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import timber.log.Timber
import java.util.*

class NotificationHandler(val context: Context) {

    companion object {
        private const val PDF_INTENT = "PDF_INTENT"
        private const val PDF_FILE_NAME = "PDF_FILE_NAME"
        private const val PDF_CHANNEL_ID = "PDF_CHANNEL_ID"

        // TODO: MIGRATION: Remove this channel in the migration
        private val DEPRECATED_PDF_CHANNEL_NAME: CharSequence =
            "DocScan Pdf" // The user-visible name of the channel.
    }

    sealed class DocScanNotification(val title: String, val showCancelButton: Boolean) {
        class Init(title: String, val documentWithPages: DocumentWithPages) :
            DocScanNotification(title, true)

        class Progress(title: String, val documentWithPages: DocumentWithPages) :
            DocScanNotification(title, true)

        class Success(title: String, val text: String) : DocScanNotification(title, false)
        class Failure(title: String, val retryNow: Boolean, val throwable: Throwable) :
            DocScanNotification(title, false)
    }

    fun showDocScanNotification(
        docScanNotification: DocScanNotification,
        docId: UUID,
        docScanNotificationChannel: DocScanNotificationChannel
    ) {
        val notification = createBuilder(
            docScanNotificationChannel,
            null,
            docScanNotification.title,
            null,
            null
        )

        when (docScanNotification) {
            is DocScanNotification.Failure -> {
                notification.setAutoCancel(true)
                notification.setOnlyAlertOnce(false)
                notification.setProgress(0, 0, false)
                notification.setContentText(docScanNotification.throwable.getMessage(context))
            }
            is DocScanNotification.Init -> {
                notification.setAutoCancel(false)
                notification.setOnlyAlertOnce(false)
                notification.setOngoing(true)
                notification.setProgress(0, 0, true)
            }
            is DocScanNotification.Progress -> {
                val progress = when (docScanNotificationChannel) {
                    DocScanNotificationChannel.CHANNEL_EXPORT -> {
                        docScanNotification.documentWithPages.numberOfFinishedExports()
                    }
                    DocScanNotificationChannel.CHANNEL_UPLOAD -> {
                        docScanNotification.documentWithPages.numberOfFinishedUploads()
                    }
                }
                notification.setOnlyAlertOnce(true)
                notification.setOngoing(true)
                notification.setProgress(
                    docScanNotification.documentWithPages.pages.size,
                    progress,
                    false
                )
            }
            is DocScanNotification.Success -> {
                notification.setAutoCancel(true)
                notification.setOnlyAlertOnce(false)
                notification.setProgress(0, 0, false)
                notification.setContentText(docScanNotification.text)
                notification.setTicker(docScanNotification.text)
            }
        }

        // Create an explicit intent for an Activity in your app
        val intent = when (docScanNotificationChannel) {
            DocScanNotificationChannel.CHANNEL_EXPORT -> {
                DocumentViewerActivity.newInstance(context, DocumentViewerLaunchViewType.PDFS)
            }
            DocScanNotificationChannel.CHANNEL_UPLOAD -> {
                DocumentViewerActivity.newInstance(context, DocumentViewerLaunchViewType.DOCUMENTS)
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            flags
        )

        notification.setContentIntent(pendingIntent)

        if (docScanNotification.showCancelButton) {
            val cancelNotification = NotificationsActionReceiver.newInstance(
                context,
                NotificationWrapper(docScanNotificationChannel, NotificationButton.CANCEL, docId)
            )
            val cancelIntent: PendingIntent =
                PendingIntent.getBroadcast(context, 0, cancelNotification, flags)
            notification.addAction(
                R.drawable.ic_cancel_white_24dp, context.getString(R.string.dialog_cancel_text),
                cancelIntent
            )
        }

        if ((docScanNotification as? DocScanNotification.Failure)?.retryNow == true) {
            val retryNotification = NotificationsActionReceiver.newInstance(
                context,
                NotificationWrapper(docScanNotificationChannel, NotificationButton.RETRY, docId)
            )
            val retryIntent: PendingIntent =
                PendingIntent.getBroadcast(context, 0, retryNotification, flags)
            notification.addAction(
                R.drawable.ic_baseline_refresh_24, context.getString(R.string.dialog_btn_retry_now),
                retryIntent
            )
        }

        showNotification(
            docScanNotificationChannel.tag,
            docId.hashCode(),
            notification.build()
        )
    }

    fun showNotification(
        tag: String,
        notificationId: Int,
        notification: Notification
    ) {
        getNotificationManager(context).notify(tag, notificationId, notification)
    }

    fun cancelNotification(tag: String, notificationId: Int) {
        getNotificationManager(context).cancel(tag, notificationId)
    }

    fun cancelNotificationByGroup(groupKey: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getNotificationManager(context).activeNotifications.forEach { statusBarNotification ->
                val notification = statusBarNotification.notification
                if (notification.group == groupKey) {
                    cancelNotification(statusBarNotification.tag, statusBarNotification.id)
                }
            }
        }
    }

    fun cancelAllNotifications(context: Context) {
        getNotificationManager(context).cancelAll()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        notificationChannel: NotificationChannel
    ) {
        getNotificationManager(context).createNotificationChannel(notificationChannel)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun removeNotificationChannel(context: Context, notificationChannelId: String) {
        getNotificationManager(context).deleteNotificationChannel(notificationChannelId)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getNotificationChannel(context: Context, channelId: String): NotificationChannel? {
        try {
            return getNotificationManager(context).getNotificationChannel(channelId)
        } catch (exception: Exception) {
            Timber.w(
                exception, "Error at loading notification channel with channelId: " + channelId +
                        ". This might be expected behaviour if the channel simply does not yet exist!"
            )
        }
        return null
    }

    /**
     * Use this method to check, if the main system notifications for the app are enabled.
     */
    private fun areSystemNotificationsEnabled(context: Context) =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun getNotificationManager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    fun createBuilder(
        docScanNotificationChannel: DocScanNotificationChannel,
        groupKey: String? = null,
        contentTitle: String,
        contentText: String?,
        ticker: String?,
        date: Long = System.currentTimeMillis()
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, docScanNotificationChannel.channelId)
            .setSmallIcon(R.drawable.ic_docscan_notification)
            .setContentText(contentText)
            .setContentTitle(contentTitle)
            .setWhen(date)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(docScanNotificationChannel.isAutoCancel)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
            .setTicker(ticker)
            // importance param needs to be set as priority to support the importance on devices with < android 8
            .setPriority(docScanNotificationChannel.importance)
            .setGroup(groupKey)
    }

    /**
     * Initializes notification channels. Call this early upon app-start.
     */
    fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        DocScanNotificationChannel.values().forEach { channel ->
            if (channel.isActive) {
                createNotificationChannel(context, getNotificationChannelTemplate(channel))
            } else {
                removeNotificationChannel(context, channel.channelId)
            }
        }
    }

    /**
     * Creates a new [NotificationCompat.Builder] object, for a summay.
     */
    fun createBuilderForSummary(
        docScanNotificationChannel: DocScanNotificationChannel,
        groupKey: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, docScanNotificationChannel.channelId)
            .setSmallIcon(R.drawable.ic_docscan_notification)
            .setGroupSummary(true)
            .setGroup(groupKey)
            .setAutoCancel(docScanNotificationChannel.isAutoCancel)
    }

    /**
     * Add bigTextStyle with text to builder.
     */
    fun addBigTextStyleToBuilder(
        builder: NotificationCompat.Builder,
        bigText: String
    ) {

        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.bigText(bigText)
        builder.setStyle(bigTextStyle)
    }

    /**
     * @param channelId channelId of the channel
     * @return null if channel does not exist, false if the importance is [NotificationManager.IMPORTANCE_NONE] else true.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun isNotificationChannelEnabled(channelId: String): Boolean? {
        return getNotificationChannel(context, channelId)?.let {
            it.importance != NotificationManager.IMPORTANCE_NONE
        }
    }

    /**
     * @return a [NotificationChannel] based on [docScanNotificationChannel].
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getNotificationChannelTemplate(
        docScanNotificationChannel: DocScanNotificationChannel
    ): NotificationChannel {
        return NotificationChannel(
            docScanNotificationChannel.channelId,
            context.getString(docScanNotificationChannel.channelNameResource),
            docScanNotificationChannel.importance
        ).apply {
            name = context.getString(docScanNotificationChannel.channelNameResource)
            description = context.getString(docScanNotificationChannel.channelDescriptionResource)
            lightColor = context.getColor(R.color.colorPrimary)
            enableVibration(true)
            enableLights(true)
        }
    }

    /**
     * @return true if a notification for the specified channel [DocScanNotificationChannel] can be shown, otherwise
     * false.
     */
    fun canNotificationBeShown(docScanNotificationChannel: DocScanNotificationChannel): Boolean {
        // Check if general notifications are enabled
        if (!areSystemNotificationsEnabled(context)) {
            return false // do nothing, otherwise notification will be persisted.
        }
        // only if the channel has been already created and is disabled, we won't show the notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isNotificationChannelEnabled(
                docScanNotificationChannel.channelId
            ) == false
        ) {
            return false // do nothing, otherwise notification will be persisted.
        }
        return true
    }
}
