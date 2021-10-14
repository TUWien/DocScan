package at.ac.tuwien.caa.docscan.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.Nullable
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.TextDirSheetDialogBinding


class TextOrientationActionSheet(
    sheetActions: ArrayList<SheetAction>, selectionListener: SheetSelection,
    dialogListener: DialogStatus, var confirmListener: DialogConfirm,
    private val opaque: Boolean = true
) : ActionSheet(sheetActions, selectionListener, dialogListener) {

    private lateinit var binding: TextDirSheetDialogBinding

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val style: Int =
            if (opaque)
                R.style.BottomSheetDialogDarkTheme
            else
                R.style.TransparentBottomSheetDialogDarkTheme

        setStyle(STYLE_NORMAL, style)
        binding.sheetDialogRecyclerview.apply {
            setStyle(STYLE_NORMAL, style)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TextDirSheetDialogBinding.inflate(layoutInflater, container, false)
        binding.apply {
            textOrientationSheetOkButton.setOnClickListener {
                dismiss()
            }
            textOrientationSheetCancelButton.setOnClickListener {
                dismiss()
                confirmListener.onCancel()
            }
        }
        return binding.root
    }

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
}
