package at.ac.tuwien.caa.docscan.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import at.ac.tuwien.caa.docscan.extensions.SnackbarOptions
import at.ac.tuwien.caa.docscan.extensions.snackbar
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import at.ac.tuwien.caa.docscan.ui.dialog.show
import com.google.android.material.snackbar.Snackbar

/**
 * A base activity holding any necessary utility functions that can be re-used across all sub fragments.
 */
open class BaseActivity : AppCompatActivity() {

    private var snackbar: Snackbar? = null

    fun showDialog(dialogAction: ADialog.DialogAction) {
        dialogAction.show(supportFragmentManager)
    }

    fun showDialog(dialogModel: DialogModel) {
        dialogModel.show(supportFragmentManager)
    }

    protected fun singleSnackbar(view: View, snackbarOptions: SnackbarOptions) {
        snackbar?.dismiss()
        snackbar = view.snackbar(snackbarOptions)
    }
}
