package at.ac.tuwien.caa.docscan.sync

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.isRecoverable
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.UploadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val docId = inputData.keyValueMap[INPUT_PARAM_DOC_ID] as? UUID ?: run {
            Timber.e("UploadWorker has failed, docId is null!")
            return Result.failure()
        }
        // TODO: Show initial notification
        withContext(Dispatchers.IO) {
            documentCollectorJob = launch {
                documentRepository.getDocumentWithPagesAsFlow(documentId = docId).collectLatest {
                    // TODO: Update progress notification
                }
            }
        }
        return withContext(Dispatchers.IO) {
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

        fun spawnUploadJob(workManager: WorkManager, documentId: UUID) {
            val uploadImages = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .addTag(UPLOAD_TAG)
                .setInputData(
                    workDataOf(
                        INPUT_PARAM_DOC_ID to documentId
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                // the backoff criteria is set to quite a short time, since a retry is only performed if there is an
                // internet connection, i.e. if the user should be offline, the work is not retried due to the connected
                // constraint, but as soon as the user gets online, this will take only 5seconds to upload.
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                .build()

            Timber.i("Requesting WorkManager to queue UploadWorker")
            // the existing policy needs to be always KEEP, otherwise it may be possible that multiple work requests
            // are started, e.g. REPLACE would cancel work, but if this function is called a lot of times, cancelled
            // work would still be ongoing, therefore we need to ensure that this is set to KEEP, so that only one request
            // is performed at a time.
            workManager.enqueueUniqueWork(
                getWorkNameByDocId(documentId),
                ExistingWorkPolicy.KEEP,
                uploadImages
            )
        }

        private fun getWorkNameByDocId(documentId: UUID) = UPLOAD_TAG + "_" + documentId.toString()

        fun cancelWorkByDocumentId(workManager: WorkManager, documentId: UUID) {
            workManager.cancelUniqueWork(getWorkNameByDocId(documentId))
        }
    }
}
