package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocumentViewerViewModel(val repository: DocumentRepository) : ViewModel() {
    val observableSelectedDocument = MutableLiveData<Document?>()
    val observableNumOfSelectedElements = MutableLiveData(0)
    val observableInitDocumentOptions = MutableLiveData<Event<DocumentWithPages>>()
    val observableInitCamera = MutableLiveData<Event<Unit>>()

    val observableResourceAction = MutableLiveData<Event<Resource<DocumentWithPages>>>()

    fun selectDocument(document: Document) {
        observableSelectedDocument.postValue(document)
    }

    fun selectDocumentOptions(documentWithPages: DocumentWithPages) {
        observableInitDocumentOptions.postValue(Event(documentWithPages))
    }

    fun setSelectedElements(selectedElements: Int) {
        observableNumOfSelectedElements.postValue(selectedElements)
    }

    fun startImagingWith(documentWithPages: DocumentWithPages) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDocumentAsActive(documentWithPages.document.id)
            observableInitCamera.postValue(Event(Unit))
        }
    }

    fun startDocumentDeletionFor(documentWithPages: DocumentWithPages) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Check constraints and inform observableResourceAction
            repository.removeDocument(documentWithPages)
        }
    }
}
