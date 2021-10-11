package at.ac.tuwien.caa.docscan.db.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.ac.tuwien.caa.docscan.db.model.Page.Companion.TABLE_NAME_PAGES
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.DocumentPage
import java.util.*

/**
 * Represents the [Page] entity, which is uniquely represented in the storage by the [docId] and
 * [id].
 */
@Entity(tableName = TABLE_NAME_PAGES)
data class Page(
        /**
         * Uniquely identifies the page.
         */
        @PrimaryKey
        @ColumnInfo(name = KEY_ID)
        val id: UUID,
        /**
         * The id of [Document] to which the page belongs to.
         */
        @ColumnInfo(name = KEY_DOC_ID)
        val docId: UUID,
        /**
         * The ordering number of the page, across all pages in a single document.
         */
        @ColumnInfo(name = KEY_NUMBER)
        val number: Int,
        /**
         * The rotation of the page. The exif info is saved separately into the file, this
         * column is necessary to keep track of file changes in the DB.
         */
        @ColumnInfo(name = KEY_ROTATION)
        val rotation: Rotation,
        /**
         * Represents the processing state of the page.
         */
        @ColumnInfo(name = KEY_POST_PROCESSING_STATE)
        val postProcessingState: PostProcessingState,
        // TODO: add state for uploading
        /**
         * An optional (cropping) boundary with 4 points.
         */
        // TODO: Check if this is ok to be nullable
        @Embedded(prefix = KEY_SINGLE_PAGE_BOUNDARY_PREFIX)
        val singlePageBoundary: SinglePageBoundary?
) {
    // TODO: Hash of the image file to determine if it has changed or not.
    companion object {
        const val TABLE_NAME_PAGES = "pages"
        const val KEY_ID = "id"
        const val KEY_DOC_ID = "doc_id"
        const val KEY_NUMBER = "number"
        const val KEY_ROTATION = "rotation"
        const val KEY_POST_PROCESSING_STATE = "post_processing_state"
        const val KEY_SINGLE_PAGE_BOUNDARY = "single_page_boundary"
        const val KEY_SINGLE_PAGE_BOUNDARY_PREFIX = "spb"
    }
}

fun Page.asDocumentPageExtra(): DocumentPage {
    return DocumentPage(docId = this.docId, pageId = this.id)
}

