package at.ac.tuwien.caa.docscan.db.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = ExportFile.TABLE_NAME_EXPORT_FILES)
data class ExportFile(
    @PrimaryKey
    @ColumnInfo(name = KEY_FILE_NAME)
    val fileName: String,
    @ColumnInfo(name = KEY_IS_PROCESSING)
    val isProcessing: Boolean
) {
    companion object {

        const val TABLE_NAME_EXPORT_FILES = "export_files"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_IS_PROCESSING = "is_processing"
    }
}
