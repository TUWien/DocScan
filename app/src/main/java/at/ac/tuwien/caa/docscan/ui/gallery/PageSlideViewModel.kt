package at.ac.tuwien.caa.docscan.ui.gallery

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.getFileName
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class PageSlideViewModel(
    val repository: DocumentRepository,
    val fileHandler: FileHandler
) : ViewModel() {

    val observablePages = MutableLiveData<Pair<List<Page>, Int>>()
    val observableInitCrop = MutableLiveData<Event<Page>>()
    val observableInitRetake = MutableLiveData<Event<Pair<UUID, UUID>>>()
    val observableSharePage = MutableLiveData<Event<Uri>>()
    val observableInitSegmentation = MutableLiveData<Event<Page>>()
    val observableInitDocumentViewer = MutableLiveData<Event<Page>>()
    val observableError = MutableLiveData<Event<Throwable>>()

    private var scrollToSpecificPage = true
    private var collectorJob: Job? = null

    fun load(docId: UUID, selectedPageId: UUID?) {
        // cancel previous collector jobs
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            // TODO: this should be just distinct by the page ids and the number, since changes to the state of the file doesn't matter at this place.
            repository.getDocumentWithPagesAsFlow(docId).collectLatest {
                it ?: return@collectLatest
                val list = it.pages.sortedBy { page -> page.index }
                val scrollToPosition: Int = if (scrollToSpecificPage) {
                    if (selectedPageId != null) {
                        list.indexOfFirst { page -> page.id == selectedPageId }
                    } else {
                        list.lastIndex
                    }
                } else {
                    -1
                }
                observablePages.postValue(Pair(list, scrollToPosition))
                scrollToSpecificPage = false
            }
        }
    }

    fun deletePageAtPosition(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.deletePage(getPageByIndex(index))) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    // ignore
                }
            }
        }
    }

    fun cropPageAtPosition(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.checkPageLock(getPageByIndex(index))) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    observableInitCrop.postValue(Event(result.data))
                }
            }
        }
    }

    fun retakeImageAtPosition(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.checkPageLock(getPageByIndex(index))) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    observableInitRetake.postValue(Event(Pair(result.data.docId, result.data.id)))
                }
            }
        }
    }

    fun shareImageAtPosition(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.checkPageLock(getPageByIndex(index))) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    when (val docResource = repository.getDocumentResource(result.data.docId)) {
                        is Failure -> {
                            observableError.postValue(Event(docResource.exception))
                        }
                        is Success -> {
                            when (val uriResource = fileHandler.getUriByPageResource(
                                result.data,
                                docResource.data.getFileName(
                                    result.data.index + 1,
                                    result.data.fileType
                                )
                            )) {
                                is Failure -> {
                                    observableError.postValue(Event(uriResource.exception))
                                }
                                is Success -> {
                                    observableSharePage.postValue(Event(uriResource.data))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun debugSegmentation(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.checkPageLock(getPageByIndex(index))) {
                is Failure -> {
                    observableError.postValue(Event(result.exception))
                }
                is Success -> {
                    observableInitSegmentation.postValue(Event(result.data))
                }
            }
        }
    }

    fun navigateToDocumentViewer(index: Int) {
        val page = getPageByIndex(index) ?: return
        observableInitDocumentViewer.postValue(Event(page))
    }

    private fun getPageByIndex(index: Int) = observablePages.value?.first?.getOrNull(index)
}
