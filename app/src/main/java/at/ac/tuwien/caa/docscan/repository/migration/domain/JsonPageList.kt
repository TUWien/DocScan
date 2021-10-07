package at.ac.tuwien.caa.docscan.repository.migration.domain

import com.google.gson.annotations.SerializedName

data class JsonPageList(
    @SerializedName("pages")
    val pages: List<JsonPage> = listOf()
)
