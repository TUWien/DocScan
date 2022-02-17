package at.ac.tuwien.caa.docscan.ui.docviewer.documents

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.NetworkStatus
import at.ac.tuwien.caa.docscan.logic.NetworkUtil
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DocumentsViewModel(
    private val repository: DocumentRepository,
    private val networkUtil: NetworkUtil,
    private val preferencesHandler: PreferencesHandler
) : ViewModel() {

    val observableDocuments = MutableLiveData<List<DocumentWithPages>>()
    private val networkStatusState = MutableStateFlow(NetworkStatus.DISCONNECTED)

    init {
        viewModelScope.launch {
            repository.getAllDocumentsAsFlow().combine(networkStatusState) { documentWithPages, networkStatus ->

                // a copy of the first level object is necessary, since due to the combine of two flows,
                // the documentWithPages may already be a copy if just the network status changes.
                val documentWithPagesCopy = mutableListOf<DocumentWithPages>()
                documentWithPages.forEach {
                    documentWithPagesCopy.add(it.copy())
                }
                val docs = documentWithPagesCopy.sortedBy { doc -> doc.document.title }
                // append the current network status
                val hasUserAllowedMeteredNetwork = preferencesHandler.isUploadOnMeteredNetworksAllowed
                docs.forEach {
                    it.networkStatus = networkStatus
                    it.hasUserAllowedMeteredNetwork = hasUserAllowedMeteredNetwork
                }
                docs
            }.collectLatest { aggregatedDoc ->
                observableDocuments.postValue(aggregatedDoc)
            }
        }

        viewModelScope.launch {
            networkUtil.watchNetworkAvailability().collectLatest {
                networkStatusState.value = it
            }
        }
    }

    fun deleteDocument(documentWithPages: DocumentWithPages) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDocument(documentWithPages)
        }
    }
}
