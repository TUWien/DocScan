package at.ac.tuwien.caa.docscan.extensions

import android.view.View
import androidx.databinding.BindingAdapter

private fun isVisible(any: Any?): Boolean {
    return any?.let {
        when (it) {
            is Boolean -> {
                it
            }
            is String -> {
                it.trim().isNotEmpty()
            }
            else -> true
        }
    } ?: false
}

@BindingAdapter("visible")
fun View.bindVisible(any: Any?) {
    visibility = if (isVisible(any)) View.VISIBLE else View.GONE
}

@BindingAdapter("hidden")
fun View.bindHidden(any: Any?) {
    visibility = if (!isVisible(any)) View.VISIBLE else View.GONE
}

@BindingAdapter("invisible")
fun View.bindInvisible(any: Any?) {
    visibility = if (isVisible(any)) View.INVISIBLE else View.VISIBLE
}
