package at.ac.tuwien.caa.docscan.db.dao

import androidx.annotation.Keep
import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.ExportFile
import kotlinx.coroutines.flow.Flow
import java.util.*

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

    @Query("SELECT * FROM ${ExportFile.TABLE_NAME_EXPORT_FILES} WHERE ${ExportFile.KEY_DOC_ID} = :docId AND ${ExportFile.KEY_IS_PROCESSING} = 1")
    suspend fun getProcessingExportFiles(docId: UUID): List<ExportFile>

    @Query("UPDATE ${ExportFile.TABLE_NAME_EXPORT_FILES} SET ${ExportFile.KEY_IS_PROCESSING}= :isProcessing WHERE ${ExportFile.KEY_FILE_NAME} = :id")
    suspend fun updateProcessingState(id: String, isProcessing: Boolean)

    @Query("UPDATE ${ExportFile.TABLE_NAME_EXPORT_FILES} SET ${ExportFile.KEY_IS_PROCESSING} = 0")
    suspend fun updatesAllProcessingStatesToFalse()

    @Query("SELECT COUNT(*) FROM ${ExportFile.TABLE_NAME_EXPORT_FILES} WHERE ${ExportFile.KEY_IS_PROCESSING} = 0")
    fun getExportFileCount(): Flow<Long>
}
