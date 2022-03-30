package at.ac.tuwien.caa.docscan.api.transkribus.model.uploads

import androidx.annotation.Keep
import at.ac.tuwien.caa.docscan.db.model.MetaData
import com.google.gson.annotations.SerializedName

@Keep
data class CreateUploadRequestBody(
    @SerializedName("md")
    val metaData: UploadMetaData?,
    @SerializedName("pageList")
    val pageList: UploadPageList
)

@Keep
data class UploadPageList(
    @SerializedName("pages")
    val pages: List<UploadPage>
)

@Keep
data class UploadPage(
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("pageNr")
    val pageNr: Int,
    @SerializedName("imgChecksum")
    val imgChecksum: String? = null
)

@Keep
data class UploadMetaData(
    @SerializedName("title")
    val title: String?,
    @SerializedName("authority")
    val authority: String? = null,
    @SerializedName("hierarchy")
    val hierarchy: String? = null,
    @SerializedName("extid")
    val extid: String? = null,
    @SerializedName("backlink")
    val backlink: String? = null,
    @SerializedName("writer")
    val writer: String? = null,
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("desc")
    val desc: String? = null,
    @SerializedName("language")
    val language: String? = null
)

fun MetaData.toUploadMetaData(title: String): UploadMetaData {
    var desc: String?
    if (isProjectReadme2020) {
        desc = " "
        desc += " #readme2020"
        if (allowImagePublication) desc += " #public"
    } else {
        desc = description
    }
    return UploadMetaData(
        title = title,
        authority = authority,
        hierarchy = hierarchy,
        extid = signature,
        backlink = url,
        writer = writer,
        author = author,
        genre = genre,
        desc = desc,
        language = language
    )
}
