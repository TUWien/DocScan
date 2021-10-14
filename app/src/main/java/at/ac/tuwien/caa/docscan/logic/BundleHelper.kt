package at.ac.tuwien.caa.docscan.logic

import android.os.Bundle
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages

private const val ARG_DOCUMENTS_WITH_PAGES = "ARG_DOCUMENTS_WITH_PAGES"

/**
 * Appends [documentWithPages] to the [Bundle] for key [ARG_DOCUMENTS_WITH_PAGES].
 */
fun Bundle.appendDocWithPages(documentWithPages: DocumentWithPages): Bundle {
    putParcelable(ARG_DOCUMENTS_WITH_PAGES, documentWithPages)
    return this
}

/**
 * Extracts [DocumentWithPages] from the [Bundle] for key [ARG_DOCUMENTS_WITH_PAGES].
 */
fun Bundle.extractDocWithPages(): DocumentWithPages? {
    return getParcelable(ARG_DOCUMENTS_WITH_PAGES)
}
