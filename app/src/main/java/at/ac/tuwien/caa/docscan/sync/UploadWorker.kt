package at.ac.tuwien.caa.docscan.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UploadWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // TODO: Upload files
        return Result.success()
    }
}
