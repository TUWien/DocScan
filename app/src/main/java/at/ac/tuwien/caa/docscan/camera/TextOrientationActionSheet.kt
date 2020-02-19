package at.ac.tuwien.caa.docscan.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatButton
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.sheet_dialog_camera.*


class TextOrientationActionSheet(sheetActions: ArrayList<SheetAction>, selectionListener: SheetSelection,
                                 dialogListener: DialogStatus, var confirmListener: DialogConfirm,
                                 private val opaque: Boolean = true) :
        ActionSheet(sheetActions, selectionListener, dialogListener) {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val style: Int =
                if (opaque)
                    R.style.BottomSheetDialogDarkTheme
                else
                    R.style.TransparentBottomSheetDialogDarkTheme

        setStyle(STYLE_NORMAL, style)
        sheet_dialog_recyclerview.apply {
            setStyle(STYLE_NORMAL, style)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

//        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)

//        titled_sheet_dialog_pdf
//        val view = inflater.inflate(R.layout.titled_sheet_dialog_camera, container, false)
        val view = inflater.inflate(R.layout.text_dir_sheet_dialog, container, false)

        val okButton = view.findViewById<AppCompatButton>(R.id.text_orientation_sheet_ok_button)
        okButton.setOnClickListener {
            dismiss()
        }

        val cancelButton = view.findViewById<AppCompatButton>(R.id.text_orientation_sheet_cancel_button)
        cancelButton.setOnClickListener {
            dismiss()
            confirmListener.onCancel()
        }

        return view
//        return inflater.inflate(R.layout.sheet_dialog_camera, container, false)
    }


//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
////        Make the dialog semi-transparent
//        sheet_dialog_recyclerview.apply {
//            setStyle(STYLE_NORMAL, R.style.ThemeOverlay_AppCompat_Dark)
//        }
//    }

    override fun sheetClicked(sheetAction: SheetAction) {
//        Just tell the listener, but do not close the dialog. So do not call super.sheetClicked
        listener?.onSheetSelected(sheetAction)
    }

    override fun onStart() {
//        Don't dim the background, because we need to see what is happening there:
        super.onStart()
        val window = dialog?.window
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


