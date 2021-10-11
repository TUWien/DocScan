package at.ac.tuwien.caa.docscan.ui.document

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.TranskribusMetaData
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class CreateDocumentViewModel(val repository: DocumentRepository) : ViewModel() {

    val observableResource = MutableLiveData<Resource<Document>>()

    fun createDocument(title: String, transkribusMetaData: TranskribusMetaData?) {
        viewModelScope.launch(Dispatchers.IO) {
            val document = Document(UUID.randomUUID(), title, isActive = true)
            transkribusMetaData?.let {
                document.metaData = MetaData(
                    relatedUploadId = transkribusMetaData.relatedUploadId,
                    author = transkribusMetaData.author,
                    authority = transkribusMetaData.authority,
                    genre = transkribusMetaData.genre,
                    language = transkribusMetaData.language,
                    isProjectReadme2020 = transkribusMetaData.readme2020,
                    allowImagePublication = transkribusMetaData.readme2020Public,
                    signature = transkribusMetaData.signature,
                    url = transkribusMetaData.url,
                    writer = transkribusMetaData.writer
                )
            }
            observableResource.postValue(repository.createOrUpdateDocument(document))
        }
    }
}
