package at.ac.tuwien.caa.docscan.ui.docviewer

import android.net.Uri
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
import java.util.*

/**
 * TODO: ERROR_HANDLING/DB_CONSTRAINTS: Add missing error handling and confirmation of actions.
 */
class DocumentViewerViewModel(private val repository: DocumentRepository) : ViewModel() {

    val selectedScreen = MutableLiveData(DocumentViewerScreen.DOCUMENTS)
    val observableInitDocumentOptions = MutableLiveData<Event<DocumentWithPages>>()
    val observableInitCamera = MutableLiveData<Event<Unit>>()

    val observableNumOfSelectedElements = MutableLiveData<Int>()
    private val observableDocumentAtImages = MutableLiveData<DocumentWithPages?>()


    val observableResourceAction = MutableLiveData<Event<Resource<DocumentAction>>>()
    val observableResourceConfirmation =
        MutableLiveData<Event<Pair<DocumentAction, DocumentWithPages>>>()

    /**
     * This function needs to be called every time the selected fragment in the bottom nav changes.
     */
    fun changeScreen(screen: DocumentViewerScreen) {
        // every time the screen is changed, the selected elements will get deleted.
        observableNumOfSelectedElements.postValue(0)
        selectedScreen.postValue(screen)
    }

    fun informAboutImageViewer(documentWithPages: DocumentWithPages?) {
        observableDocumentAtImages.postValue(documentWithPages)
    }

    fun initDocumentOptions(documentWithPages: DocumentWithPages) {
        observableInitDocumentOptions.postValue(Event(documentWithPages))
    }

    fun setSelectedElements(selectedElements: Int) {
        observableNumOfSelectedElements.postValue(selectedElements)
    }

    fun addNewImages(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = run {
                if (selectedScreen.value == DocumentViewerScreen.IMAGES) {
                    observableDocumentAtImages.value?.document
                } else {
                    null
                }
            } ?: return@launch
            repository.saveNewImportedImageForDocument(doc, uris)
        }
    }

    /**
     * Starts imaging with a document, there are several scenarios.
     * - If [docId] is available, then this will be the next active document.
     * - If [docId] is null but [selectedScreen] is [DocumentViewerScreen.IMAGES], then
     * the currently selected document will become active.
     * - If none of the previous cases occur, imaging will be continued with the active document.
     */
    fun startImagingWith(docId: UUID? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val docIdToBecomeActive = docId ?: run {
                if (selectedScreen.value == DocumentViewerScreen.IMAGES) {
                    observableDocumentAtImages.value?.document?.id
                } else {
                    null
                }
            }
            docIdToBecomeActive?.let {
                repository.setDocumentAsActive(it)
            }
            observableInitCamera.postValue(Event(Unit))
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
                    Failure(resource.exception)
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
