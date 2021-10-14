package at.ac.tuwien.caa.docscan.db.model.boundary

import android.graphics.PointF
import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointF(
    @ColumnInfo(name = KEY_X)
    val x: Float,
    @ColumnInfo(name = KEY_Y)
    val y: Float
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
