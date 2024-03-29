package at.ac.tuwien.caa.docscan.repository.migration.domain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class JsonMetaData(
    @SerializedName("mAuthor")
    var author: String? = null,
    @SerializedName("mAuthority")
    var authority: String? = null,
    @SerializedName("mHierarchy")
    var hierarchy: String? = null,
    @SerializedName("mGenre")
    var genre: String? = null,
    @SerializedName("mLanguage")
    var language: String? = null,
    @SerializedName("mReadme2020")
    var readme2020: Boolean = false,
    @SerializedName("mReadme2020Public")
    var readme2020Public: Boolean = false,
    @SerializedName("mSignature")
    var signature: String? = null,
    @SerializedName("mUrl")
    var uri: String? = null,
    @SerializedName("mWriter")
    var writer: String? = null,
    @SerializedName("mDesc")
    var desc: String? = null
)
