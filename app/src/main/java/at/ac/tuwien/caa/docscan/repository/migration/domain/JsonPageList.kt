package at.ac.tuwien.caa.docscan.repository.migration.domain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class JsonPageList(
    @SerializedName("pages")
    val pages: List<JsonPage> = listOf()
)
