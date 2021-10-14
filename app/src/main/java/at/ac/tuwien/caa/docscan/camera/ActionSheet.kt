package at.ac.tuwien.caa.docscan.camera

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.databinding.SheetDialogCameraBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize

@Deprecated(message = "Use AModalActionSheet.kt instead. This current implementation does not handle rotations correctly crashes due to constructor overload of fragments!")
open class ActionSheet(
    private val sheetActions: ArrayList<SheetAction>,
    protected var listener: SheetSelection? = null,
    private var dialogListener: DialogStatus? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: SheetDialogCameraBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SheetDialogCameraBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        Lambda for RecyclerView clicks. Got this from:
//        https://www.andreasjakl.com/recyclerview-kotlin-style-click-listener-android/
        val sheetAdapter =
            SheetAdapter(sheetActions) { sheetAction: SheetAction -> sheetClicked(sheetAction) }
        binding.sheetDialogRecyclerview.apply {
            adapter = sheetAdapter
            layoutManager = GridLayoutManager(this@ActionSheet.context, 2)
        }
    }

    open fun sheetClicked(sheetAction: SheetAction) {

//        Close the BottomSheetDialogFragment:
        listener?.onSheetSelected(sheetAction)
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        dialogListener?.onShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogListener?.onDismiss()
    }

    interface SheetSelection {
        fun onSheetSelected(sheetAction: SheetAction)
    }

    interface DialogStatus {
        fun onShown()
        fun onDismiss()
    }
}

@Parcelize
data class SheetAction(val id: Int, val text: String, @DrawableRes val icon: Int) : Parcelable
