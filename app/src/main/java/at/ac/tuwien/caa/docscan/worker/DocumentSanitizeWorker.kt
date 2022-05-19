package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.notification.DocScanNotificationChannel
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import timber.log.Timber

/**
 * Represents a worker which checks document/page states and repairs them if necessary.
 * If e.g. the app has crashed or was killed during a document operation, the document
 * would remain locked.
 */
class DocumentSanitizeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val documentRepository by KoinJavaComponent.inject<DocumentRepository>(
        DocumentRepository::class.java
    )

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            return@withContext when (documentRepository.sanitizeDocuments()) {
                is Success -> Result.success()
                is Failure -> Result.failure()
            }
        }
    }

    companion object {

        const val TAG = "document_sanitizer"
        const val ID = "c5c52f8b-f238-484e-b988-1f635d06fa4e"

        /**
         * Sanitizes DB states in the app:
         * - Cancels all enqueued [ExportWorker] jobs.
         * - Cancels all notifications for channel [DocScanNotificationChannel.CHANNEL_EXPORT].
         * - Spawns a sanitize worker, that will run [DocumentRepository.sanitizeDocuments]
         */
        fun sanitizeWorkerJobs(
            context: Context,
            notificationHandler: NotificationHandler,
            workManager: WorkManager
        ) {

            // cancel all export notifications as they will be cancelled
            notificationHandler.cancelAllNotificationsByChannel(DocScanNotificationChannel.CHANNEL_EXPORT)

            // worker jobs that have not been running when the app was killed, will be still enqueued
            // and might be called at a later time.
            // - enqueued export jobs will be cancelled, as their state is reset in sanitizeDocuments, since
            // they are not designed to be restarted.
            getCurrentWorkerJobStates(
                context, listOf(
                    ExportWorker.EXPORT_TAG
                ), states = listOf(WorkInfo.State.ENQUEUED)
            ).forEach {
                Timber.i("Cancelling worker job $it")
                workManager.cancelWorkById(it.jobId)
            }

            val sanitizeDocumentsRequest =
                OneTimeWorkRequest.Builder(DocumentSanitizeWorker::class.java)
                    // please note, to not add multiple tags (see getCurrentWorkerJobStates for more info)
                    .addTag(TAG)
                    .build()
            Timber.i("Requesting WorkManager to queue DocumentSanitizeWorker")
            workManager.enqueueUniqueWork(
                "${TAG}_${ID}",
                ExistingWorkPolicy.REPLACE,
                sanitizeDocumentsRequest
            )
        }
    }
}
