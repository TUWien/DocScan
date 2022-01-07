package at.ac.tuwien.caa.docscan.extensions

import androidx.documentfile.provider.DocumentFile

fun DocumentFile.sizeMB(): String {
    val fileSizeInBytes = length()
    val fileSize: Float = (fileSizeInBytes / (1024 * 1024).toFloat())
    return "%.1f".format(fileSize)
}
