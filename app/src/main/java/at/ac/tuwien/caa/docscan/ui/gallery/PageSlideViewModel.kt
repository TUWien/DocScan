package at.ac.tuwien.caa.docscan.ui.gallery

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.Companion.EXTRA_DOCUMENT_ID
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.Companion.EXTRA_SELECTED_PAGE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class PageSlideViewModel(extras: Bundle, val repository: DocumentRepository, val fileHandler: FileHandler) : ViewModel() {

    private val docId = extras.getSerializable(EXTRA_DOCUMENT_ID) as UUID
    private val selectedPageId = extras.getSerializable(EXTRA_SELECTED_PAGE_ID) as UUID?
    val observablePages = MutableLiveData<Pair<List<Page>, Int>>()
    val observableInitCrop = MutableLiveData<Event<Pair<Document, Page>>>()
    val observableInitRetake = MutableLiveData<Event<Pair<UUID, UUID>>>()
    val observableSharePage = MutableLiveData<Event<Uri>>()
    val observableInitSegmentation = MutableLiveData<Event<Page>>()
    val observableInitDocumentViewer = MutableLiveData<Event<Page>>()
    // TODO: Add error handling in case the file is being processed.

    private var scrollToSpecificPage = true

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // TODO: this should be just distinct by the page ids and the number, since changes to the state of the file doesn't matter at this place.
            repository.getDocumentWithPagesAsFlow(docId).collectLatest {
                it ?: return@collectLatest
                val list = it.pages.sortedBy { page -> page.number }
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
            val page = getPageByIndex(index) ?: return@launch
            repository.deletePage(page)
        }
    }

    fun cropPageAtPosition(index: Int) {
        val page = getPageByIndex(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val doc = repository.getDocument(docId) ?: return@launch
            observableInitCrop.postValue(Event(Pair(doc, page)))
        }
    }

    fun retakeImageAtPosition(index: Int) {
        val page = getPageByIndex(index) ?: return
        observableInitRetake.postValue(Event(Pair(docId, page.id)))
    }

    fun shareImageAtPosition(index: Int) {
        val page = getPageByIndex(index) ?: return
        val uriToShare = fileHandler.getUriByPage(page) ?: return
        observableSharePage.postValue(Event(uriToShare))
    }

    fun debugSegmentation(index: Int) {
        val page = getPageByIndex(index) ?: return
        observableInitSegmentation.postValue(Event(page))
    }

    fun onSingleTap() {

    }

    fun navigateToDocumentViewer(index: Int) {
        val page = getPageByIndex(index) ?: return
        observableInitDocumentViewer.postValue(Event(page))
    }

    private fun getPageByIndex(index: Int) = observablePages.value?.first?.getOrNull(index)
}
