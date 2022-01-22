package at.ac.tuwien.caa.docscan.db.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = ExportFile.TABLE_NAME_EXPORT_FILES)
data class ExportFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = KEY_ID)
    val id: Int = 0,
    @ColumnInfo(name = KEY_FILE_NAME)
    val fileName: String
) {
    companion object {

        const val TABLE_NAME_EXPORT_FILES = "export_files"
        const val KEY_ID = "id"
        const val KEY_FILE_NAME = "file_name"
    }
}
