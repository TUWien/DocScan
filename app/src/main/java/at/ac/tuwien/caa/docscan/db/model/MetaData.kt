package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class MetaData(

    /**
     * Represents the related upload id, this is usually available when a document is created
     * from a QR-code, which can be then used for uploads so that a document is associated to
     * specific document in the backend already.
     */
    @ColumnInfo(name = KEY_RELATED_UPLOAD_ID)
    var relatedUploadId: Int? = null,
    /**
     * Transkribus author related tag
     */
    @ColumnInfo(name = KEY_AUTHOR)
    var author: String?,
    /**
     * Transkribus authority related tag
     */
    @ColumnInfo(name = KEY_AUTHORITY)
    var authority: String?,
    /**
     * Transkribus authority related tag
     */
    @ColumnInfo(name = KEY_HIERARCHY)
    var hierarchy: String?,
    /**
     * Transkribus genre related tag
     */
    @ColumnInfo(name = KEY_GENRE)
    var genre: String?,
    /**
     * Transkribus language related tag
     */
    @ColumnInfo(name = KEY_LANGUAGE)
    var language: String?,
    /**
     * Transkribus isProjectReadme2020 related tag
     */
    @ColumnInfo(name = KEY_IS_PROJECT_README_2020)
    var isProjectReadme2020: Boolean,
    /**
     * Transkribus allowImagePublication related tag
     */
    @ColumnInfo(name = KEY_ALLOW_IMAGE_PUBLICATION)
    var allowImagePublication: Boolean,
    /**
     * Transkribus signature related tag
     */
    @ColumnInfo(name = KEY_SIGNATURE)
    var signature: String?,
    /**
     * Transkribus url related tag
     */
    @ColumnInfo(name = KEY_URL)
    var url: String?,
    /**
     * Transkribus writer related tag
     */
    @ColumnInfo(name = KEY_WRITER)
    var writer: String?,
    /**
     * Transkribus description tag (only set from the QR-code)
     */
    @ColumnInfo(name = KEY_DESCRIPTION)
    var description: String?
) : Parcelable {
    companion object {
        const val KEY_RELATED_UPLOAD_ID = "related_upload_id"
        const val KEY_AUTHOR = "author"
        const val KEY_WRITER = "writer"
        const val KEY_DESCRIPTION = "description"
        const val KEY_GENRE = "genre"
        const val KEY_SIGNATURE = "signature"
        const val KEY_AUTHORITY = "authority"
        const val KEY_HIERARCHY = "hierarchy"
        const val KEY_URL = "url"
        const val KEY_LANGUAGE = "language"
        const val KEY_IS_PROJECT_README_2020 = "is_project_readme_2020"
        const val KEY_ALLOW_IMAGE_PUBLICATION = "allow_image_publication"
    }
}
