package at.ac.tuwien.caa.docscan.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.EditText
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class EditDocumentActivity : CreateDocumentActivity() {

    private val viewModel: EditDocumentViewModel by viewModel { parametersOf(intent!!.extras) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.initWithTitle(R.string.edit_series_title)
        observe()
    }

    private fun observe() {
        viewModel.observableDocument.observe(this) {
            fillViews(it)
        }
        viewModel.observableRequestResource.observe(this, ConsumableEvent { resource ->
            when (resource) {
                is Failure -> {
                    if (resource.exception.getDocScanDBError()?.code == DBErrorCode.DUPLICATE) {
                        showDialog(ADialog.DialogAction.CREATE_DOCUMENT_DUPLICATE)
                    } else {
                        resource.exception.handleError(this, logAsWarning = true)
                    }
                }
                is Success -> {
                    finish()
                }
            }
        })
    }

    override fun initOkButton() {
        binding.createSeriesDoneButton.apply {
            text = getString(R.string.edit_series_done_button_text)
            setOnClickListener { saveChanges() }
        }
    }

    private fun saveChanges() {
        if (!isReadme2020FieldsCompleted || !isCustomNamingValid) {
            return
        }
        val title = binding.createSeriesNameEdittext.text.toString()
        val metaData = MetaData(
            author = binding.createSeriesAuthorEdittext.text.toString(),
            authority = binding.createSeriesAuthorityEdittext.text.toString(),
            hierarchy = null,
            genre = binding.createSeriesGenreEdittext.text.toString(),
            language = binding.createSeriesReadmeLanguageDropdown.text.toString(),
            isProjectReadme2020 = binding.createSeriesReadmeCheckbox.isChecked,
            allowImagePublication = binding.createSeriesReadmePublicRadioButton.isChecked,
            signature = binding.createSeriesSignatureEdittext.text.toString(),
            url = binding.createSeriesUrlEdittext.text.toString(),
            writer = binding.createSeriesWriterEdittext.text.toString(),
            description = null
        )

        var prefix: String? = null
        if (binding.createSeriesCustomNameCheckbox.isChecked) {
            val currentPrefix = binding.createSeriesCustomNamePrefixInput.text.toString()
            if (currentPrefix.isNotEmpty()) {
                prefix = currentPrefix
            }
        }
        viewModel.saveDocument(
            title,
            prefix,
            metaData
        )
    }

    private fun fillViews(document: Document) {
        val titleEditText = fillDocumentTitle(document)
        fillTranskribusData(document, titleEditText)
        fillCustomName(document)
    }

    private fun fillCustomName(document: Document) {
        val namingCheckbox = binding.createSeriesCustomNameCheckbox
        namingCheckbox.isChecked = !document.filePrefix.isNullOrEmpty()
        if (!document.filePrefix.isNullOrEmpty()) {
            val input = binding.createSeriesCustomNamePrefixInput
            input.setText(document.filePrefix)
        }
    }

    private fun fillTranskribusData(document: Document, titleEditText: EditText) {
        val metaData = document.metaData ?: return
        binding.createSeriesAuthorEdittext.setText(metaData.author)
        binding.createSeriesWriterEdittext.setText(metaData.writer)
        binding.createSeriesGenreEdittext.setText(metaData.genre)
        binding.createSeriesSignatureEdittext.setText(metaData.signature)
        binding.createSeriesAuthorityEdittext.setText(metaData.authority)
        binding.createSeriesUrlEdittext.setText(metaData.url)

//        Check if the document is a special archive document created from QR code:
        if (metaData.relatedUploadId != null) {
            titleEditText.keyListener = null
            binding.createSeriesAuthorEdittext.keyListener = null
            binding.createSeriesAuthorEdittext.keyListener = null
            binding.createSeriesGenreEdittext.keyListener = null
            binding.createSeriesSignatureEdittext.keyListener = null
            binding.createSeriesAuthorityEdittext.keyListener = null
            binding.createSeriesUrlEdittext.keyListener = null
        }

        //    Uncomment for readme2020:
        binding.createSeriesReadmeCheckbox.isChecked = metaData.isProjectReadme2020

        val radioGroup = binding.createSeriesReadmePublicRadioGroup
        if (metaData.allowImagePublication) radioGroup.check(R.id.create_series_readme_public_radio_button) else radioGroup.check(
            R.id.create_series_readme_private_radio_button
        )
        val textView =
            findViewById<AutoCompleteTextView>(R.id.create_series_readme_language_dropdown)
        val languages = resources.getStringArray(R.array.create_document_languages)
        val selIdx = listOf(*languages).indexOf(metaData.language)
        if (selIdx != -1) textView.setText(languages[selIdx], false)
    }

    private fun fillDocumentTitle(document: Document): EditText {
        val title = document.title
        val titleEditText = binding.createSeriesNameEdittext
        titleEditText.setText(title)
        return titleEditText
    }

    companion object {
        const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"
        fun newInstance(context: Context, document: Document): Intent {
            return Intent(context, EditDocumentActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}
