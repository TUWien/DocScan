package at.ac.tuwien.caa.docscan.ui.docviewer.images

import at.ac.tuwien.caa.docscan.db.model.Page

data class PageSelection(val page: Page, var isSelectionActivated: Boolean, var isSelected: Boolean)