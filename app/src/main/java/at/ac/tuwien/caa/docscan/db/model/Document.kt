package at.ac.tuwien.caa.docscan.db.model

import androidx.annotation.Keep
import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Document.Companion.TABLE_NAME_DOCUMENTS
import java.util.*

/**
 * TODO: Consider adding a pipeline status for processing, awaitingUpload, and uploading and also editing.
 */
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
     * Represents the active state of a document, only one document can be active.
     */
    @Embedded(prefix = KEY_META_DATA_PREFIX)
    var metaData: MetaData? = null
) {
    companion object {
        const val TABLE_NAME_DOCUMENTS = "documents"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_META_DATA_PREFIX = "metadata_"
        const val KEY_IS_ACTIVE = "is_active"
    }
}
