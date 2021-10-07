package at.ac.tuwien.caa.docscan.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.ac.tuwien.caa.docscan.db.model.Page.Companion.TABLE_NAME_PAGES
import java.util.*

@Entity(tableName = TABLE_NAME_PAGES)
data class Page(
    @PrimaryKey
    @ColumnInfo(name = KEY_ID)
    val id: UUID,
    // TODO: Consider adding a foreign key constraint
    @ColumnInfo(name = KEY_DOC_ID)
    val docId: UUID,
    @ColumnInfo(name = KEY_NUMBER)
    val number: Int
    // TODO: Add task type which is currently being performed on that page.
    // TODO: Hash of the image file to determine if it has changed or not.
) {
    companion object {
        const val TABLE_NAME_PAGES = "pages"
        const val KEY_ID = "id"
        const val KEY_DOC_ID = "doc_id"
        const val KEY_NUMBER = "number"
    }
}
