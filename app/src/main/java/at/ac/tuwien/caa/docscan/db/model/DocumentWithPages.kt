package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import kotlinx.android.parcel.Parcelize

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

fun DocumentWithPages.isCropped(): Boolean {
    return pages.firstOrNull { page -> page.postProcessingState != PostProcessingState.DONE } != null
}

fun DocumentWithPages.isProcessing(): Boolean {
    return pages.firstOrNull { page -> page.postProcessingState == PostProcessingState.PROCESSING } != null
}

fun DocumentWithPages.isUploaded(): Boolean {
    //TODO: Implement this, this is basically false and should be represented with a XOR enum in the Document
    return false
}
