package at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectDocumentViewModel(val documentRepository: DocumentRepository) : ViewModel() {

    val observableDocuments: MutableLiveData<List<DocumentWithPages>> = MutableLiveData()
    val observableSelectionResource: MutableLiveData<SelectDocumentModel> = MutableLiveData()
    val observableConfirmExport: MutableLiveData<Event<Bundle>> = MutableLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.getAllDocumentsAsFlow().collectLatest {
                observableDocuments.postValue(it.sortedBy { documentWithPages -> documentWithPages.document.title.lowercase() })
            }
        }
    }

    fun export(arguments: Bundle) {
        val docWithPages = arguments.extractDocWithPages() ?: kotlin.run {
            observableSelectionResource.postValue(
                SelectDocumentModel(
                    DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure(),
                    arguments
                )
            )
            return
        }
        export(docWithPages, arguments)
    }

    fun export(documentWithPages: DocumentWithPages, arguments: Bundle) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!arguments.extractIsConfirmed()) {
                observableConfirmExport.postValue(
                    Event(
                        arguments.appendDocWithPages(
                            documentWithPages
                        )
                    )
                )
                return@launch
            }
            observableSelectionResource.postValue(
                SelectDocumentModel(
                    documentRepository.exportDocument(
                        documentWithPages,
                        skipCropRestriction = arguments.extractSkipCropRestriction(),
                        if (arguments.extractUseOCR()) ExportFormat.PDF_WITH_OCR else ExportFormat.PDF
                    ), arguments
                )
            )
        }
    }
}

data class SelectDocumentModel(val resource: Resource<Unit>, val arguments: Bundle)
