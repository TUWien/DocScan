package at.ac.tuwien.caa.docscan.ui.document

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.db.model.edit
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity.Companion.EXTRA_DOCUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditDocumentViewModel(extras: Bundle, val documentRepository: DocumentRepository) :
    ViewModel() {

    val observableDocument = MutableLiveData<Document>(extras.getParcelable(EXTRA_DOCUMENT))
    val observableRequestResource = MutableLiveData<Event<Resource<Document>>>()

    fun saveDocument(title: String, metaData: MetaData) {
        val document = observableDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            observableRequestResource.postValue(
                Event(
                    documentRepository.createOrUpdateDocument(
                        document.edit(title, metaData)
                    )
                )
            )
        }
    }
}
