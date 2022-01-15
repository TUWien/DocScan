package at.ac.tuwien.caa.docscan.ui.base

import androidx.fragment.app.Fragment
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import at.ac.tuwien.caa.docscan.ui.dialog.show

open class BaseFragment : Fragment() {

    fun setTitle(title: String) {
        if (isAdded && !isRemoving) {
            (requireActivity() as BaseActivity).supportActionBar?.title = title
        }
    }

    fun showDialog(dialogAction: ADialog.DialogAction) {
        dialogAction.show(childFragmentManager)
    }

    fun showDialog(dialogModel: DialogModel) {
        dialogModel.show(childFragmentManager)
    }

    protected fun requireBaseActivity(): BaseActivity {
        return requireActivity() as BaseActivity
    }
}
