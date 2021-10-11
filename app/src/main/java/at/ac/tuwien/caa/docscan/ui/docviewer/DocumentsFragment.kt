package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.databinding.FragmentDocumentsBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DocumentsFragment : Fragment() {

    companion object {
        fun newInstance() = DocumentsFragment()
        val TAG = "DocumentsFragment"
    }

    private var scroll = true
    private lateinit var adapter: DocumentAdapter
    private lateinit var binding: FragmentDocumentsBinding

    private val viewModel: DocumentsViewModel by viewModel()
    private val sharedViewModel: DocumentViewerViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDocumentsBinding.inflate(inflater, container, false)
        adapter = DocumentAdapter({
            sharedViewModel.selectDocument(it.document)
        }, {
            sharedViewModel.selectDocumentOptions(it)
        })
        binding.documentsList.adapter = adapter
        binding.documentsList.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }

    private fun observe() {
        viewModel.observableDocuments.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            if (it.isEmpty()) {
                binding.documentsEmptyLayout.visibility = View.VISIBLE
                binding.documentsList.visibility = View.INVISIBLE
            } else {
                binding.documentsList.visibility = View.VISIBLE
                binding.documentsEmptyLayout.visibility = View.INVISIBLE
            }

            if (scroll) {
                binding.documentsList.smoothScrollToPosition(it.indexOfFirst { doc -> doc.document.isActive })
                scroll = false
            }
        })
    }


    /**
     * Checks for a given file if the processing status of the parenting document has changed and
     * changes the list adapter in case of changes.
     */
// TODO: Add processing, upload etc. stuff for the document, this doesn't need to be performed manually, but rather through the DB.
//    fun checkDocumentProcessStatus(file: File) {
//
////        Get the document or return if it is not found:
//        val document = Helper.getParentDocument(context, file) ?: return
//
//        var docFromAdapter: Document? = null
//
////        Find the corresponding document in the adapter:
//        var idx = 0
//        val docIt = documents.iterator()
//        while (docIt.hasNext()) {
//            val doc = docIt.next()
//            if (doc.title.equals(document.title, true)) {
//                Log.d(TAG, "found the document")
//                docFromAdapter = doc
//                break
//            }
//            idx++
//        }
//
////        Has the status changed?
//        if (docFromAdapter != null) {
////            Tell the adaptor about the change at position idx:
//            val isProcessed = Helper.isCurrentlyProcessed(document)
//            if (docFromAdapter.isCurrentlyProcessed != isProcessed) {
//                docFromAdapter.setIsCurrentlyProcessed(isProcessed)
//                documents_list.adapter!!.notifyItemChanged(idx)
//            }
//        }
//
//    }
}
