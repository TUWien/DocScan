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
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import timber.log.Timber

class NotificationHandler(val context: Context) {

    companion object {
        private const val PDF_INTENT = "PDF_INTENT"
        private const val PDF_FILE_NAME = "PDF_FILE_NAME"
        private const val PDF_CHANNEL_ID = "PDF_CHANNEL_ID"
        private val PDF_CHANNEL_NAME: CharSequence = "DocScan Pdf" // The user-visible name of the channel.
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
            context: Context,
            docScanNotificationChannel: DocScanNotificationChannel,
            groupKey: String?,
            contentTitle: String,
            contentText: String,
            ticker: String,
            date: Long = System.currentTimeMillis()
    ): NotificationCompat.Builder {

        val builder = NotificationCompat.Builder(context, docScanNotificationChannel.channelId)
                .setSmallIcon(R.drawable.ic_docscan_notification)
                .setContentText(contentText)
                .setContentTitle(contentTitle)
                .setWhen(date)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
//                .setSound(channelEnum.ringTone)
                .setAutoCancel(docScanNotificationChannel.isAutoCancel)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                .setTicker(ticker)
                .setGroup(groupKey)

        // priority is only necessary if the channel importance is not set.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.priority = docScanNotificationChannel.priority
        }
        return builder
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
            description = context.getString(docScanNotificationChannel.channelDescriptionResource)
            enableLights(true)
            lightColor = context.getColor(R.color.colorPrimary)
            enableVibration(true)
//            setSound(channelEnum.ringTone, Notification.AUDIO_ATTRIBUTES_DEFAULT)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isNotificationChannelEnabled(docScanNotificationChannel.channelId) == false
        ) {
            return false // do nothing, otherwise notification will be persisted.
        }
        return true
    }

    // TODO: Refactor the notifications for exports and uploads
    private fun getNotificationBuilder(documentName: String,
                                       notificationManager: NotificationManager, context: Context?): NotificationCompat.Builder {
        val title = context!!.getString(R.string.pdf_notification_exporting)

        //        Create an intent that is started, if the user clicks on the notification:
        val intent = Intent(context, DocumentViewerActivity::class.java)
        intent.putExtra(NotificationHandler.PDF_INTENT, true)
//        intent.putExtra(NotificationHandler.PDF_FILE_NAME, PdfCreator.getPdfFile(documentName).absolutePath)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context,
                NotificationHandler.PDF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_docscan_notification)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setChannelId(NotificationHandler.PDF_CHANNEL_ID)

        // On Android O we need a NotificationChannel, otherwise the notification is not shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_LOW disables the notification sound:
            val importance = NotificationManager.IMPORTANCE_LOW
//            val notificationChannel = NotificationChannel(
//                    NotificationHandler.PDF_CHANNEL_ID, PdfCreator.PDF_CHANNEL_NAME, importance)
//            notificationManager.createNotificationChannel(NotificationHandler)
        }
        return builder
    }

    // TODO: Refactor the progress notification for exports and uploads
    private fun progressNotification(documentName: String, progress: Int,
                                     notificationManager: NotificationManager,
                                     context: Context?, builder: NotificationCompat.Builder?) {
        if (builder == null) return
        if (progress != -1) builder.setContentTitle(context!!.getString(R.string.pdf_notification_exporting))
                .setContentText(documentName)
                .setProgress(100, progress, false) else builder.setContentTitle(context!!.getString(R.string.pdf_notification_exporting))
                .setContentText(documentName) // Removes the progress bar
                .setProgress(0, 0, false)

        // show the new notification:
        notificationManager.notify(25, builder.build())
    }

    // TODO: Refactor the progress notification for exports and uploads
    private fun successNotification(documentName: String,
                                    notificationManager: NotificationManager,
                                    context: Context?, builder: NotificationCompat.Builder?) {
        if (builder == null) return
        builder.setContentTitle(context!!.getString(R.string.pdf_notification_done))
                .setContentText(documentName) // Removes the progress bar
                .setProgress(0, 0, false)

        // show the new notification:
        notificationManager.notify(25, builder.build())
    }

    // TODO: Refactor the progress notification for exports and uploads
    private fun errorNotification(documentName: String,
                                  notificationManager: NotificationManager,
                                  context: Context?, builder: NotificationCompat.Builder?) {
        if (builder == null) return
        builder.setContentTitle(context!!.getString(R.string.pdf_notification_error))
                .setContentText(documentName) // Removes the progress bar
                .setProgress(0, 0, false)

        // show the new notification:
        notificationManager.notify(25, builder.build())
    }
}
