package at.ac.tuwien.caa.docscan.db.model.error

import androidx.annotation.Keep
import androidx.annotation.StringRes
import at.ac.tuwien.caa.docscan.R

/**
 * Represents internal error codes for IO errors.
 */
@Keep
enum class IOErrorCode(
    @StringRes val titleStringResId: Int,
    @StringRes val textStringResId: Int,
    val needsInvestigation: Boolean
) {
    FILE_MISSING(R.string.error_missing_file_title, R.string.error_missing_file_text, false),
    EXPORT_FILE_MISSING_PERMISSION(
        R.string.error_export_missing_permission_title,
        R.string.error_export_missing_permission_text,
        false
    ),
    EXPORT_GOOGLE_PLAYSTORE_NOT_INSTALLED_FOR_OCR(
        R.string.gallery_confirm_no_ocr_available_title,
        R.string.gallery_confirm_no_ocr_available_text,
        false
    ),

    // generic title/message, since the these errors needs to be investigated via the exception.
    FILE_COPY_ERROR(R.string.generic_error_title, R.string.generic_error_text, true),
    NOT_ENOUGH_DISK_SPACE(R.string.generic_error_title, R.string.generic_error_text, true),
    PARSING_FAILED(R.string.generic_error_title, R.string.generic_error_text, true),
    CROPPING_FAILED(R.string.generic_error_title, R.string.generic_error_text, true),
    SINGLE_PAGE_DETECTION_FAILED(R.string.generic_error_title, R.string.generic_error_text, true),
    ML_KIT_OCR_ANALYSIS_FAILED(R.string.generic_error_title, R.string.generic_error_text, true),
    APPLY_EXIF_ROTATION_ERROR(R.string.generic_error_title, R.string.generic_error_text, true),
    SHARE_URI_FAILED(R.string.generic_error_title, R.string.generic_error_text, true),
    EXPORT_CREATE_PDF_FAILED(
        R.string.generic_export_error_title,
        R.string.generic_export_error_text,
        true
    ),
    EXPORT_CREATE_URI_DOCUMENT_FAILED(
        R.string.generic_export_error_title,
        R.string.generic_export_error_text,
        true
    ),
    EXPORT_LOGS_FAILED(R.string.generic_error_title, R.string.generic_error_text, true)
}
