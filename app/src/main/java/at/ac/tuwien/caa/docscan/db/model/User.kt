package at.ac.tuwien.caa.docscan.db.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = User.TABLE_NAME_USERS)
data class User(
    @PrimaryKey
    @ColumnInfo(name = KEY_ID)
    val id: Int = USER_ID,
    @ColumnInfo(name = KEY_FIRST_NAME)
    val firstName: String,
    @ColumnInfo(name = KEY_LAST_NAME)
    val lastName: String,
    @ColumnInfo(name = KEY_USER_NAME)
    val userName: String
) {
    companion object {
        const val USER_ID = 1

        const val TABLE_NAME_USERS = "users"
        const val KEY_ID = "id"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_USER_NAME = "user_name"
    }
}
