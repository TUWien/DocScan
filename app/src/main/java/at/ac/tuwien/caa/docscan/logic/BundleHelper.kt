package at.ac.tuwien.caa.docscan.logic

import android.os.Bundle
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.ExportList

private const val ARG_DOCUMENTS_WITH_PAGES = "ARG_DOCUMENTS_WITH_PAGES"
private const val ARG_EXPORT = "ARG_EXPORT"

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

/**
 * Appends [exportList] to the [Bundle] for key [ARG_DOCUMENTS_WITH_PAGES].
 */
fun Bundle.appendExportFile(exportList: ExportList.File?): Bundle {
    putParcelable(ARG_EXPORT, exportList)
    return this
}

/**
 * Extracts [DocumentWithPages] from the [Bundle] for key [ARG_DOCUMENTS_WITH_PAGES].
 */
fun Bundle.extractExportFile(): ExportList.File? {
    return getParcelable(ARG_EXPORT)
}
