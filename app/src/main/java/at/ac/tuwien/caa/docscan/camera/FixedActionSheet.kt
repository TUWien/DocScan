package at.ac.tuwien.caa.docscan.camera

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import at.ac.tuwien.caa.docscan.R

class FixedActionSheet(sheetActions: ArrayList<SheetAction>, selectionListener: SheetSelection,
                       dialogListener: DialogStatus, var confirmListener: DialogConfirm) :
        ActionSheet(sheetActions, selectionListener, dialogListener) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fixed_sheet_dialog_camera, container, false)

        val okButton = view.findViewById<AppCompatButton>(R.id.fixed_sheet_ok_button)
        okButton.setOnClickListener {
            dismiss()
        }

        val cancelButton = view.findViewById<AppCompatButton>(R.id.fixed_sheet_cancel_button)
        cancelButton.setOnClickListener {
            dismiss()
            confirmListener.onCancel()
        }

        return view
//        return inflater.inflate(R.layout.sheet_dialog_camera, container, false)
    }

    override fun sheetClicked(sheetAction: SheetAction) {
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
    }


    interface DialogConfirm {
        fun onOk()
        fun onCancel()
    }


//    interface DialogStatus {
//        fun onShown()
//        fun onDismiss()
//    }


}


