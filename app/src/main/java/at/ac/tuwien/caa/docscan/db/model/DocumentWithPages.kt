package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import kotlinx.parcelize.Parcelize

@Parcelize
data class DocumentWithPages(
    @Embedded
    val document: Document,
    @Relation(
        parentColumn = Document.KEY_ID,
        entityColumn = Page.KEY_DOC_ID
    )
    var pages: List<Page> = listOf()
) : Parcelable

fun DocumentWithPages.isProcessing(): Boolean {
    return pages.firstOrNull { page -> page.isProcessing() } != null
}

fun DocumentWithPages.isExporting(): Boolean {
    return pages.firstOrNull { page -> page.isExporting() } != null
}

fun DocumentWithPages.numberOfFinishedExports(): Int {
    return pages.count { page -> page.exportState == ExportState.DONE }
}

/**
 * @return true if any of the pages is currently being uplodead.
 */
fun DocumentWithPages.isUploadInProgress(): Boolean {
    pages.forEach { page ->
        if (page.isUploadInProgress()) {
            return true
        }
    }
    return false
}

/**
 * @return true if all of the pages have been uploaded.
 */
fun DocumentWithPages.isUploaded(): Boolean {
    pages.forEach { page ->
        if (!page.isUploaded()) {
            return false
        }
    }
    return true
}

fun DocumentWithPages.isCropped(): Boolean {
    pages.forEach { page ->
        if (!page.isPostProcessed()) {
            return false
        }
    }
    return true
}
