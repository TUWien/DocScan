package at.ac.tuwien.caa.docscan.logic

import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.ui.crop.ImageMeta
import timber.log.Timber
import java.io.File

@WorkerThread
fun calculateImageResolution(file: File, rotation: Rotation): ImageMeta {
    try {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)
        var width = options.outWidth
        var height = options.outHeight
        if (rotation.angle == Rotation.ORIENTATION_90.angle || rotation.angle == Rotation.ORIENTATION_270.angle) {
            val tmp = width
            width = height
            height = tmp
        }
        return ImageMeta(width, height, width / height.toDouble())
    } catch (e: Exception) {
        Timber.e("Could not determine aspect ratio", e)
    }
    return ImageMeta(0, 0, .0)
}
