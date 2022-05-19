package at.ac.tuwien.caa.docscan.db.dao

import androidx.annotation.Keep
import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.Upload
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import java.util.*

@Keep
@Dao
interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPage(page: Page)

    @Delete
    fun deletePage(page: Page)

    @Delete
    fun deletePages(page: List<Page>)

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_ID} = :id")
    suspend fun getPageById(id: UUID): Page?

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_ID} = :id")
    fun getPageByIdNonSuspendable(id: UUID): Page?

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_LEGACY_ABSOLUTE_FILE_PATH} = :legacyFilePath AND ${Page.KEY_DOC_ID} = :docId")
    suspend fun getPageByLegacyFilePath(docId: UUID, legacyFilePath: String): List<Page>

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_DOC_ID} = :docId")
    suspend fun getPagesByDoc(docId: UUID): List<Page>

    @Query("SELECT DISTINCT ${Page.KEY_DOC_ID} FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_UPLOAD_PREFIX}${Upload.KEY_UPLOAD_STATE} = :state OR ${Page.KEY_UPLOAD_PREFIX}${Upload.KEY_UPLOAD_STATE} = :stateTwo")
    suspend fun getAllDocIdsWithPendingUploadState(state: UploadState = UploadState.SCHEDULED, stateTwo: UploadState = UploadState.UPLOAD_IN_PROGRESS): List<UUID>

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_POST_PROCESSING_STATE}= :state WHERE ${Page.KEY_ID} = :pageId ")
    fun updatePageProcessingState(pageId: UUID, state: PostProcessingState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_POST_PROCESSING_STATE}= :state WHERE ${Page.KEY_DOC_ID} = :docId ")
    fun updatePageProcessingStateForDocument(docId: UUID, state: PostProcessingState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_EXPORT_STATE}= :state WHERE ${Page.KEY_DOC_ID} = :docId ")
    fun updatePageExportStateForDocument(docId: UUID, state: ExportState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_UPLOAD_PREFIX}${Upload.KEY_UPLOAD_STATE} = :state WHERE ${Page.KEY_ID} = :pageId ")
    fun updateUploadState(pageId: UUID, state: UploadState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_EXPORT_STATE} = :state WHERE ${Page.KEY_ID} = :pageId")
    fun updateExportState(pageId: UUID, state: ExportState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_UPLOAD_PREFIX}${Upload.KEY_UPLOAD_STATE} = :state WHERE ${Page.KEY_DOC_ID} = :docId ")
    fun updateUploadStateForDocument(docId: UUID, state: UploadState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_UPLOAD_PREFIX}${Upload.KEY_UPLOAD_FILE_NAME} = null WHERE ${Page.KEY_DOC_ID} = :docId ")
    suspend fun clearDocumentPagesUploadFileNames(docId: UUID)
}
