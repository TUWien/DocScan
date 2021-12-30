package at.ac.tuwien.caa.docscan.extensions

import timber.log.Timber
import java.util.*

fun String.asUUID(): UUID? {
    return try {
        UUID.fromString(this)
    } catch (e: Exception) {
        Timber.e("Could not parse $this as UUID!")
        null
    }
}