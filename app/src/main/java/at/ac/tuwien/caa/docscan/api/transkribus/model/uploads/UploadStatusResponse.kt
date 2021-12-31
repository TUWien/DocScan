package at.ac.tuwien.caa.docscan.api.transkribus.model.uploads

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UploadStatusResponse(
    @SerializedName("uploadId")
    val uploadId: Int,
    @SerializedName("colId")
    val colId: Int,
    @SerializedName("jobId")
    val jobId: Int?,
    @SerializedName("pageList")
    val pageList: UploadStatusPageList
)

@Keep
data class UploadStatusPageList(
    @SerializedName("pages")
    val pages: List<UploadStatusPage>
)

@Keep
data class UploadStatusPage(
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("pageUploaded")
    val pageUploaded: Boolean,
    @SerializedName("pageNr")
    val pageNr: Int
)
