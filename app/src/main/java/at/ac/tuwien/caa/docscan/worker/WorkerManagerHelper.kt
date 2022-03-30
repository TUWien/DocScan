package at.ac.tuwien.caa.docscan.worker

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import java.util.*

/**
 * @return a list of some worker jobs.
 *
 * The params are set to return jobs for all tags, but only for work.info states that are != SUCCEEDED.
 */
fun getCurrentWorkerJobStates(
    context: Context,
    tags: List<String> = listOf(
        UploadWorker.UPLOAD_TAG,
        ExportWorker.EXPORT_TAG,
        DocumentSanitizeWorker.TAG
    ),
    states: List<WorkInfo.State> = WorkInfo.State.values()
        .filter { state -> state != WorkInfo.State.SUCCEEDED },
): List<DocScanWorkInfo> {
    val docScanWorkInfos = mutableListOf<DocScanWorkInfo>()

    val workInfosFuture = WorkManager.getInstance(context)
        .getWorkInfos(WorkQuery.Builder.fromTags(tags).addStates(states).build())
    // TODO: Call can throw
    val workInfos = workInfosFuture.get()
    workInfos.forEach {
        // we assume that a worker request was always associated with just one tag.
        val firstTag = it.tags.firstOrNull()
        if (firstTag != null) {
            docScanWorkInfos.add(DocScanWorkInfo(firstTag, it.id, it))
        }
    }
    return docScanWorkInfos.sortedBy { docScanWorkInfo -> docScanWorkInfo.tag }
}

data class DocScanWorkInfo(val tag: String, val jobId: UUID, val workInfo: WorkInfo)
