package at.ac.tuwien.caa.docscan.repository.migration.domain

import com.google.gson.annotations.SerializedName

data class JsonPage(
    @SerializedName("mFile")
    val file: JsonFile
)

data class JsonFile(
    @SerializedName("path")
    val path: String
)
