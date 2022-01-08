package at.ac.tuwien.caa.docscan.extensions

import android.os.Build
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
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
