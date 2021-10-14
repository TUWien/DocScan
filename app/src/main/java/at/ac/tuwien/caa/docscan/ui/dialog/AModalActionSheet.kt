package at.ac.tuwien.caa.docscan.ui.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.camera.SheetAction
import at.ac.tuwien.caa.docscan.camera.SheetAdapter
import at.ac.tuwien.caa.docscan.databinding.TitledSheetDialogCameraBinding
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.extractDocWithPages
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.sheet_dialog_camera.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber

/**
 * An utility class for handling simple modal action sheet chooser. This is basically an extension
 * of the older ActionSheet, which also provides a correct handling for rotation and saving states.
 *
 * @author Matej Bartalsky
 */
open class AModalActionSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AModalActionSheet"

        const val ARG_SHEET_MODEL = "ARG_SHEET_MODEL"

        fun newInstance(model: SheetModel): AModalActionSheet {
            return AModalActionSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SHEET_MODEL, model)
                }
            }
        }
    }

    protected lateinit var binding: TitledSheetDialogCameraBinding
    lateinit var model: SheetModel

    private val viewModel: ModalActionSheetViewModel by lazy {
        // if parent fragment is null, i.e. viewModel was attached to the activity's scope.
        if (parentFragment == null) {
            requireActivity().getViewModel()
        } else {
            // otherwise, it was attached in a fragment scope.
            requireParentFragment().getViewModel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TitledSheetDialogCameraBinding.inflate(layoutInflater)
        model = arguments?.getParcelable(ARG_SHEET_MODEL)
            ?: throw IllegalArgumentException("No arguments passed!")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sheetAdapter =
            SheetAdapter(model.sheetActions) { sheetAction: SheetAction ->
                viewModel.select(
                    sheetAction,
                    model.arguments
                )
                dismissAllowingStateLoss()
            }
        binding.sheetDialogRecyclerview.apply {
            adapter = sheetAdapter
            layoutManager = GridLayoutManager(this@AModalActionSheet.context, 2)
        }
    }

    fun show(fragmentManager: FragmentManager) {
        val fragment = fragmentManager.findFragmentByTag(TAG)
        if (fragment == null ||
            fragment is ADialog &&
            fragment.dialog?.isShowing != true
        ) {
            fragmentManager.beginTransaction().add(this, TAG).commitAllowingStateLoss()
        } else {
            Timber.d("AModalActionSheet already shown!")
        }
    }
}

class DocumentSheet : AModalActionSheet() {
    companion object {
        fun newInstance(model: SheetModel): DocumentSheet {
            return DocumentSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SHEET_MODEL, model)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.arguments.extractDocWithPages()?.let {
            binding.sheetDialogTitle.text = it.document.title
        }
    }
}

class ModalActionSheetViewModel : ViewModel() {
    val observableSheetAction = MutableLiveData<Event<ModalSheetResult>>()

    internal fun select(
        pressedSheetAction: SheetAction,
        arguments: Bundle
    ) {
        observableSheetAction.value = Event(ModalSheetResult(pressedSheetAction, arguments))
    }
}

data class ModalSheetResult(
    val pressedSheetAction: SheetAction,
    val arguments: Bundle
)

@Parcelize
data class SheetModel(
    val sheetActions: ArrayList<SheetAction>,
    val arguments: Bundle = Bundle()
) : Parcelable

fun SheetModel.show(
    fragmentManager: FragmentManager
) = DocumentSheet.newInstance(this).show(fragmentManager)
