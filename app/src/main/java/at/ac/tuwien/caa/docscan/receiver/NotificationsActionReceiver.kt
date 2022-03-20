package at.ac.tuwien.caa.docscan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.work.WorkManager
import at.ac.tuwien.caa.docscan.logic.notification.DocScanNotificationChannel
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.worker.ExportWorker
import at.ac.tuwien.caa.docscan.worker.UploadWorker
import kotlinx.parcelize.Parcelize
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.*

/**
 * A notification action receiver which handles button clicks of notifications.
 */
class NotificationsActionReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_NOTIFICATION_ACTION = "EXTRA_NOTIFICATION_ACTION"

        fun newInstance(context: Context, wrapper: NotificationWrapper): Intent {
            return Intent(context, NotificationsActionReceiver::class.java).apply {
                // action is defined in the manifest
                action = "DOC_SCAN_NOTIFICATION_ACTION"
                putExtra(EXTRA_NOTIFICATION_ACTION, wrapper)
            }
        }
    }

    private val notificationHandler by KoinJavaComponent.inject<NotificationHandler>(
        NotificationHandler::class.java
    )

    private val workManager by KoinJavaComponent.inject<WorkManager>(
        WorkManager::class.java
    )

    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.getParcelable<NotificationWrapper>(EXTRA_NOTIFICATION_ACTION)
            ?.let { notificationWrapper ->
                Timber.d("Receive notification action!")
                when (notificationWrapper.channel) {
                    DocScanNotificationChannel.CHANNEL_EXPORT -> {
                        when (notificationWrapper.button) {
                            NotificationButton.CANCEL -> {
                                ExportWorker.cancelWorkByDocumentId(
                                    workManager,
                                    notificationWrapper.docId
                                )
                                notificationHandler.cancelNotification(
                                    notificationWrapper.channel.tag,
                                    notificationWrapper.docId.hashCode()
                                )
                            }
                        }
                    }
                    DocScanNotificationChannel.CHANNEL_UPLOAD -> {
                        when (notificationWrapper.button) {
                            NotificationButton.CANCEL -> {
                                UploadWorker.cancelWorkByDocumentId(
                                    workManager,
                                    notificationWrapper.docId
                                )
                                notificationHandler.cancelNotification(
                                    notificationWrapper.channel.tag,
                                    notificationWrapper.docId.hashCode()
                                )
                            }
                        }
                    }
                    DocScanNotificationChannel.CHANNEL_GENERAL -> {
                        // ignore
                    }
                }
            } ?: kotlin.run {
            Timber.e("Cannot parse notification wrapper in NotificationsActionReceiver!")
        }
    }
}

@Parcelize
data class NotificationWrapper(
    val channel: DocScanNotificationChannel,
    val button: NotificationButton,
    val docId: UUID
) : Parcelable

enum class NotificationButton {
    CANCEL
}
