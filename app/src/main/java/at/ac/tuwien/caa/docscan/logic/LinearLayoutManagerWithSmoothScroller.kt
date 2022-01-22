package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.graphics.PointF
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class LinearLayoutManagerWithSmoothScroller(context: Context) :
    LinearLayoutManager(context, RecyclerView.VERTICAL, false) {

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val smoothScroller = TopSnappedSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    private inner class TopSnappedSmoothScroller(context: Context) : LinearSmoothScroller(context) {

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }

        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            return this@LinearLayoutManagerWithSmoothScroller
                .computeScrollVectorForPosition(targetPosition)
        }
    }
}
