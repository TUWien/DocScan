package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ImagesViewModel(val repository: DocumentRepository) :
    ViewModel() {

//    val documentId: UUID = extras.getSerializable(ARG_DOCUMENT_ID) as UUID
//    val fileId = extras.getSerializable(ARG_FILE_ID) as? UUID

    val observableProgress = MutableLiveData<Boolean>()
    val observablePages = MutableLiveData<ImageModel>()
    val observableDoc = MutableLiveData<Document>()

    val observableInitGallery = MutableLiveData<Event<Page>>()

    private var collectorJob: Job? = null

    /**
     * Loads document pages by id with three different scenarios:
     * - if the param [documentId] is not null, a stream of that document is going to be collected.
     * - if the param is null, the active document is being collected.
     */
    fun loadDocumentPagesById(documentId: UUID?) {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch(Dispatchers.IO) {
            observableProgress.postValue(true)
            if (documentId != null) {
                repository.getDocumentWithPagesAsFlow(documentId = documentId)
                    .collectLatest {
                        processDocumentPage(it)
                    }
            } else {
                repository.getActiveDocumentAsFlow().collectLatest {
                    processDocumentPage(it)
                }
            }
        }
    }

    private fun processDocumentPage(documentWithPages: DocumentWithPages?) {
        observableProgress.postValue(false)
        val pages = documentWithPages?.pages
        documentWithPages?.let {
            observableDoc.postValue(it.document)
        }

        val currentModel = observablePages.value
        val isSelectionActivated =
            currentModel?.pages?.firstOrNull { currentPage -> currentPage.isSelected } != null
        val selectionList = pages?.map { page ->
            PageSelection(
                page,
                isSelectionActivated,
                currentModel?.pages?.find { currentPage -> currentPage.page.id == page.id } != null)
        } ?: listOf()
        // TODO: Check scrolling mechanism, only scroll if necessary. selectionList.indexOfFirst { page -> page.page.id == documentPage?.pageId })
        observablePages.postValue(
            ImageModel(
                documentWithPages?.document,
                selectionList,
                -1
            )
        )
    }

    fun rotateAllSelectedPages() {

    }

    fun deleteAllSelectedPages() {

    }

    fun setSelectedForAll(isSelected: Boolean) {
        val pages = observablePages.value ?: return
        pages.pages.forEach { page ->
            page.isSelectionActivated = true
            page.isSelected = isSelected
        }
        observablePages.postValue(pages)
    }

    fun setSelected(page: PageSelection, isSelected: Boolean) {
        val pages = observablePages.value ?: return
        val foundPage = pages.pages.find { currentPage -> currentPage.page.id == page.page.id }
        foundPage?.isSelected = isSelected
        pages.pages.forEach {
            // TODO: check selection mode status
        }
        observablePages.postValue(pages)
    }

    fun clickOnItem(page: PageSelection) {
        val pages = observablePages.value ?: return
        val isSelectionActivated =
            pages.pages.firstOrNull { currentPage -> currentPage.isSelectionActivated } != null
        if (!isSelectionActivated)
            observableInitGallery.postValue(Event(page.page))
    }

    fun longClickOnItem(page: PageSelection) {
        val pages = observablePages.value ?: return
        // TODO: activate/deactivate mode
    }
}
