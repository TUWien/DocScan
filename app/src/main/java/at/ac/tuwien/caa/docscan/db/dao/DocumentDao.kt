package at.ac.tuwien.caa.docscan.db.dao

import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface DocumentDao {
    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS}")
    fun getAllDocumentWithPagesAsFlow(): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS}")
    fun getAllDocumentWithPages(): List<DocumentWithPages>

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE NOT ${Document.KEY_LOCK_STATE} = :state")
    fun getAllLockedDocumentWithPages(state: LockState = LockState.NONE): List<DocumentWithPages>

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_ID} =:documentId")
    suspend fun getDocumentWithPages(documentId: UUID): DocumentWithPages?

    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_ID} =:documentId")
    suspend fun getDocument(documentId: UUID): Document?

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_ID} =:documentId")
    fun getDocumentWithPagesAsFlow(documentId: UUID): Flow<DocumentWithPages?>

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_ID} =:pageId")
    fun getPageAsFlow(pageId: UUID): Flow<Page?>

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_ID} =:pageId")
    fun getPage(pageId: UUID): Page?

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_IS_ACTIVE} = 1")
    fun getActiveDocumentasFlow(): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_IS_ACTIVE} = 1")
    fun getActiveDocument(): DocumentWithPages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDocument(document: Document)

    @Delete
    fun deleteDocument(document: Document)

    @Delete
    fun deletePage(page: Page)

    @Delete
    fun deletePages(page: List<Page>)

    @Query("UPDATE ${Document.TABLE_NAME_DOCUMENTS} SET ${Document.KEY_IS_ACTIVE}=0")
    fun setAllDocumentsInactive()

    @Query("UPDATE ${Document.TABLE_NAME_DOCUMENTS} SET ${Document.KEY_TRANSKRIBUS_UPLOAD_ID}=:uploadId WHERE ${Document.KEY_ID} =:documentId")
    fun updateUploadIdForDoc(documentId: UUID, uploadId: Int?)

    @Query("UPDATE ${Document.TABLE_NAME_DOCUMENTS} SET ${Document.KEY_IS_ACTIVE}=1 WHERE ${Document.KEY_ID} =:documentId")
    fun setDocumentActive(documentId: UUID)

    @Query("SELECT * FROM ${Document.TABLE_NAME_DOCUMENTS} WHERE ${Document.KEY_TITLE} =:documentTitle")
    fun getDocumentByTitle(documentTitle: String): Document?

    @Query("UPDATE ${Document.TABLE_NAME_DOCUMENTS} SET ${Document.KEY_LOCK_STATE} = :state WHERE ${Document.KEY_ID} = :documentId")
    fun setDocumentLock(documentId: UUID, state: LockState)
}