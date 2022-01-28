package at.ac.tuwien.caa.docscan.repository.migration.domain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class JsonPage(
    @SerializedName("mFile")
    val file: JsonFile
)

@Keep
data class JsonFile(
    @SerializedName("path")
    val path: String
)
