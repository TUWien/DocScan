package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Upload(
    /**
     * The upload state of the entity.
     */
    @ColumnInfo(name = KEY_UPLOAD_STATE)
    val state: UploadState = UploadState.NONE,

    /**
     * The unique target fileName for the upload.
     */
    @ColumnInfo(name = KEY_UPLOAD_FILE_NAME)
    val uploadFileName: String? = null

) : Parcelable {
    companion object {
        const val KEY_UPLOAD_STATE = "upload_state"
        const val KEY_UPLOAD_FILE_NAME = "upload_file_name"
    }
}
