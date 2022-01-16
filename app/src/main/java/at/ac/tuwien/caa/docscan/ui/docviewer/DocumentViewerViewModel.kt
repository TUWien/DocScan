package at.ac.tuwien.caa.docscan.ui.docviewer

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.isUploaded
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class DocumentViewerViewModel(private val repository: DocumentRepository) : ViewModel() {

    val selectedScreen = MutableLiveData(DocumentViewerScreen.DOCUMENTS)
    val observableInitDocumentOptions = MutableLiveData<Event<DocumentWithPages>>()
    val observableInitCamera = MutableLiveData<Event<Unit>>()

    val observableNumOfSelectedElements = MutableLiveData<Int>()
    private val observableDocumentAtImages = MutableLiveData<DocumentWithPages?>()

    val observableResourceAction = MutableLiveData<Event<DocumentViewerModel>>()
    val observableResourceConfirmation =
        MutableLiveData<Event<DocumentConfirmationModel>>()

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

    fun uploadSelectedDocument() {
        val docWithPages =
            if (selectedScreen.value == DocumentViewerScreen.IMAGES) {
                observableDocumentAtImages.value ?: kotlin.run {
                    null
                }
            } else {
                null
            }
        docWithPages ?: kotlin.run {
            observableResourceAction.postValue(
                Event(
                    DocumentViewerModel(
                        DocumentAction.UPLOAD,
                        DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure<Unit>(),
                        Bundle()
                    )
                )
            )
            return
        }
        applyActionFor(
            action = DocumentAction.UPLOAD,
            documentActionArguments = Bundle().appendDocWithPages(docWithPages),
            documentWithPages = docWithPages
        )
    }

    fun applyActionFor(
        action: DocumentAction,
        documentActionArguments: Bundle
    ) {
        val documentWithPages = documentActionArguments.extractDocWithPages() ?: kotlin.run {
            observableResourceAction.postValue(
                Event(
                    DocumentViewerModel(
                        action,
                        DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure<Unit>(),
                        documentActionArguments
                    )
                )
            )
            return
        }
        applyActionFor(action, documentActionArguments, documentWithPages)
    }

    private fun applyActionFor(
        action: DocumentAction,
        documentActionArguments: Bundle,
        documentWithPages: DocumentWithPages
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            if (!skipConfirmation(action, documentWithPages)) {
                // ask for confirmation first, if forceAction, then confirmation is not necessary anymore.
                if (action.needsConfirmation && !documentActionArguments.extractIsConfirmed()) {
                    observableResourceConfirmation.postValue(
                        Event(
                            DocumentConfirmationModel(
                                action,
                                documentWithPages,
                                documentActionArguments.appendDocWithPages(documentWithPages)
                            )
                        )
                    )
                    return@launch
                }
            }

            val resource = when (action) {
                DocumentAction.DELETE -> {
                    repository.removeDocument(documentWithPages)
                }
                DocumentAction.EXPORT -> {
                    repository.exportDocument(
                        documentWithPages,
                        skipCropRestriction = documentActionArguments.extractSkipCropRestriction(),
                        ExportFormat.PDF
                    )
                }
                DocumentAction.CROP -> {
                    repository.cropDocument(documentWithPages)
                }
                DocumentAction.UPLOAD -> {
                    repository.uploadDocument(
                        documentWithPages,
                        skipCropRestriction = documentActionArguments.extractSkipCropRestriction(),
                        skipAlreadyUploadedRestriction = documentActionArguments.extractSkipAlreadyUploadedRestriction()
                    )
                }
            }
            observableResourceAction.postValue(
                Event(
                    DocumentViewerModel(
                        action,
                        resource,
                        documentActionArguments
                    )
                )
            )
        }
    }
}

/**
 * Evaluates if the confirmation can be skipped.
 */
private fun skipConfirmation(
    action: DocumentAction,
    documentWithPages: DocumentWithPages,
): Boolean {
    return when (action) {
        DocumentAction.UPLOAD -> {
            // if the doc is already uploaded, we do not need to ask for further confirmation
            documentWithPages.isUploaded()
        }
        else -> false
    }
}

data class DocumentViewerModel(
    val action: DocumentAction,
    val resource: Resource<*>,
    val arguments: Bundle
)

data class DocumentConfirmationModel(
    val action: DocumentAction,
    val documentWithPages: DocumentWithPages,
    val arguments: Bundle
)

enum class DocumentAction(val needsConfirmation: Boolean, val showSuccessMessage: Boolean) {
    DELETE(true, false),
    EXPORT(true, true),
    CROP(true, false),
    UPLOAD(true, true)
}

enum class DocumentViewerScreen {
    DOCUMENTS,
    IMAGES,
    PDFS
}
