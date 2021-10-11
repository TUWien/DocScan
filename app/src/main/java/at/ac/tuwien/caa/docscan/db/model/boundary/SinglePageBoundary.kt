package at.ac.tuwien.caa.docscan.db.model.boundary

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class SinglePageBoundary(
        @Embedded(prefix = KEY_TOP_LEFT)
        val topLeft: PointF,
        @Embedded(prefix = KEY_TOP_RIGHT)
        val topRight: PointF,
        @Embedded(prefix = KEY_BOTTOM_LEFT)
        val bottomLeft: PointF,
        @Embedded(prefix = KEY_BOTTOM_RIGHT)
        val bottomRight: PointF
) {
    companion object {
        const val KEY_TOP_LEFT = "top_left"
        const val KEY_TOP_RIGHT = "top_right"
        const val KEY_BOTTOM_LEFT = "bottom_left"
        const val KEY_BOTTOM_RIGHT = "bottom_right"

        fun getDefault(): SinglePageBoundary {
            return SinglePageBoundary(PointF(0F, 0F), PointF(1F, 0F), PointF(1F, 1F), PointF(0F, 1F))
        }
    }
}

fun SinglePageBoundary.asClockwiseList(): List<PointF> {
    return listOf(topLeft, topRight, bottomLeft, bottomRight)
}
