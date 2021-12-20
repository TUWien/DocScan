package at.ac.tuwien.caa.docscan.sync.transkribus.model.uploads

import androidx.annotation.Keep
import at.ac.tuwien.caa.docscan.db.model.MetaData
import com.google.gson.annotations.SerializedName

// TODO: Check the old implementation for possibly missing params
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
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("genre")
    val genre: String? = null
)

fun MetaData.toUploadMetaData(title: String): UploadMetaData {
    // TODO: Add missing data!
    return UploadMetaData(title = title, author = this.author, genre = this.genre)
}
