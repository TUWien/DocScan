package at.ac.tuwien.caa.docscan.api.transkribus.model.collection

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DocResponse(
    @SerializedName("docId") val docId: Int,
    @SerializedName("nrOfPages") val numberOfPages: Int,
    @SerializedName("title") val title: String,
)
