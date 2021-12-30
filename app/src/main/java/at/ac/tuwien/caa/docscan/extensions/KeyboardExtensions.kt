package at.ac.tuwien.caa.docscan.extensions

import android.app.Activity
import android.content.Context
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Hides the keyboard for the current focused view.
 */
fun View.hideKeyboard() {
    hideKeyboard(context, windowToken)
}

/**
 * Hides the keyboard if the activity holds the current focus on a view.
 */
fun Activity?.hideKeyboard() {
    if (this != null) {
        val focusedView = this.currentFocus
        if (focusedView != null) {
            hideKeyboard(focusedView.context, focusedView.windowToken)
        }
    }
}

private fun hideKeyboard(context: Context, windowToken: IBinder?) {
    val imm = context.getSystemService(
        Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Shows the keyboard for a currently focused view.
 */
fun View.showKeyboard() {
    val imm = context.getSystemService(
        Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_FORCED)
}
