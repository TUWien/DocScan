package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.docviewer.ImagesFragment.Companion.ARG_DOCUMENT_ID
import at.ac.tuwien.caa.docscan.ui.docviewer.ImagesFragment.Companion.ARG_FILE_ID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ImagesViewModel(extras: Bundle, val repository: DocumentRepository) : ViewModel() {
    
    val documentId: UUID = extras.getSerializable(ARG_DOCUMENT_ID) as UUID
    val fileId = extras.getSerializable(ARG_FILE_ID) as? UUID

    val observablePages = MutableLiveData<List<PageSelection>>()
    val observableDoc = MutableLiveData<Document>()

    init {
        viewModelScope.launch {
            repository.getDocumentWithPagesAsFlow(documentId).collectLatest {
                val pages = it?.pages ?: return@collectLatest // TODO: Add empty handling
                observableDoc.postValue(it.document)
                val currentPages = observablePages.value
                observablePages.postValue(pages.map { page ->
                    PageSelection(
                        page,
                        currentPages?.find { currentPage -> currentPage.page.id == page.id } != null)
                })
            }
        }
    }

    fun rotateAllSelectedPages() {

    }

    fun deleteAllSelectedPages() {

    }

    fun setSelectedForAll(isSelected: Boolean) {
        val pages = observablePages.value ?: return
        pages.forEach { page ->
            page.isSelected = isSelected
        }
        observablePages.postValue(pages)
    }

    fun setSelected(page: PageSelection, isSelected: Boolean) {
        val pages = observablePages.value ?: return
        val foundPage = pages.find { currentPage -> currentPage.page.id == page.page.id }
        foundPage?.isSelected = isSelected
        observablePages.postValue(pages)
    }
}
