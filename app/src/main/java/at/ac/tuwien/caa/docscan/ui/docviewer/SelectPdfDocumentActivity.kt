package at.ac.tuwien.caa.docscan.ui.docviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityPdfSelectorBinding
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.ui.BaseActivity

// TODO: Adapt for the new domain structure
class SelectPdfDocumentActivity : BaseActivity(),
    DocumentSelector {

    private lateinit var documents: ArrayList<Document>
    private lateinit var listener: DocumentSelector
    private lateinit var binding: ActivityPdfSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listener = this
        initToolbar()
        loadDocuments()
        updateRecyclerView()
    }

    private fun initToolbar() {

        var toolbar: Toolbar = findViewById(R.id.main_toolbar)
        toolbar.title = getString(R.string.select_document_title)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun comparator(d: Document): String = d.title.toLowerCase()

    private fun loadDocuments() {

//        Load the documents:
        DocumentStorage.getInstance(this).updateStatus(this)
        documents = DocumentStorage.getInstance(this).documents
//        Sort the documents alphabetically:
        documents.sortBy { comparator(it) }

    }

    private fun updateRecyclerView() {

        val adapter = SelectDocumentAdapter(
            documents
        ) { document: Document ->
            listener?.onDocumentSelected(document)
        }

        binding.documentsList.adapter = adapter
        binding.documentsList.layoutManager = LinearLayoutManager(this)
    }


    override fun onDocumentSelected(document: Document) {

        //        Send back the new file name, so that it can be used in the DocumentViewerActivity:
        val data = Intent()
        data.data = Uri.parse(document.title)
        setResult(Activity.RESULT_OK, data)

        finish()
    }


}

interface DocumentSelector {
    fun onDocumentSelected(document: Document)
}