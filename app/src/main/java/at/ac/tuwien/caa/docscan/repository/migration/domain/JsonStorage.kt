package at.ac.tuwien.caa.docscan.repository.migration.domain

import com.google.gson.annotations.SerializedName

data class JsonStorage(
    @SerializedName("mDocuments")
    val documents: List<JsonDocument> = listOf(),

    /**
     * Represents the title of the currently active document.
     */
    @SerializedName("mTitle")
    val title: String? = null,
)
