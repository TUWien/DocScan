package at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityPdfSelectorBinding
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectPdfDocumentActivity : BaseNoNavigationActivity() {

    private val viewModel: SelectDocumentViewModel by viewModel()
    private lateinit var binding: ActivityPdfSelectorBinding
    private lateinit var adapter: SelectDocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWithTitle(R.string.select_document_title)
        adapter = SelectDocumentAdapter(clickListener = { documentWithPages ->
            viewModel.export(documentWithPages, forceAction = true)
        })
        binding.documentsList.adapter = adapter
        binding.documentsList.layoutManager = LinearLayoutManager(this)
        observe()
    }

    private fun observe() {
        viewModel.observableDocuments.observe(this, {
            adapter.submitList(it)
        })
        viewModel.observableSelectionResource.observe(this, {
            when (it) {
                is Failure -> {
                    it.exception.handleError(this)
                }
                is Success -> {
                    // TODO: Show snackbar
                    finish()
                }
            }
        })
    }
}
