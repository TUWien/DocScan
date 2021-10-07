package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.launch

class DocumentViewerViewModel(val repository: DocumentRepository) : ViewModel() {
    val observableSelectedDocument = MutableLiveData<DocumentWithPages?>()

    init {
        viewModelScope.launch {
            observableSelectedDocument.postValue(repository.getActiveDocument())
        }
    }

    fun selectDocument(documentWithPages: DocumentWithPages) {
        observableSelectedDocument.postValue(documentWithPages)
    }

    fun selectDocumentOptions(documentWithPages: DocumentWithPages) {
        // TODO: Initiate action sheet for documents
    }
}
