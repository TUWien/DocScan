package at.ac.tuwien.caa.docscan.db.dao

import androidx.annotation.Keep
import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.ExportFile
import kotlinx.coroutines.flow.Flow

@Keep
@Dao
interface ExportFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportFile(exportFile: ExportFile)

    @Query("DELETE FROM ${ExportFile.TABLE_NAME_EXPORT_FILES} WHERE ${ExportFile.KEY_FILE_NAME} = :fileName")
    suspend fun deleteExportFileByFileName(fileName: String)

    @Query("DELETE FROM ${ExportFile.TABLE_NAME_EXPORT_FILES}")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(exportFiles: List<ExportFile>)

    @Query("SELECT * FROM ${ExportFile.TABLE_NAME_EXPORT_FILES} WHERE ${ExportFile.KEY_FILE_NAME} = :fileName")
    suspend fun getExportFileByFileName(fileName: String): List<ExportFile>

    @Query("SELECT * FROM ${ExportFile.TABLE_NAME_EXPORT_FILES}")
    suspend fun getExportFiles(): List<ExportFile>

    @Query("SELECT COUNT(*) FROM ${ExportFile.TABLE_NAME_EXPORT_FILES} WHERE ${ExportFile.KEY_IS_PROCESSING} = 0")
    fun getExportFileCount(): Flow<Long>
}
