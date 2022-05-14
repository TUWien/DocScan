package at.ac.tuwien.caa.docscan.db.model

import android.net.Uri
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Keep
@Entity(tableName = ExportFile.TABLE_NAME_EXPORT_FILES)
data class ExportFile(

    /**
     * A unique file name. The uniqueness of file names is automatically ensured by the android
     * system for a specific folder.
     */
    @PrimaryKey
    @ColumnInfo(name = KEY_FILE_NAME)
    val fileName: String,
    /**
     * The corresponding documentId, this is not unique as there might be several different exports
     * for a single document.
     * - the default value is just an arbitrary id in order to prevent making the field nullable,
     * but for already created exports, this field doesn't matter anyway.
     */
    @ColumnInfo(name = KEY_DOC_ID, defaultValue = "d0289e3b-f7f3-4e9b-8eb8-6f392c503f51")
    val docId: UUID,
    /**
     * The corresponding fileUri where the export is going to placed, this is only available for
     * a short period of time while the document is being exported.
     *
     * Please note that this fileUri should be only used for clean-ups if the export has been interrupted
     * in an unexpected way.
     */
    @ColumnInfo(name = KEY_FILE_URI)
    val fileUri: Uri?,
    /**
     * A boolean flag that indicates if the export file is being processed.
     */
    @ColumnInfo(name = KEY_IS_PROCESSING)
    val isProcessing: Boolean
) {
    companion object {

        const val TABLE_NAME_EXPORT_FILES = "export_files"
        const val KEY_DOC_ID = "doc_id"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_URI = "file_uri"
        const val KEY_IS_PROCESSING = "is_processing"
    }
}
