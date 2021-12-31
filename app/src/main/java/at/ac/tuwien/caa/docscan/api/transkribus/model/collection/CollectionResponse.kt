package at.ac.tuwien.caa.docscan.api.transkribus.model.collection

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CollectionResponse(
    @SerializedName("colId")
    val id: Int,
    @SerializedName("colName")
    val name: String,
)
