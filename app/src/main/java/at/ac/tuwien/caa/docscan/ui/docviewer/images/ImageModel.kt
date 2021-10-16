package at.ac.tuwien.caa.docscan.ui.docviewer.images

import at.ac.tuwien.caa.docscan.db.model.Document

data class ImageModel(
    val document: Document?,
    val pages: List<PageSelection>,
    var scrollTo: Int = -1
)
