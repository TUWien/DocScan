package at.ac.tuwien.caa.docscan.camera

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Document
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.sheet_dialog_camera.*
import java.io.File

open class PdfActionSheet(private val pdf: File, sheetActions: ArrayList<SheetAction>,
                          private var pdfListener: PdfSheetSelection, dialogListener: DialogStatus) :
        ActionSheet(sheetActions, null, dialogListener) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.titled_sheet_dialog_pdf, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleField: TextView = view.findViewById(R.id.sheet_dialog_title)
        titleField.text = pdf.name
    }

    override fun sheetClicked(sheetAction: SheetAction) {

//        Close the BottomSheetDialogFragment:
        pdfListener?.onPdfSheetSelected(pdf, sheetAction)
        dismiss()
    }

}

open class DocumentActionSheet(private var document: Document, sheetActions: ArrayList<SheetAction>,
                               private var docListener: DocumentSheetSelection, dialogListener: DialogStatus) :
        ActionSheet(sheetActions, null, dialogListener) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.titled_sheet_dialog_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleField: TextView = view.findViewById(R.id.sheet_dialog_title)
        titleField.text = document.title
    }

    override fun sheetClicked(sheetAction: SheetAction) {

//        Close the BottomSheetDialogFragment:
        docListener?.onDocumentSheetSelected(document, sheetAction)
        dismiss()
    }

}

open class ActionSheet: BottomSheetDialogFragment {

    private val sheetActions: ArrayList<SheetAction>
    protected var listener: SheetSelection? = null
    protected var dialogListener: DialogStatus? = null
    private val CLASS_NAME = "ActionSheet"

    constructor(sheetActions: ArrayList<SheetAction>, listener: SheetSelection?, dialogListener: DialogStatus) {

        this.sheetActions = sheetActions
        this.listener = listener
        this.dialogListener = dialogListener
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

    open fun sheetClicked(sheetAction: SheetAction) {

//        Close the BottomSheetDialogFragment:
        listener?.onSheetSelected(sheetAction)
        dismiss()
    }

//    override fun onDismiss(dialog: DialogInterface?) {
//        super.onDismiss(dialog)
//    }

    override fun onResume() {
        super.onResume()
        dialogListener?.onShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogListener?.onDismiss()
    }

    class SheetAction(id: Int, text: String, icon: Int) {

        val mId = id
        val mText = text
        val mIcon = icon

        fun getID(): Int {
            return mId
        }
    }


    interface PdfSheetSelection {
        fun onPdfSheetSelected(pdf: File, sheetAction: SheetAction)
    }

    interface DocumentSheetSelection {
        fun onDocumentSheetSelected(document: Document, sheetAction: SheetAction)
    }

    interface SheetSelection {
        fun onSheetSelected(sheetAction: SheetAction)
    }

    interface DialogStatus {
        fun onShown()
        fun onDismiss()
    }



}