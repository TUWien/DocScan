package at.ac.tuwien.caa.docscan.camera

import android.location.Location

data class ImageExifMetaData(
    val exifOrientation: Int,
    val exifSoftware: String,
    val exifArtist: String?,
    val exifCopyRight: String?,
    val location: ExifLocation?,
    val resolution: ExifResolution?
)

data class ExifLocation(
    val gpsLat: String,
    val gpsLatRef: String,
    val gpsLon: String,
    val gpsLonRef: String
)

data class ExifResolution(
    val x: String,
    val y: String
)
