package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Document.Companion.TABLE_NAME_DOCUMENTS
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * TODO: Consider adding a pipeline status for processing, awaitingUpload, and uploading and also editing.
 * TODO: Save the relatedUploadURL which is retrieved from the URL
 * TODO: It's probably better to remove the doc from being active, when an upload/export is performed.
 */
@Parcelize
@Keep
@Entity(tableName = TABLE_NAME_DOCUMENTS)
data class Document(
    @PrimaryKey
    @ColumnInfo(name = KEY_ID)
    val id: UUID,
    @ColumnInfo(name = KEY_TITLE)
    var title: String,
    /**
     * Represents the active state of a document, only one document can be active.
     */
    @ColumnInfo(name = KEY_IS_ACTIVE)
    var isActive: Boolean,
    /**
     * Represents the lock state of a document.
     */
    @ColumnInfo(name = KEY_LOCK_STATE)
    var lockState: LockState = LockState.NONE,
    /**
     * Represents the active state of a document, only one document can be active.
     */
    @Embedded(prefix = KEY_META_DATA_PREFIX)
    var metaData: MetaData? = null,
    /**
     * Represents the transkribus upload id, which is used to associate uploads of documents with an id.
     * A soon as the upload is entirely finished, this id correponds to the document id of the Transkribus BE.
     *
     * see [Upload] or [TranskribusAPIService] for more information.
     */
    @ColumnInfo(name = KEY_TRANSKRIBUS_UPLOAD_ID)
    var uploadId: Int? = null
) : Parcelable {
    companion object {
        const val TABLE_NAME_DOCUMENTS = "documents"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_META_DATA_PREFIX = "metadata_"
        const val KEY_IS_ACTIVE = "is_active"
        const val KEY_LOCK_STATE = "lock_state"
        const val KEY_TRANSKRIBUS_UPLOAD_ID = "transkribus_upload_id"
    }
}

fun Document.sanitizedTitle(): String {
    return title.replace(" ", "").lowercase()
}

fun Document.edit(title: String, metaData: MetaData?): Document {
    this.title = title
    val metaDataTemp = metaData
    // TODO: Check if this is ok, so that the relateduPloadId is copied from the old one.
    metaData?.relatedUploadId = this.metaData?.relatedUploadId
    this.metaData = metaDataTemp
    return this
}
