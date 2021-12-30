package at.ac.tuwien.caa.docscan.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.ac.tuwien.caa.docscan.db.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User)

    @Query("DELETE FROM ${User.TABLE_NAME_USERS}")
    suspend fun deleteUser()

    @Query("SELECT * FROM ${User.TABLE_NAME_USERS} WHERE ${User.KEY_ID} = ${User.USER_ID}")
    fun getUserAsFlow(): Flow<User?>

    @Query("SELECT * FROM ${User.TABLE_NAME_USERS} WHERE ${User.KEY_ID} = ${User.USER_ID}")
    suspend fun getUser(): User?
}
