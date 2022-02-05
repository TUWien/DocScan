package at.ac.tuwien.caa.docscan.extensions

import java.text.SimpleDateFormat
import java.util.*

private const val DATE_FORMAT_TIMESTAMP = "yyyyMMdd_HHmmss"
const val DATE_FORMAT_EXPORT_FILE = "MMM dd, yyyy HH:mm:ss"

fun Date.getTimeStamp(): String {
    return SimpleDateFormat(DATE_FORMAT_TIMESTAMP, Locale.getDefault()).format(this)
}

fun Long.asTimeStamp(dateFormat: String = DATE_FORMAT_TIMESTAMP): String {
    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(this)
}
