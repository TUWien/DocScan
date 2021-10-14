package at.ac.tuwien.caa.docscan.db.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

/**
 * TODO: The UI introduced a prefix, is it still relevant? (Is it even relevant for the upload?) it's probably relevant for the new export of images
 */
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
    var writer: String?
) : Parcelable {
    companion object {
        const val KEY_RELATED_UPLOAD_ID = "related_upload_id"
        const val KEY_AUTHOR = "author"
        const val KEY_WRITER = "writer"
        const val KEY_GENRE = "genre"
        const val KEY_SIGNATURE = "signature"
        const val KEY_AUTHORITY = "authority"
        const val KEY_URL = "url"
        const val KEY_LANGUAGE = "language"
        const val KEY_IS_PROJECT_README_2020 = "is_project_readme_2020"
        const val KEY_ALLOW_IMAGE_PUBLICATION = "allow_image_publication"
    }
}

// TODO: Check if the values are correctly represented
//private void setMetaDataValues(TranskribusMetaData md) {
//
////            mTitle = md.getTitle();
//    mAuthority = md.getAuthority();
//    mHierarchy = md.getHierarchy();
//    mSignature = md.getSignature();
//    mUri = md.getLink();
//    mWriter = md.getWriter();
//    mAuthor = md.getAuthor();
//    mGenre = md.getGenre();
//    mDesc = md.getDescription();
//
//    if (md.getReadme2020()) {
//        if (mDesc != null)
//            mDesc = " ";
//        else
//            mDesc = "";
//
//        mDesc += " #readme2020";
//        if (md.getReadme2020Public())
//            mDesc += " #public";
//    }
//    mLanguage = md.getLanguage();
//
//}


//@SerializedName("title")
//private var title: String? = null,
//@SerializedName("authority")
//private val authority: String? = null,
//@SerializedName("hierarchy")
//private val hierarchy: String? = null,
//@SerializedName("extid")
//private val signature: String? = null,
//@SerializedName("backlink")
//private val uri: String? = null,
//@SerializedName("writer")
//private val writer: String? = null,

//@SerializedName("genre")
//private val genre: String? = null,
//@SerializedName("desc")
//private val desc: String? = null,
//@SerializedName("language")
//private var language: String? = null