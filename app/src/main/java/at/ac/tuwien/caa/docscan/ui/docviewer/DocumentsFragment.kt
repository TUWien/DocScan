package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ActionSheet
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.ui.syncui.DocumentAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_documents.*

class DocumentsFragment(private val listener: DocumentListener) : Fragment() {

//    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_documents, container, false)
    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//
//        super.onViewCreated(view, savedInstanceState)
//
//
//
//    }

    override fun onResume() {
        super.onResume()

        val documents = DocumentStorage.getInstance(context).documents
        val adapter = DocAdapter(documents,
                {
//                    Inform the ViewerActivity that a document should be opened:
                    document: Document -> listener?.onDocumenOpened(document)
                },
                {
//                    Inform the ViewerActivity that document options should be shown:
                    document: Document -> listener?.onDocumentOptions(document)
                },
                DocumentStorage.getInstance(context).activeDocument)
//        val adapter = DocAdapter(documents) {
////            Inform the ViewerActivity that a document should be opened:
//            document: Document -> listener?.onDocumenOpened(document)
//        }
        documents_list.adapter = adapter
        documents_list.layoutManager = LinearLayoutManager(context)
    }

    interface DocumentListener {
        fun onDocumenOpened(document: Document)
        fun onDocumentOptions(document: Document)
    }

}

