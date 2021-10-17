package at.ac.tuwien.caa.docscan.db.model.boundary

import android.graphics.PointF
import android.os.Parcelable
import androidx.room.ColumnInfo
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointF(
    @ColumnInfo(name = KEY_X)
    var x: Float,
    @ColumnInfo(name = KEY_Y)
    var y: Float
) : Parcelable {
    companion object {
        const val KEY_X = "x"
        const val KEY_Y = "y"
    }
}

fun PointF.asPoint(): at.ac.tuwien.caa.docscan.db.model.boundary.PointF {
    return at.ac.tuwien.caa.docscan.db.model.boundary.PointF(x, y)
}

fun at.ac.tuwien.caa.docscan.db.model.boundary.PointF.asPoint(): PointF {
    return PointF(x, y)
}

fun at.ac.tuwien.caa.docscan.db.model.boundary.PointF.rotateNormedPointBy(
    rotation: Rotation
) {
    when (rotation) {
        Rotation.ORIENTATION_90 -> {
            val tmpY = x
            x = 1 - y
            y = tmpY
        }
        Rotation.ORIENTATION_180 -> {
            x = 1 - x
            y = 1 - y
        }
        Rotation.ORIENTATION_270 -> {
            val tmpX = y
            y = 1 - x
            x = tmpX
        }
        else -> {
            // ignore
        }
    }
}
