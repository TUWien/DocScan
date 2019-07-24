package at.ac.tuwien.caa.docscan.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.sheet_dialog_camera.*

class ActionSheet: BottomSheetDialogFragment {

    private val sheetActions: ArrayList<SheetAction>
    private var listener: SheetSelection? = null
    private val CLASS_NAME = "ActionSheet"

    constructor(sheetActions: ArrayList<SheetAction>, listener : SheetSelection) {

        this.sheetActions = sheetActions
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.sheet_dialog_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        Lambda for RecyclerView clicks. Got this from:
//        https://www.andreasjakl.com/recyclerview-kotlin-style-click-listener-android/
        val sheetAdapter = SheetAdapter(sheetActions) { sheetAction : SheetAction -> sheetClicked(sheetAction) }
        sheet_dialog_recyclerview.apply {
            adapter = sheetAdapter
            layoutManager = GridLayoutManager(this@ActionSheet.context, 2)
        }
    }

    private fun sheetClicked(sheetAction: SheetAction) {

//        Close the BottomSheetDialogFragment:
        listener?.onSheetSelected(sheetAction)
        dismiss()
    }

    class SheetAction(id: Int, text: String, icon: Int) {

        val mId = id
        val mText = text
        val mIcon = icon

        fun getID(): Int {
            return mId
        }
    }



    interface SheetSelection {
        fun onSheetSelected(sheetAction: SheetAction)
    }

}