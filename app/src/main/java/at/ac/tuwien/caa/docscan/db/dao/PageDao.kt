package at.ac.tuwien.caa.docscan.db.dao

import androidx.room.*
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPage(page: Page)

    @Delete
    fun removePage(page: Page)

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_ID} = :id")
    suspend fun getPageById(id: UUID): Page?

    @Query("SELECT * FROM ${Page.TABLE_NAME_PAGES} WHERE ${Page.KEY_DOC_ID} = :docId")
    suspend fun getPagesByDoc(docId: UUID): List<Page>
}