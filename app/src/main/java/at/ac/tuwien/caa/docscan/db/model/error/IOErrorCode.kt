package at.ac.tuwien.caa.docscan.db.model.error

/**
 * Represents internal error codes for IO errors.
 */
enum class IOErrorCode {
    FILE_MISSING,
    FILE_COPY_ERROR,
    APPLY_EXIF_ROTATION_ERROR,
    CROPPING_FAILED,
    SINGLE_PAGE_DETECTION_FAILED
}
