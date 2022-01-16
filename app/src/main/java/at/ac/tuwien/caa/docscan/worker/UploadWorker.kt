package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.extensions.asUUID
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.isRecoverable
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.UploadRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Represents a worker for uploading a document.
 */
class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val uploadRepository by inject<UploadRepository>(UploadRepository::class.java)
    private val documentRepository by inject<DocumentRepository>(DocumentRepository::class.java)
    private val notificationHandler by inject<NotificationHandler>(NotificationHandler::class.java)

    private var documentCollectorJob: Job? = null

    /**
     * TODO: Which notifications should be shown?
     *
     * 1) "Upload wird gestartet."
     * 2) "Upload fuer Doc X, Page ì/n wird hochgeladen."
     * 3) "Upload ist fehlgeschlagen. (Retry)"
     * 4) "Upload ist fehlgeschlagen. User session abgelaufen."
     * 5) "Upload ist fehlgeschlagen. User session abgelaufen."
     */
    override suspend fun doWork(): Result {
        val docId = inputData.getString(INPUT_PARAM_DOC_ID)?.asUUID() ?: run {
            Timber.e("UploadWorker has failed, docId is null!")
            return Result.failure()
        }
        return withContext(Dispatchers.IO) {
            documentCollectorJob = async {
                documentRepository.getDocumentWithPagesAsFlow(documentId = docId).collectLatest {
                    // TODO: Update progress notification
                    Timber.d("doc has changed!")
                }
            }
            val resource = uploadRepository.uploadDocument(docId)
            documentCollectorJob?.cancel()
            // TODO: Update notification with final message!
            return@withContext when (resource) {
                is Failure -> {
                    if (resource.isRecoverable()) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                is Success -> Result.success()
            }
        }
    }

    companion object {

        private const val INPUT_PARAM_DOC_ID = "INPUT_PARAM_DOC_ID"
        private const val UPLOAD_TAG = "transkribus_image_uploader"

        fun spawnUploadJob(
            workManager: WorkManager,
            documentId: UUID,
            allowMobileData: Boolean
        ) {
            val uploadImages = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .addTag(UPLOAD_TAG)
                .setInputData(
                    workDataOf(
                        INPUT_PARAM_DOC_ID to documentId.toString()
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (allowMobileData) NetworkType.METERED else NetworkType.UNMETERED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            Timber.i("Requesting WorkManager to queue UploadWorker")
            workManager.enqueueUniqueWork(
                getWorkNameByDocId(documentId),
                ExistingWorkPolicy.KEEP,
                uploadImages
            )
        }

        private fun getWorkNameByDocId(documentId: UUID) = UPLOAD_TAG + "_" + documentId.toString()

        /**
         * Cancels a worker job per documentId.
         */
        fun cancelWorkByDocumentId(workManager: WorkManager, documentId: UUID) {
            workManager.cancelUniqueWork(getWorkNameByDocId(documentId))
        }
    }
}
