package at.ac.tuwien.caa.docscan.db.dao

import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import java.util.*

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

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_DOC_ID} = :docId")
    suspend fun getPagesByDoc(docId: UUID): List<Page>

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_POST_PROCESSING_STATE}= :state WHERE ${Page.KEY_ID} = :pageId ")
    fun updatePageProcessingState(pageId: UUID, state: PostProcessingState)

    @Query("UPDATE ${Page.TABLE_NAME_PAGES} SET ${Page.KEY_POST_PROCESSING_STATE}= :state WHERE ${Page.KEY_DOC_ID} = :docId ")
    fun updatePageProcessingStateForDocument(docId: UUID, state: PostProcessingState)
}
