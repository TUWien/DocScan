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
                val position = (it.indexOfFirst { doc -> doc.document.isActive })
                if (position != -1) {
                    binding.documentsList.smoothScrollToPosition(position)
                }
                scroll = false
            }
        })
    }
}
