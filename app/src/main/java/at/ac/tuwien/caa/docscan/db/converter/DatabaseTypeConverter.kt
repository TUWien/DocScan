package at.ac.tuwien.caa.docscan.db.converter

import androidx.room.TypeConverter
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import java.util.*

class DatabaseTypeConverter {

    @TypeConverter
    fun fromStringToUUID(value: String): UUID {
        return UUID.fromString(value)
    }

    @TypeConverter
    fun fromUUIDtoString(value: UUID): String {
        return value.toString()
    }

    @TypeConverter
    fun fromIntToRotation(exifRotation: Int): Rotation {
        return Rotation.getRotationByExif(exifRotation)
    }

    @TypeConverter
    fun fromRotationToExifInt(rotation: Rotation): Int {
        return rotation.exifOrientation
    }

    @TypeConverter
    fun fromStringToPostProcessing(id: String): PostProcessingState {
        return PostProcessingState.getProcessingStateById(id)
    }

    @TypeConverter
    fun fromPostProcessingToString(postProcessingState: PostProcessingState): String {
        return postProcessingState.id
    }
}
