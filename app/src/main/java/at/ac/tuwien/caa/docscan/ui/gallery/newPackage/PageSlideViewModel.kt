package at.ac.tuwien.caa.docscan.ui.gallery.newPackage

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.PageSlideActivity.Companion.EXTRA_DOCUMENT_ID
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.PageSlideActivity.Companion.EXTRA_SELECTED_PAGE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class PageSlideViewModel(extras: Bundle, val repository: DocumentRepository) : ViewModel() {

    private val docId = extras.getSerializable(EXTRA_DOCUMENT_ID) as UUID
    private val selectedPageId = extras.getSerializable(EXTRA_SELECTED_PAGE_ID) as UUID?
    val observablePages = MutableLiveData<Pair<List<Page>, Int>>()
    val observableInitCrop = MutableLiveData<Event<Page>>()
    val observableInitRetake = MutableLiveData<Event<Pair<UUID, UUID>>>()

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
        observableInitCrop.postValue(Event(page))
    }

    fun retakeImageAtPosition(index: Int) {
        val page = getPageByIndex(index) ?: return
        observableInitRetake.postValue(Event(Pair(docId, page.id)))
    }

    private fun getPageByIndex(index: Int) = observablePages.value?.first?.getOrNull(index)
}
