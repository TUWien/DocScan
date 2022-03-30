package at.ac.tuwien.caa.docscan.extensions

fun Int.sizeMB(): String {
    val fileSize: Float = (this / (1024 * 1024).toFloat())
    return "%.1f".format(fileSize)
}
