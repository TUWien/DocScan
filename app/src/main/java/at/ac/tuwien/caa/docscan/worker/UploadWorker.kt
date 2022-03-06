package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.UploadStatusResponse
import at.ac.tuwien.caa.docscan.extensions.asUUID
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.isUploadRecoverable
import at.ac.tuwien.caa.docscan.logic.notification.DocScanNotificationChannel
import at.ac.tuwien.caa.docscan.logic.notification.DocumentNotificationType
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
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val uploadRepository by inject<UploadRepository>(UploadRepository::class.java)
    private val documentRepository by inject<DocumentRepository>(DocumentRepository::class.java)
    private val notificationHandler by inject<NotificationHandler>(NotificationHandler::class.java)

    private var documentCollectorJob: Job? = null

    override suspend fun doWork(): Result {
        val docId = inputData.getString(INPUT_PARAM_DOC_ID)?.asUUID() ?: run {
            Timber.e("UploadWorker has failed, docId is null!")
            return Result.failure()
        }
        val doc = documentRepository.getDocumentWithPages(docId) ?: run {
            Timber.e("The doc cannot be found for upload!")
            return Result.failure()
        }
        return withContext(Dispatchers.IO) {
            documentCollectorJob = async {
                documentRepository.getDocumentWithPagesAsFlow(documentId = docId).collectLatest {
                    it ?: return@collectLatest
                    notificationHandler.showDocumentNotification(
                        NotificationHandler.DocScanNotification.Progress(
                            String.format(
                                context.getString(
                                    R.string.notification_upload_title_progress
                                ), doc.document.title
                            ),
                            it
                        ),
                        docId,
                        DocumentNotificationType.UPLOAD
                    )
                }
            }
            notificationHandler.showDocumentNotification(
                NotificationHandler.DocScanNotification.Init(
                    String.format(
                        context.getString(R.string.notification_upload_title_progress),
                        doc.document.title
                    ), doc
                ),
                docId,
                DocumentNotificationType.UPLOAD
            )
            val resource: Resource<UploadStatusResponse>
            try {
                resource = uploadRepository.uploadDocument(docId)
            } catch (e: CancellationException) {
                documentCollectorJob?.cancel()
                notificationHandler.cancelNotification(
                    DocScanNotificationChannel.CHANNEL_UPLOAD.tag,
                    doc.document.id.hashCode()
                )
                return@withContext Result.failure()
            }
            documentCollectorJob?.cancel()
            return@withContext when (resource) {
                is Failure -> {
                    notificationHandler.showDocumentNotification(
                        NotificationHandler.DocScanNotification.Failure(
                            String.format(
                                context.getString(R.string.notification_upload_title_error),
                                doc.document.title
                            ),
                            resource.exception
                        ),
                        docId,
                        DocumentNotificationType.UPLOAD
                    )
                    if (resource.isUploadRecoverable()) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                is Success -> {
                    notificationHandler.showDocumentNotification(
                        NotificationHandler.DocScanNotification.Success(
                            String.format(
                                context.getString(
                                    R.string.notification_upload_title_success
                                ), doc.document.title
                            ),
                            String.format(
                                context.getString(R.string.notification_upload_text_success),
                                doc.document.title
                            )
                        ),
                        docId,
                        DocumentNotificationType.UPLOAD
                    )
                    Result.success()
                }
            }
        }
    }

    companion object {

        private const val INPUT_PARAM_DOC_ID = "INPUT_PARAM_DOC_ID"
        const val UPLOAD_TAG = "transkribus_image_uploader"

        fun spawnUploadJob(
            workManager: WorkManager,
            documentId: UUID,
            allowMobileData: Boolean
        ) {
            val uploadImages = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                // please note, to not add multiple tags (see getCurrentWorkerJobStates for more info)
                .addTag(UPLOAD_TAG)
                .setInputData(
                    workDataOf(
                        INPUT_PARAM_DOC_ID to documentId.toString()
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED)
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

        private fun workNamePrefix() = "${UPLOAD_TAG}_"

        private fun getWorkNameByDocId(documentId: UUID) = workNamePrefix() + documentId.toString()

        fun getDocumentIdByWorkName(workName: String): String {
            return workName.replace(workNamePrefix(), "")
        }

        /**
         * Cancels a worker job per documentId.
         */
        fun cancelWorkByDocumentId(workManager: WorkManager, documentId: UUID) {
            workManager.cancelUniqueWork(getWorkNameByDocId(documentId))
        }

        /**
         * Cancels a worker job per name.
         */
        fun cancelWorkByUniqueName(workManager: WorkManager, name: String) {
            workManager.cancelUniqueWork(name)
        }

        /**
         * Cancels all worker jobs for the [UPLOAD_TAG].
         */
        fun cancelByTag(workManager: WorkManager) {
            workManager.cancelAllWorkByTag(UPLOAD_TAG)
        }

        /**
         * Cancels a worker job by its internal workId (this is not the same as the unique name/id
         * which can be set from the caller!)
         */
        fun cancelByWorkId(workManager: WorkManager, workId: UUID) {
            workManager.cancelWorkById(workId)
        }
    }
}
