package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.extensions.asUUID
import at.ac.tuwien.caa.docscan.logic.ExportFormat
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.*

/**
 * Represents a worker for exporting a document.
 */
class ExportWorker(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val documentRepository by KoinJavaComponent.inject<DocumentRepository>(DocumentRepository::class.java)
    private val exportRepository by KoinJavaComponent.inject<ExportRepository>(ExportRepository::class.java)
    private var documentCollectorJob: Job? = null

    override suspend fun doWork(): Result {
        val docId = inputData.getString(INPUT_PARAM_DOC_ID)?.asUUID() ?: run {
            Timber.e("ExportWorker has failed, docId is null!")
            return Result.failure()
        }
        val exportFormat = ExportFormat.getExportFormatById(inputData.getString(INPUT_PARAM_EXPORT_FORMAT))
        // TODO: Show initial notification
        return withContext(Dispatchers.IO) {
            documentCollectorJob = async {
                documentRepository.getDocumentWithPagesAsFlow(documentId = docId).collectLatest {
                    // TODO: Update progress notification
                    Timber.d("doc has changed!")
                }
            }
            val resource = exportRepository.exportDoc(docId, exportFormat)
            documentCollectorJob?.cancel()
            // TODO: Update notification with final message!
            return@withContext when (resource) {
                is Failure -> Result.failure()
                is Success -> Result.success()
            }
        }
    }

    companion object {

        private const val INPUT_PARAM_DOC_ID = "INPUT_PARAM_DOC_ID"
        private const val INPUT_PARAM_EXPORT_FORMAT = "INPUT_PARAM_EXPORT_FORMAT"
        private const val EXPORT_TAG = "export"

        fun spawnExportJob(
                workManager: WorkManager,
                documentId: UUID,
                exportFormat: ExportFormat
        ) {
            val exportDocRequest = OneTimeWorkRequest.Builder(ExportWorker::class.java)
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
