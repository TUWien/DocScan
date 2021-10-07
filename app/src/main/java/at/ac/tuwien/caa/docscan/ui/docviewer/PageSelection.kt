package at.ac.tuwien.caa.docscan.ui.docviewer

import at.ac.tuwien.caa.docscan.db.model.Page

data class PageSelection(val page: Page, var isSelected: Boolean)