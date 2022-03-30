package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
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

        fun spawnSanitize(workManager: WorkManager) {
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
