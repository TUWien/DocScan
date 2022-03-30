package at.ac.tuwien.caa.docscan.ui.docviewer.images

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class ImagesViewModel(val repository: DocumentRepository) : ViewModel() {

    val observablePages = MutableLiveData<ImageModel>()
    val observableDocWithPages = MutableLiveData<DocumentWithPages?>()
    val observableInitGallery = MutableLiveData<Event<Page>>()
    val observableError = MutableLiveData<Event<Throwable>>()
    val observableConfirmDelete = MutableLiveData<Event<Int>>()

    private var collectorJob: Job? = null
    private var isRotating: Boolean = false
    private var shouldScroll = true

    /**
     * Loads document pages by id with three different scenarios:
     * - if the param [documentId] is not null, a stream of that document is going to be collected.
     * - if the param is null, the active document is being collected.
     */
    fun loadDocumentPagesById(documentId: UUID?, pageId: UUID?) {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch(Dispatchers.IO) {
            if (documentId != null) {
                repository.getDocumentWithPagesAsFlow(documentId = documentId)
                    .collectLatest {
                        processDocumentPage(
                            it, if (shouldScroll) {
                                shouldScroll = false
                                pageId
                            } else {
                                null
                            }
                        )
                    }
            } else {
                repository.getActiveDocumentAsFlow().collectLatest {
                    processDocumentPage(it, null)
                }
            }
        }
    }

    private fun processDocumentPage(documentWithPages: DocumentWithPages?, scrollToPageId: UUID?) {
        val pages = documentWithPages?.pages
        observableDocWithPages.postValue(documentWithPages)

        val currentModel = observablePages.value
        val isSelectionActivated =
            currentModel?.pages?.firstOrNull { currentPage -> currentPage.isSelected } != null
        val selectionList = pages?.map { page ->
            PageSelection(
                page,
                isSelectionActivated,
                currentModel?.pages?.find { currentPage -> currentPage.page.id == page.id }?.isSelected
                    ?: false
            )
        } ?: listOf()
        observablePages.postValue(
            ImageModel(
                documentWithPages?.document,
                selectionList,
                selectionList.indexOfFirst { page -> page.page.id == scrollToPageId }
            )
        )
    }

    fun rotateAllSelectedPages() {
        if (isRotating) {
            return
        } else {
            isRotating = true
        }
        Timber.d("rotateAllSelectedPages")
        viewModelScope.launch(Dispatchers.IO) {
            val pages = getPagesCopy() ?: return@launch
            when (val resource =
                repository.rotatePagesBy90(pages.pages.filter { selection -> selection.isSelected }
                    .map { selection -> selection.page })) {
                is Failure -> {
                    observableError.postValue(Event(resource.exception))
                }
                is Success -> {
                    // ignore success
                }
            }
            isRotating = false
        }
    }

    fun deleteAllSelectedPages(force: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val pages = getPagesCopy() ?: return@launch
            val selectedPages =
                pages.pages.filter { page -> page.isSelected }.map { page -> page.page }
            if (!force) {
                observableConfirmDelete.postValue(Event(selectedPages.count()))
                return@launch
            }
            when (val resource = repository.deletePages(selectedPages)) {
                is Failure -> {
                    observableError.postValue(Event(resource.exception))
                }
                is Success -> {
                    // ignore success
                }
            }
        }
    }

    fun setSelectedForAll(isSelected: Boolean) {
        val pages = getPagesCopy() ?: return
        pages.pages.forEach { page ->
            page.isSelected = isSelected
        }
        pages.pages.checkSelectionState()
        observablePages.postValue(pages)
    }

    fun clickOnItem(page: PageSelection) {
        val pages = getPagesCopy() ?: return
        val isSelectionActivated =
            pages.pages.isSelectionActivated().first
        if (!isSelectionActivated) {
            observableInitGallery.postValue(Event(page.page))
        } else {
            longClickOnItem(page)
        }
    }

    fun longClickOnItem(page: PageSelection) {
        val model = getPagesCopy() ?: return
        val pageFound =
            model.pages.find { currentPage -> currentPage.page.id == page.page.id } ?: return
        // invert the selection
        pageFound.isSelected = !pageFound.isSelected
        model.pages.checkSelectionState()
        observablePages.postValue(model)
    }

    /**
     * A pages copy is necessary in order to correctly pass the difference information about
     * selection states to the adapter.
     * @return a deep copy of [observablePages]
     */
    private fun getPagesCopy(): ImageModel? {
        val model = observablePages.value ?: return null
        val pagesCopy = mutableListOf<PageSelection>()
        model.pages.forEach { pageSelection ->
            pagesCopy.add(
                pageSelection.copy(
                    page = pageSelection.page.copy(),
                    isSelectionActivated = pageSelection.isSelectionActivated,
                    isSelected = pageSelection.isSelected
                )
            )
        }
        return ImageModel(model.document?.copy(), pagesCopy, model.scrollTo)
    }
}


fun List<PageSelection>.checkSelectionState() {
    val count = isSelectionActivated().second
    forEach { page ->
        page.isSelectionActivated = count > 0
    }
}

fun List<PageSelection>.isSelectionActivated(): Pair<Boolean, Int> {
    val isActivated = find { pageSelection -> pageSelection.isSelectionActivated } != null
    val selectionCount = count { pageSelection -> pageSelection.isSelected }
    return Pair(isActivated, selectionCount)
}
