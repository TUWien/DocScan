package at.ac.tuwien.caa.docscan.db.model.error

import androidx.annotation.StringRes
import at.ac.tuwien.caa.docscan.R

enum class DBErrorCode(@StringRes val titleStringResId: Int, @StringRes val textStringResId: Int) {
    GENERIC(R.string.generic_error_title, R.string.generic_error_text),
    DOCUMENT_LOCKED(R.string.error_document_busy_title, R.string.error_document_busy_text),
    DOCUMENT_PARTIALLY_LOCKED(R.string.error_page_busy_title, R.string.error_page_busy_text),
    DOCUMENT_ALREADY_UPLOADED(
        R.string.viewer_document_uploaded_title,
        R.string.viewer_document_uploaded_title
    ),
    DOCUMENT_NOT_CROPPED(
        R.string.viewer_document_error_not_cropped_title,
        R.string.viewer_document_error_not_cropped_text
    ),
    DOCUMENT_DIFFERENT_UPLOAD_EXPECTATIONS(
        R.string.error_different_upload_expectations_title,
        R.string.error_different_upload_expectations_text
    ),
    DOCUMENT_PAGE_FILE_FOR_UPLOAD_MISSING(
        R.string.error_upload_file_missing_title,
        R.string.error_upload_file_missing_text
    ),
    DOCUMENT_PAGE_FILE_FOR_EXPORT_MISSING(
        R.string.error_export_file_missing_title,
        R.string.error_export_file_missing_text
    ),
    ENTRY_NOT_AVAILABLE(
        R.string.viewer_document_error_missing_entry_title,
        R.string.viewer_document_error_missing_entry_text
    ),
    DUPLICATE(
        R.string.viewer_document_error_duplicate_title,
        R.string.viewer_document_error_duplicate_text
    )
}
