package at.ac.tuwien.caa.docscan.db.converter

import androidx.room.TypeConverter
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
}
