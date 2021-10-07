package at.ac.tuwien.caa.docscan.ui.gallery

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryNewActivity.Companion.EXTRA_DOCUMENT_ID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class GalleryViewModel(val extras: Bundle, val documentRepository: DocumentRepository) :
    ViewModel() {

    private val docId: UUID = extras.getSerializable(EXTRA_DOCUMENT_ID) as UUID
    val observableDocument = MutableLiveData<DocumentWithPages>()
    val observableCloseGallery = MutableLiveData<Event<Unit>>()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            documentRepository.getDocumentWithPagesAsFlow(docId).collectLatest {
                if (it != null) {
                    observableDocument.postValue(it)
                } else {
                    // close gallery in case the document becomes null
                    observableCloseGallery.postValue(Event(Unit))
                }
            }
        }
    }
}