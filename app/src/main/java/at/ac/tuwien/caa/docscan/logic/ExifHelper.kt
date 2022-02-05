package at.ac.tuwien.caa.docscan.logic

import androidx.exifinterface.media.ExifInterface
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import timber.log.Timber
import java.io.File

fun applyExifData(file: File, exifMetaData: ImageExifMetaData) {
    try {
        val exif = ExifInterface(file)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            exifMetaData.exifOrientation.toString()
        )
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, exifMetaData.exifSoftware)
        exifMetaData.exifArtist?.let {
            exif.setAttribute(ExifInterface.TAG_ARTIST, it)
        }
        exifMetaData.exifCopyRight?.let {
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, it)
        }
        exifMetaData.location?.let {
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, it.gpsLat)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, it.gpsLatRef)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, it.gpsLon)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, it.gpsLonRef)
        }
        exifMetaData.resolution?.let {
            exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, it.x)
            exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, it.y)
        }
        exif.saveAttributes()
    } catch (exception: Exception) {
        Timber.e(exception)
    }
}

fun applyRotation(file: File, rotation: Rotation) {
    try {
        val exif = ExifInterface(file)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            rotation.exifOrientation.toString()
        )
        exif.saveAttributes()
    } catch (exception: Exception) {
        Timber.e(exception, "Couldn't save exif attributes!")
    }
}

fun applyRotationResource(file: File, rotation: Rotation): Resource<Unit> {
    return try {
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, rotation.exifOrientation.toString())
        exif.saveAttributes()
        Success(Unit)
    } catch (exception: Exception) {
        Timber.e(exception, "Unable to apply exif orientation!")
        IOErrorCode.APPLY_EXIF_ROTATION_ERROR.asFailure(exception)
    }
}

fun getRotation(file: File): Rotation {
    return try {
        val exif = ExifInterface(file)
        Rotation.getRotationByExif(
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        )
    } catch (exception: Exception) {
        Timber.e(exception, "Couldn't read orientation exif attribute!")
        Rotation.ORIENTATION_NORMAL
    }
}

fun removeRotation(file: File) {
    try {
        val exif = ExifInterface(file)
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            null
        )
        exif.saveAttributes()
    } catch (exception: Exception) {
        Timber.e(exception, "Couldn't save exif attributes!")
    }
}
