package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocumentViewerViewModel(val repository: DocumentRepository) : ViewModel() {

    val observableInitDocumentOptions = MutableLiveData<Event<DocumentWithPages>>()
    val selectedScreen = MutableLiveData(DocumentViewerScreen.DOCUMENTS)
    val observableNumOfSelectedElements = MutableLiveData<Int>()

    val observableInitCamera = MutableLiveData<Event<Unit>>()

    val observableResourceAction = MutableLiveData<Event<Resource<DocumentAction>>>()
    val observableResourceConfirmation =
        MutableLiveData<Event<Pair<DocumentAction, DocumentWithPages>>>()

    fun changeScreen(screen: DocumentViewerScreen) {
        // every time the screen is changed, the selected elements will get deleted.
        observableNumOfSelectedElements.postValue(0)
        selectedScreen.postValue(screen)
    }

    fun initDocumentOptions(documentWithPages: DocumentWithPages) {
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

    fun deleteEntireDocument(documentWithPages: DocumentWithPages) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Check constraints and inform observableResourceAction
            repository.removeDocument(documentWithPages)
        }
    }

    fun applyActionFor(
        force: Boolean = false,
        action: DocumentAction,
        documentWithPages: DocumentWithPages
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            // ask for confirmation first
            if (!force && action.needsConfirmation) {
                observableResourceConfirmation.postValue(Event(Pair(action, documentWithPages)))
                return@launch
            }

            val resource = when (action) {
                DocumentAction.DELETE -> {
                    repository.removeDocument(documentWithPages)
                }
                DocumentAction.EXPORT -> {
                    repository.exportDocument(documentWithPages)
                }
                DocumentAction.CROP -> {
                    repository.processDocument(documentWithPages)
                }
                DocumentAction.UPLOAD -> {
                    repository.uploadDocument(documentWithPages)
                }
            }
            val result = when (resource) {
                is Failure -> {
                    Failure<DocumentAction>(resource.exception)
                }
                is Success -> {
                    Success(action)
                }
            }
            observableResourceAction.postValue(Event(result))
        }
    }
}

enum class DocumentAction(val needsConfirmation: Boolean) {
    DELETE(true),
    EXPORT(true),
    CROP(true),
    UPLOAD(true)
}

enum class DocumentViewerScreen {
    DOCUMENTS,
    IMAGES,
    PDFS
}