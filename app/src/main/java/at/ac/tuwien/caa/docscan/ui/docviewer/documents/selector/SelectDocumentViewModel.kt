package at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.ExportFormat
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectDocumentViewModel(val documentRepository: DocumentRepository) : ViewModel() {

    val observableDocuments: MutableLiveData<List<DocumentWithPages>> = MutableLiveData()
    val observableSelectionResource: MutableLiveData<Resource<Unit>> = MutableLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.getAllDocumentsAsFlow().collectLatest {
                observableDocuments.postValue(it.sortedBy { documentWithPages -> documentWithPages.document.title.lowercase() })
            }
        }
    }

    fun export(documentWithPages: DocumentWithPages, forceAction: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            observableSelectionResource.postValue(documentRepository.exportDocument(documentWithPages, forceExport = forceAction, ExportFormat.PDF_WITH_OCR))
        }
    }
}