package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_documents.*
import java.io.File

class DocumentsFragment(private val listener: DocumentListener) : Fragment() {

    companion object {
        val TAG = "DocumentsFragment"
    }

    private var scroll = false
//    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var documents: ArrayList<Document>
    private lateinit var adapter: DocumentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_documents, container, false)
    }

    fun scrollToActiveDocument() {
        scroll = true
    }

    fun comparator(d: Document): String = d.title.toLowerCase()

    fun deleteDocument(document: Document) {

        Log.d(TAG, "document: ${document.title}")

        val docIt = documents.iterator()
        var idx = 0
        while (docIt.hasNext()) {
            val doc = docIt.next()
            Log.d(TAG, "docIt: " + doc.title)
            if (doc.title.equals(document.title, true)) {
                Log.d(TAG, "removing idx: " + idx)
                documents_list.adapter!!.notifyItemRemoved(idx)
                return
            }
            idx++
        }


    }

    override fun onResume() {
        super.onResume()

        resetAdapter()
        documents_list.layoutManager = LinearLayoutManager(context)

        if (scroll) {
            val pos = documents.indexOf(
                    DocumentStorage.getInstance(context).activeDocument)
            if (pos != -1)
                documents_list.smoothScrollToPosition(pos)
            scroll = false
        }

    }

    fun updateDocument(title: String) {

        val docIt = documents.iterator()
        var idx = 0
        while (docIt.hasNext()) {
            val doc = docIt.next()
            if (doc.title == title) {
                documents_list.adapter!!.notifyItemChanged(idx)
                return
            }

            idx++
        }

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

    fun resetAdapter() {

        DocumentStorage.getInstance(context).updateStatus(context)

        documents = DocumentStorage.getInstance(context).documents
        //        Sort the documents alphabetically:
        documents.sortBy { comparator(it) }
        adapter = DocumentAdapter(documents,
                {
    //                    Inform the DocumentViewerActivity that a document should be opened:
                    document: Document ->
                    listener?.onDocumenOpened(document)
                },
                {
    //                    Inform the DocumentViewerActivity that document options should be shown:
                    document: Document ->
                    listener?.onDocumentOptions(document)
                },
                DocumentStorage.getInstance(context).activeDocument)

        documents_list.adapter = adapter
    }

    interface DocumentListener {
        fun onDocumenOpened(document: Document)
        fun onDocumentOptions(document: Document)
    }

}

