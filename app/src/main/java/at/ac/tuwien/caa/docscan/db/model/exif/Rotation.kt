package at.ac.tuwien.caa.docscan.db.model.exif

import androidx.exifinterface.media.ExifInterface

enum class Rotation(val exifOrientation: Int, val angle: Int) {
    ORIENTATION_NORMAL(ExifInterface.ORIENTATION_NORMAL, 0),
    ORIENTATION_90(ExifInterface.ORIENTATION_ROTATE_90, 90),
    ORIENTATION_180(ExifInterface.ORIENTATION_ROTATE_180, 180),
    ORIENTATION_270(ExifInterface.ORIENTATION_ROTATE_270, 270);

    companion object {

        /**
         * @return [Rotation] by exifOrientation, fallbacks to [Rotation.ORIENTATION_NORMAL]
         */
        fun getRotationByExif(exifOrientation: Int?): Rotation {
            exifOrientation ?: return ORIENTATION_NORMAL
            return values().firstOrNull { rotation ->
                rotation.exifOrientation == exifOrientation
            } ?: ORIENTATION_NORMAL
        }
    }
}
