package at.ac.tuwien.caa.docscan.logic

import androidx.exifinterface.media.ExifInterface
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation.Companion.getRotationByExif
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
        Timber.e(exception, "Apply exif data has failed!")
    }
}

fun getExifInterface(file: File): ExifInterface? {
    return try {
        ExifInterface(file)
    } catch (e: Exception) {
        Timber.e(e, "Unable to open exif interface!")
        null
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
        getRotationByExif(
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
        Timber.e(exception, "Couldn't remove exif rotation!")
    }
}

fun saveExif(exif: ExifInterface, targetFile: File) {
    try {
        val newExif = ExifInterface(targetFile)
        for (i in attributes.indices) {
            val value = exif.getAttribute(attributes[i])
            if (value != null) newExif.setAttribute(attributes[i], value)
        }
        newExif.resetOrientation()
        newExif.saveAttributes()
    } catch (e: Exception) {
        Timber.e(e, "Couldn't save exif attributes!")
    }
}

/**
 * Represents all exif attributes which are preserved when files are copied.
 */
private val attributes = arrayOf(
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_BITS_PER_SAMPLE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_CFA_PATTERN,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_COMPONENTS_CONFIGURATION,
    ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
    ExifInterface.TAG_COMPRESSION,
    ExifInterface.TAG_CONTRAST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_CUSTOM_RENDERED,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DEFAULT_CROP_SIZE,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_DNG_VERSION,
    ExifInterface.TAG_EXIF_VERSION,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_EXPOSURE_INDEX,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_FILE_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_FLASHPIX_VERSION,
    ExifInterface.TAG_FLASH_ENERGY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
    ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_GAIN_CONTROL,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_DOP,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_MAP_DATUM,
    ExifInterface.TAG_GPS_MEASURE_MODE,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_SATELLITES,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_STATUS,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_IMAGE_DESCRIPTION,  //                        ExifInterface.TAG_IMAGE_LENGTH,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,  //                        ExifInterface.TAG_IMAGE_WIDTH,
    ExifInterface.TAG_INTEROPERABILITY_INDEX,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
    ExifInterface.TAG_LIGHT_SOURCE,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MAKER_NOTE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_NEW_SUBFILE_TYPE,
    ExifInterface.TAG_OECF,
    ExifInterface.TAG_ORF_ASPECT_FRAME,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
    ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,  //                        ExifInterface.TAG_ORIENTATION,
    ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,  //                        ExifInterface.TAG_PIXEL_X_DIMENSION,
    //                        ExifInterface.TAG_PIXEL_Y_DIMENSION,
    ExifInterface.TAG_PLANAR_CONFIGURATION,
    ExifInterface.TAG_PRIMARY_CHROMATICITIES,
    ExifInterface.TAG_REFERENCE_BLACK_WHITE,
    ExifInterface.TAG_RELATED_SOUND_FILE,
    ExifInterface.TAG_RESOLUTION_UNIT,  //                        ExifInterface.TAG_ROWS_PER_STRIP,
    ExifInterface.TAG_RW2_ISO,
    ExifInterface.TAG_RW2_JPG_FROM_RAW,
    ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
    ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
    ExifInterface.TAG_SAMPLES_PER_PIXEL,
    ExifInterface.TAG_SATURATION,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_SCENE_TYPE,
    ExifInterface.TAG_SENSING_METHOD,
    ExifInterface.TAG_SHARPNESS,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
    ExifInterface.TAG_SPECTRAL_SENSITIVITY,  //                        ExifInterface.TAG_STRIP_BYTE_COUNTS,
    //                        ExifInterface.TAG_STRIP_OFFSETS,
    ExifInterface.TAG_SUBFILE_TYPE,
    ExifInterface.TAG_SUBJECT_AREA,
    ExifInterface.TAG_SUBJECT_DISTANCE,
    ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
    ExifInterface.TAG_SUBJECT_LOCATION,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
    ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
    ExifInterface.TAG_TRANSFER_FUNCTION,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_WHITE_POINT,  //                        ExifInterface.TAG_X_RESOLUTION,
    ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
    ExifInterface.TAG_Y_CB_CR_POSITIONING,
    ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING
)
