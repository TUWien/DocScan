package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.launch

class DocumentViewerViewModel(val repository: DocumentRepository) : ViewModel() {
    val observableSelectedDocument = MutableLiveData<Document?>()
    val observableNumOfSelectedElements = MutableLiveData(0)

    fun selectDocument(document: Document) {
        observableSelectedDocument.postValue(document)
    }

    fun selectDocumentOptions(documentWithPages: DocumentWithPages) {
        // TODO: Initiate action sheet for documents
    }

    fun setSelectedElements(selectedElements: Int) {
        observableNumOfSelectedElements.postValue(selectedElements)
    }
}
