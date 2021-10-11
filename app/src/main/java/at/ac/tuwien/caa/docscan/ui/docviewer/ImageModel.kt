package at.ac.tuwien.caa.docscan.ui.docviewer

data class ImageModel(val pages: List<PageSelection>, var scrollTo: Int = -1)
