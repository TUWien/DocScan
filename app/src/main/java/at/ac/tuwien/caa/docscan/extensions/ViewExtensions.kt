package at.ac.tuwien.caa.docscan.extensions

import android.os.Build
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar


fun View.snackbar(snackbarOptions: SnackbarOptions): Snackbar {
    return Snackbar.make(this, snackbarOptions.text, snackbarOptions.duration).apply {
        if (snackbarOptions.backgroundTint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setBackgroundTint(context.getColor(snackbarOptions.backgroundTint))
            } else {
                setBackgroundTint(ContextCompat.getColor(context, snackbarOptions.backgroundTint))
            }
        }
        show()
    }
}

data class SnackbarOptions(
    val text: String,
    @BaseTransientBottomBar.Duration val duration: Int = Snackbar.LENGTH_SHORT,
    @ColorRes val backgroundTint: Int? = null
)

/**
 * PreCondition: [RecyclerView] must have a set [LinearLayoutManager]
 *
 * Scrolls to a specific position in the [RecyclerView] if the item position
 * in question is not already visible. If it is not visible the list tries to show the
 * item position at the top.
 *
 */
fun RecyclerView.scrollToPositionIfNotVisible(position: Int) {
    if (position == RecyclerView.NO_POSITION) return
    (layoutManager as? LinearLayoutManager)?.let {
        if (position < it.findFirstCompletelyVisibleItemPosition() || position > it.findLastCompletelyVisibleItemPosition()) {
            it.scrollToPositionWithOffset(position, 0)
        }
    }
}
