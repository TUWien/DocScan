package at.ac.tuwien.caa.docscan.logic

import android.os.Bundle
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.ExportList

private const val ARG_DOCUMENTS_WITH_PAGES = "ARG_DOCUMENTS_WITH_PAGES"
private const val ARG_EXPORT = "ARG_EXPORT"
private const val ARG_IS_CONFIRMED = "ARG_IS_CONFIRMED"
private const val ARG_SKIP_CROP_RESTRICTION = "ARG_SKIP_CROP_RESTRICTION"
private const val ARG_SKIP_ALREADY_UPLOADED_RESTRICTION = "ARG_SKIP_ALREADY_UPLOADED_RESTRICTION"
private const val ARG_EXPORT_USE_OCR = "ARG_EXPORT_USE_OCR"

private const val ARG_CONFIRM_MIGRATION_DIALOG_1 = "ARG_CONFIRM_MIGRATION_DIALOG_1"
private const val ARG_CONFIRM_MIGRATION_DIALOG_2 = "ARG_CONFIRM_MIGRATION_DIALOG_2"

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

/**
 * Appends [isConfirmed] to the [Bundle] for key [ARG_IS_CONFIRMED].
 */
fun Bundle.appendIsConfirmed(isConfirmed: Boolean): Bundle {
    putBoolean(ARG_IS_CONFIRMED, isConfirmed)
    return this
}

/**
 * Extracts [Boolean] from the [Bundle] for key [ARG_IS_CONFIRMED].
 */
fun Bundle.extractIsConfirmed(): Boolean {
    return getBoolean(ARG_IS_CONFIRMED, false)
}

/**
 * Appends [skipCropRestriction] to the [Bundle] for key [ARG_SKIP_CROP_RESTRICTION].
 */
fun Bundle.appendSkipCropRestriction(skipCropRestriction: Boolean): Bundle {
    putBoolean(ARG_SKIP_CROP_RESTRICTION, skipCropRestriction)
    return this
}

/**
 * Extracts [Boolean] from the [Bundle] for key [ARG_SKIP_CROP_RESTRICTION].
 */
fun Bundle.extractSkipCropRestriction(): Boolean {
    return getBoolean(ARG_SKIP_CROP_RESTRICTION, false)
}

/**
 * Appends [skipAlreadyUploadedRestriction] to the [Bundle] for key [ARG_SKIP_ALREADY_UPLOADED_RESTRICTION].
 */
fun Bundle.appendSkipAlreadyUploadedRestriction(skipAlreadyUploadedRestriction: Boolean): Bundle {
    putBoolean(ARG_SKIP_ALREADY_UPLOADED_RESTRICTION, skipAlreadyUploadedRestriction)
    return this
}

/**
 * Extracts [Boolean] from the [Bundle] for key [ARG_SKIP_ALREADY_UPLOADED_RESTRICTION].
 */
fun Bundle.extractSkipAlreadyUploadedRestriction(): Boolean {
    return getBoolean(ARG_SKIP_ALREADY_UPLOADED_RESTRICTION, false)
}

/**
 * Appends [useOCR] to the [Bundle] for key [ARG_EXPORT_USE_OCR].
 */
fun Bundle.appendUseOCR(useOCR: Boolean): Bundle {
    putBoolean(ARG_EXPORT_USE_OCR, useOCR)
    return this
}

/**
 * Extracts [Boolean] from the [Bundle] for key [ARG_EXPORT_USE_OCR].
 */
fun Bundle.extractUseOCR(): Boolean {
    return getBoolean(ARG_EXPORT_USE_OCR, false)
}

/**
 * Appends true to the [Bundle] for key [ARG_CONFIRM_MIGRATION_DIALOG_1].
 */
fun Bundle.appendMigrationDialogOne(): Bundle {
    putBoolean(ARG_CONFIRM_MIGRATION_DIALOG_1, true)
    return this
}

/**
 * Appends true to the [Bundle] for key [ARG_CONFIRM_MIGRATION_DIALOG_2].
 */
fun Bundle.appendMigrationDialogTwo(): Bundle {
    putBoolean(ARG_CONFIRM_MIGRATION_DIALOG_2, true)
    return this
}

/**
 * Appends true to the [Bundle] for key [ARG_CONFIRM_MIGRATION_DIALOG_1].
 */
fun Bundle.extractMigrationDialogOne(): Boolean {
    return getBoolean(ARG_CONFIRM_MIGRATION_DIALOG_1)
}

/**
 * Appends true to the [Bundle] for key [ARG_CONFIRM_MIGRATION_DIALOG_2].
 */
fun Bundle.extractMigrationDialogTwo(): Boolean {
    return getBoolean(ARG_CONFIRM_MIGRATION_DIALOG_2)
}
