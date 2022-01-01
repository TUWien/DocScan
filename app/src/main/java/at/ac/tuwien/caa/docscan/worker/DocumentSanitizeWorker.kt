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
import java.util.*

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

        private const val TAG = "document_sanitizer"

        fun spawnSanitize(workManager: WorkManager) {

            val sanitizeDocumentsRequest =
                OneTimeWorkRequest.Builder(DocumentSanitizeWorker::class.java)
                    .addTag(TAG)
                    .build()
            Timber.i("Requesting WorkManager to queue DocumentSanitizeWorker")
            workManager.enqueue(sanitizeDocumentsRequest)
        }
    }
}
