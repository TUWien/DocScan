package at.ac.tuwien.caa.docscan.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.sheet_dialog_camera.*

class CameraSheetDialog: BottomSheetDialogFragment {

    private val mSheetActions: ArrayList<SheetAction>

    constructor(sheetActions: ArrayList<SheetAction>) {

        mSheetActions = sheetActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


//        val newDocumentAction = SheetAction(
//                42.toInt(), "New document", R.drawable.ic_folder_open_gray_24dp)
//
//        val sheetAction = arrayListOf<SheetAction>()
//        sheetAction.addAll(listOf(newDocumentAction))

//        val sheetAdapter = SheetAdapter(mSheetActions)
//        sheet_dialog_recyclerview.adapter = sheetAdapter
//        sheet_dialog_recyclerview.layoutManager = LinearLayoutManager(this@CameraSheetDialog.context)

        return inflater.inflate(R.layout.sheet_dialog_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sheetAdapter = SheetAdapter(mSheetActions)
        sheet_dialog_recyclerview.apply {
            adapter = sheetAdapter
            layoutManager = LinearLayoutManager(this@CameraSheetDialog.context)
        }
    }

    class SheetAction(id: Int, text: String, icon: Int) {

        val mId = id
        val mText = text
        val mIcon = icon
    }
}