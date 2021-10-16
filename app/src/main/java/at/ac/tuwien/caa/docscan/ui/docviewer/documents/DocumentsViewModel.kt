package at.ac.tuwien.caa.docscan.ui.docviewer.documents

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DocumentsViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    val observableDocuments = MutableLiveData<List<DocumentWithPages>>()

    init {
        viewModelScope.launch {
            repository.getAllDocuments().collectLatest {
                // sort alphabetically
                observableDocuments.postValue(it.sortedBy { doc -> doc.document.title })
            }
        }
    }

    fun deleteDocument(documentWithPages: DocumentWithPages) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDocument(documentWithPages)
        }
    }
}
