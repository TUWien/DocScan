package at.ac.tuwien.caa.docscan.repository.migration.domain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class JsonDocument(
    /**
     * Represents the title of the document.
     */
    @SerializedName("mTitle")
    val title: String,
//    @SerializedName("mUseCustomFileName")
//    val useCustomFileName: Boolean = false,
    @SerializedName("mMetaData")
    val jsonMetaData: JsonMetaData? = null,
    @SerializedName("mIsAwaitingUpload")
    val isAwaitingUpload: Boolean = false,
    @SerializedName("mIsCurrentlyProcessed")
    val isCurrentlyProcessed: Boolean = false,
    @SerializedName("mIsUploaded")
    val isUploaded: Boolean = false,
    @SerializedName("mPages")
    val pages: List<JsonPage> = listOf()
)
