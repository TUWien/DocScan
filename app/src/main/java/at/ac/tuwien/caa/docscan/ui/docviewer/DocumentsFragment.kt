package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_documents.*
import java.io.File

class DocumentsFragment : Fragment() {

    companion object {

        fun newInstance(): DocumentsFragment {
            return DocumentsFragment()
        }

        val TAG = "DocumentsFragment"
    }

    private var scroll = false
    private lateinit var documents: ArrayList<Document>
    private lateinit var adapter: DocumentAdapter
    private lateinit var listener: DocumentListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(at.ac.tuwien.caa.docscan.R.layout.fragment_documents, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = context as DocumentListener

    }

    fun scrollToActiveDocument() {
        scroll = true
    }

    private fun comparator(d: Document): String = d.title.toLowerCase()

    fun deleteDocument(document: Document) {

        val docIt = documents.iterator()
        var idx = 0
        while (docIt.hasNext()) {
            val doc = docIt.next()
            if (doc.title.equals(document.title, true)) {
                documents_list.adapter!!.notifyItemRemoved(idx)
                checkEmptyDocuments()
                return
            }
            idx++
        }

    }

    fun getImageView(document: Document): ImageView {
        val idx = documents.indexOf(document)
        val selectedViewHolder = documents_list
                .findViewHolderForAdapterPosition(idx)
        return selectedViewHolder!!.itemView.findViewById(at.ac.tuwien.caa.docscan.R.id.document_thumbnail_imageview)

    }

    override fun onResume() {

        super.onResume()

        loadDocuments()

        if (!checkEmptyDocuments()) {

            updateRecyclerView()
            if (scroll) {
                val pos = documents.indexOf(
                        DocumentStorage.getInstance(context).activeDocument)
                if (pos != -1)
                    documents_list.smoothScrollToPosition(pos)
                scroll = false
            }

        }

    }

    private fun loadDocuments() {

//        Load the documents:
        DocumentStorage.getInstance(context).updateStatus(context)
        documents = DocumentStorage.getInstance(context).documents
//        Sort the documents alphabetically:
        documents.sortBy { comparator(it) }

    }

    fun checkEmptyDocuments(): Boolean {

        if (documents.isEmpty()) {
            documents_empty_layout.visibility = View.VISIBLE
            documents_list.visibility = View.INVISIBLE
            return true
        }

        return false

    }


    /**
     * Checks for a given file if the processing status of the parenting document has changed and
     * changes the list adapter in case of changes.
     */
    fun checkDocumentProcessStatus(file: File) {

//        Get the document or return if it is not found:
        val document = Helper.getParentDocument(context, file) ?: return

        var docFromAdapter: Document? = null

//        Find the corresponding document in the adapter:
        var idx = 0
        val docIt = documents.iterator()
        while (docIt.hasNext()) {
            val doc = docIt.next()
            if (doc.title.equals(document.title, true)) {
                Log.d(TAG, "found the document")
                docFromAdapter = doc
                break
            }
            idx++
        }

//        Has the status changed?
        if (docFromAdapter != null) {
//            Tell the adaptor about the change at position idx:
            val isProcessed = Helper.isCurrentlyProcessed(document)
            if (docFromAdapter.isCurrentlyProcessed != isProcessed) {
                docFromAdapter.setIsCurrentlyProcessed(isProcessed)
                documents_list.adapter!!.notifyItemChanged(idx)
            }
        }

    }

    fun reloadDocuments() {
        loadDocuments()
        if (!checkEmptyDocuments())
            updateRecyclerView()
    }

    private fun updateRecyclerView() {

        adapter = DocumentAdapter(documents,
                {
    //                    Inform the DocumentViewerActivity that a document should be opened:
                    document: Document ->
                    listener?.onDocumentOpened(document)
                },
                {
    //                    Inform the DocumentViewerActivity that document options should be shown:
                    document: Document ->
                    listener?.onDocumentOptions(document)
                },
                DocumentStorage.getInstance(context).activeDocument)

        documents_list.adapter = adapter

        documents_list.layoutManager = LinearLayoutManager(context)
    }

    interface DocumentListener {
        fun onDocumentOpened(document: Document)
        fun onDocumentOptions(document: Document)
    }

}

