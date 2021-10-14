package at.ac.tuwien.caa.docscan.ui.gallery.newPackage

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.ImageViewerFragment.Companion.ARG_PAGE_ID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ImageViewModel(extras: Bundle, val documentRepository: DocumentRepository) : ViewModel() {

    private val pageId = extras.getSerializable(ARG_PAGE_ID) as UUID
    val observablePage = MutableLiveData<Page>()

    init {
        viewModelScope.launch {
            documentRepository.getPageByIdAsFlow(pageId).collectLatest {
                // ignore null values, since those need to be resolved on a higher level anyway.
                it?.let {
                    observablePage.postValue(it)
                }
            }
        }
    }
}
