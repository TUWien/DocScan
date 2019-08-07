package at.ac.tuwien.caa.docscan.camera

import android.content.DialogInterface
import android.view.WindowManager

class FixedActionSheet(sheetActions: ArrayList<SheetAction>, selectionListener: SheetSelection,
                       var dialogListener: DialogStatus?) :
        ActionSheet(sheetActions, selectionListener) {

    override fun sheetClicked(sheetAction: SheetAction) {
//        super.sheetClicked(sheetAction)
//        Just tell the listener, but do not close the dialog. So do not call super.sheetClicked
        listener?.onSheetSelected(sheetAction)
    }

    override fun onStart() {
//        Don't dim the background, because we need to see what is happening there:
        super.onStart()
        val window = dialog.window
        val windowParams = window!!.attributes
        windowParams.dimAmount = 0f
        windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window.attributes = windowParams

//        dialogListener?.onShown()
    }

    override fun onResume() {
        super.onResume()
        dialogListener?.onShown()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        dialogListener?.onDismiss()
    }

    interface DialogStatus {
        fun onShown()
        fun onDismiss()
    }


}


