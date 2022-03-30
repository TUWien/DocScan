package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.extensions.asUUID
import at.ac.tuwien.caa.docscan.logic.ExportFormat
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.notification.DocScanNotificationChannel
import at.ac.tuwien.caa.docscan.logic.notification.DocumentNotificationType
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.ExportRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.*

/**
 * Represents a worker for exporting a document.
 */
class ExportWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val documentRepository by KoinJavaComponent.inject<DocumentRepository>(
        DocumentRepository::class.java
    )
    private val exportRepository by KoinJavaComponent.inject<ExportRepository>(ExportRepository::class.java)
    private val notificationHandler by KoinJavaComponent.inject<NotificationHandler>(
        NotificationHandler::class.java
    )
    private var documentCollectorJob: Job? = null

    override suspend fun doWork(): Result {
        val docId = inputData.getString(INPUT_PARAM_DOC_ID)?.asUUID() ?: run {
            Timber.e("ExportWorker has failed, docId is null!")
            return Result.failure()
        }
        val doc = documentRepository.getDocumentWithPages(docId) ?: run {
            return Result.failure()
        }
        val exportFormat =
            ExportFormat.getExportFormatById(inputData.getString(INPUT_PARAM_EXPORT_FORMAT))
        val fileType = exportFormat.getFileType().extension.uppercase()

        notificationHandler.showDocumentNotification(
            NotificationHandler.DocScanNotification.Init(
                String.format(
                    context.getString(R.string.notification_export_title_progress),
                    fileType
                ),
                doc
            ),
            docId,
            DocumentNotificationType.EXPORT
        )
        return withContext(Dispatchers.IO) {
            documentCollectorJob = async {
                documentRepository.getDocumentWithPagesAsFlow(documentId = docId).collectLatest {
                    it?.let {
                        notificationHandler.showDocumentNotification(
                            NotificationHandler.DocScanNotification.Progress(
                                String.format(
                                    context.getString(R.string.notification_export_title_progress),
                                    fileType
                                ),
                                it
                            ),
                            docId,
                            DocumentNotificationType.EXPORT
                        )
                    }
                }
            }
            val resource: Resource<String>
            try {
                resource = exportRepository.exportDoc(docId, exportFormat)
            } catch (e: CancellationException) {
                documentCollectorJob?.cancel()
                notificationHandler.cancelNotification(
                    DocScanNotificationChannel.CHANNEL_EXPORT.tag,
                    doc.document.id.hashCode()
                )
                return@withContext Result.failure()
            }
            documentCollectorJob?.cancel()
            return@withContext when (resource) {
                is Failure -> {
                    notificationHandler.showDocumentNotification(
                        NotificationHandler.DocScanNotification.Failure(
                            context.getString(R.string.notification_export_title_error),
                            resource.exception,
                        ),
                        docId,
                        DocumentNotificationType.EXPORT
                    )
                    Result.failure()
                }
                is Success -> {
                    notificationHandler.showDocumentNotification(
                        NotificationHandler.DocScanNotification.Success(
                            context.getString(R.string.notification_export_title_success),
                            String.format(
                                context.getString(R.string.notification_export_text_success),
                                resource.data
                            )
                        ),
                        docId,
                        DocumentNotificationType.EXPORT
                    )
                    Result.success()
                }
            }
        }
    }

    companion object {

        private const val INPUT_PARAM_DOC_ID = "INPUT_PARAM_DOC_ID"
        private const val INPUT_PARAM_EXPORT_FORMAT = "INPUT_PARAM_EXPORT_FORMAT"
        const val EXPORT_TAG = "export"

        fun spawnExportJob(
            workManager: WorkManager,
            documentId: UUID,
            exportFormat: ExportFormat
        ) {
            val exportDocRequest = OneTimeWorkRequest.Builder(ExportWorker::class.java)
                // please note, to not add multiple tags (see getCurrentWorkerJobStates for more info)
                .addTag(EXPORT_TAG)
                .setInputData(
                    workDataOf(
                        INPUT_PARAM_DOC_ID to documentId.toString(),
                        INPUT_PARAM_EXPORT_FORMAT to exportFormat.id
                    )
                )
                .build()

            Timber.i("Requesting WorkManager to queue ExportWorker")
            workManager.enqueueUniqueWork(
                getWorkNameByDocId(documentId),
                ExistingWorkPolicy.KEEP,
                exportDocRequest
            )
        }

        private fun getWorkNameByDocId(documentId: UUID) = EXPORT_TAG + "_" + documentId.toString()

        /**
         * Cancels a worker job per documentId.
         */
        fun cancelWorkByDocumentId(workManager: WorkManager, documentId: UUID) {
            workManager.cancelUniqueWork(getWorkNameByDocId(documentId))
        }
    }
}
