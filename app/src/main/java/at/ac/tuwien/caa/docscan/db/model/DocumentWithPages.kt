package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.logic.NetworkStatus
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class DocumentWithPages(
    @Embedded
    val document: Document,
    @Relation(
        parentColumn = Document.KEY_ID,
        entityColumn = Page.KEY_DOC_ID
    )
    var pages: List<Page> = listOf()
) : Parcelable {

    /**
     * An additional field to provide the current network status with the document, this is only
     * aggregated in some cases and needs to be done explicitly!
     */
    @IgnoredOnParcel
    @Ignore
    var networkStatus: NetworkStatus = NetworkStatus.DISCONNECTED

    /**
     * An additional field to provide the doc with the info if the user has allowed uploads on metered networks.
     */
    @IgnoredOnParcel
    @Ignore
    var hasUserAllowedMeteredNetwork: Boolean = false
}

fun DocumentWithPages.isProcessing(): Boolean {
    return pages.firstOrNull { page -> page.isProcessing() } != null
}

fun DocumentWithPages.isExporting(): Boolean {
    return pages.firstOrNull { page -> page.isExporting() } != null
}

fun DocumentWithPages.numberOfFinishedExports(): Int {
    return pages.count { page -> page.exportState == ExportState.DONE }
}

fun DocumentWithPages.numberOfFinishedUploads(): Int {
    return pages.count { page -> page.transkribusUpload.state == UploadState.UPLOADED }
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
 * @return true if any of the pages is currently being scheduled for upload.
 */
fun DocumentWithPages.isUploadScheduled(): Boolean {
    pages.forEach { page ->
        if (page.isUploadScheduled()) {
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
