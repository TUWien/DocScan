package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.*
import at.ac.tuwien.caa.docscan.extensions.asUUID
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.isRecoverable
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
        // TODO: Show initial notification
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
            documentId: UUID
        ) {
// TODO: Check old constraints
            //        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//
//        boolean useMobileConnection = sharedPref.getBoolean(context.getResources().getString(
//                R.string.key_upload_mobile_data), true);
//        int[] constraints;
//        if (useMobileConnection) {
//            constraints = new int[]{Constraint.ON_ANY_NETWORK};
//            Log.d(CLASS_NAME, "startSyncJob: using mobile connection");
//        } else {
//            constraints = new int[]{Constraint.ON_UNMETERED_NETWORK};
//            Log.d(CLASS_NAME, "startSyncJob: using just wifi");
//        }
            //        Job syncJob = dispatcher.newJobBuilder()
//                // the JobService that will be called
////                .setService(SyncService.class)
//                .setService(UploadService.class)
//                // uniquely identifies the job
//                .setTag(JOB_TAG)
//                // one-off job
//                .setRecurring(false)
//                // don't persist past a device reboot
//                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
//                .setTrigger(timeWindow)
//                // overwrite an existing job with the same tag - this assures that just one job is running at a time:
//                .setReplaceCurrent(true)
//                // retry with exponential backoff
//                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
//                .setConstraints(
//                        constraints
//                )
//                .build();
            val uploadImages = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .addTag(UPLOAD_TAG)
                .setInputData(
                    workDataOf(
                        INPUT_PARAM_DOC_ID to documentId.toString()
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

        /**
         * Cancels a worker job per documentId.
         */
        fun cancelWorkByDocumentId(workManager: WorkManager, documentId: UUID) {
            workManager.cancelUniqueWork(getWorkNameByDocId(documentId))
        }
    }
}
