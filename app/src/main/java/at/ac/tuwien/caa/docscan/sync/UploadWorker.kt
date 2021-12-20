package at.ac.tuwien.caa.docscan.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.UploadRepository
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val uploadRepository by inject<UploadRepository>(UploadRepository::class.java)

    override suspend fun doWork(): Result {
        // TODO: Check how to deal with failures, maybe use retry instead if the error is recoverable!
        return when (val resource = uploadRepository.uploadPendingDocuments()) {
            is Failure -> Result.failure()
            is Success -> Result.success()
        }
    }
}
