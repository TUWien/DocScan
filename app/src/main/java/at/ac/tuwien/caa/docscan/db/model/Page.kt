package at.ac.tuwien.caa.docscan.db.model

import android.graphics.PointF
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.model.Page.Companion.TABLE_NAME_PAGES
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.boundary.asClockwiseList
import at.ac.tuwien.caa.docscan.db.model.boundary.asPoint
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.logic.*
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * Represents the [Page] entity, which is uniquely represented in the storage by the [docId] and
 * [id].
 */
@Parcelize
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
     * The hash of the page file.
     */
    @ColumnInfo(name = KEY_FILE_HASH)
    var fileHash: String,
    /**
     * The ordering number of the page, across all pages in a single document.
     *
     * Do NOT take this as an index, since they might be gaps, the number only takes care of the
     * ordering.
     */
    @ColumnInfo(name = KEY_NUMBER)
    val number: Int,
    /**
     * The rotation of the page. The exif info is saved separately into the file, this
     * column is necessary to keep track of file changes in the DB.
     */
    @ColumnInfo(name = KEY_ROTATION)
    var rotation: Rotation,

    /**
     * Represents the page file type.
     */
    @ColumnInfo(name = KEY_FILE_TYPE)
    val fileType: PageFileType,
    /**
     * Represents the processing state of the page.
     */
    @ColumnInfo(name = KEY_POST_PROCESSING_STATE)
    var postProcessingState: PostProcessingState,
    /**
     * Represents the export state of the page.
     */
    @ColumnInfo(name = KEY_EXPORT_STATE)
    var exportState: ExportState,
    /**
     * An optional (cropping) boundary with 4 points.
     */
    @Embedded(prefix = KEY_SINGLE_PAGE_BOUNDARY_PREFIX)
    var singlePageBoundary: SinglePageBoundary?,

    @Embedded(prefix = KEY_UPLOAD_PREFIX)
    var transkribusUpload: Upload = Upload()
) : Parcelable {
    companion object {
        const val TABLE_NAME_PAGES = "pages"
        const val KEY_ID = "id"
        const val KEY_DOC_ID = "doc_id"
        const val KEY_FILE_HASH = "file_hash"
        const val KEY_NUMBER = "number"
        const val KEY_ROTATION = "rotation"
        const val KEY_UPLOAD_PREFIX = "upload"
        const val KEY_POST_PROCESSING_STATE = "post_processing_state"
        const val KEY_FILE_TYPE = "file_type"
        const val KEY_SINGLE_PAGE_BOUNDARY = "single_page_boundary"
        const val KEY_EXPORT_STATE = "export_state"
        const val KEY_SINGLE_PAGE_BOUNDARY_PREFIX = "spb"
    }
}

fun Page.isUploadingOrProcessing(): Boolean {
    return isUploadInProgress() || isProcessing()
}

fun Page.isUploaded(): Boolean {
    return transkribusUpload.state == UploadState.UPLOADED
}

fun Page.isUploadInProgress(): Boolean {
    return transkribusUpload.state == UploadState.UPLOAD_IN_PROGRESS
}

fun Page.isProcessing(): Boolean {
    return postProcessingState == PostProcessingState.PROCESSING
}

fun Page.isPostProcessed(): Boolean {
    return postProcessingState == PostProcessingState.DONE
}

fun Page.isExporting(): Boolean {
    return exportState == ExportState.EXPORTING
}

fun Page.getSingleBoundaryPoints(): MutableList<PointF> {
    return (singlePageBoundary?.asClockwiseList() ?: SinglePageBoundary.getDefault()
        .asClockwiseList()).map { point ->
        point.asPoint()
    }.toMutableList()
}

/**
 * Pre-Condition: [points] must have at least 4 elments.
 */
fun Page.setSinglePageBoundary(points: List<PointF>) {
    singlePageBoundary = points.toSinglePageBoundary()
}

fun List<PointF>.toSinglePageBoundary(): SinglePageBoundary {
    return SinglePageBoundary(
        this[0].asPoint(),
        this[1].asPoint(),
        this[2].asPoint(),
        this[3].asPoint()
    )
}

fun Page.asDocumentPageExtra(): DocumentPage {
    return DocumentPage(docId = this.docId, pageId = this.id)
}

fun Page.computeFileHash(fileHandler: FileHandler): Resource<Unit> {
    val file = fileHandler.getFileByPage(this) ?: return IOErrorCode.FILE_MISSING.asFailure()
    fileHash = file.getFileHash()
    return Success(Unit)
}

fun Page.getScaledCropPoints(
    width: Int,
    height: Int
): List<PointF> {
    val points =
        (singlePageBoundary?.asClockwiseList()
            ?: SinglePageBoundary.getDefault().asClockwiseList()).map { point ->
            PointF(point.x, point.y)
        }
    PageDetector.scalePointsGeneric(points, width, height)
    return points
}
