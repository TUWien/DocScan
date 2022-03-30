package at.ac.tuwien.caa.docscan.ui.camera

import android.location.Location
import android.util.Rational
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ExifLocation
import at.ac.tuwien.caa.docscan.camera.ExifResolution
import at.ac.tuwien.caa.docscan.camera.GPS
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class CameraViewModel(
    private val documentRepository: DocumentRepository,
    private val preferencesHandler: PreferencesHandler,
    private val app: DocScanApp
) : ViewModel() {

    val observableDocumentWithPages: MutableLiveData<DocumentWithPages?> = MutableLiveData()
    val observableThumbnail: MutableLiveData<Page> = MutableLiveData()
    val observableImageLoadingProgress: MutableLiveData<Boolean> = MutableLiveData()
    val observableTookImage: MutableLiveData<Event<Resource<Page>>> = MutableLiveData()
    val observableOpenGallery = MutableLiveData<Event<Pair<UUID, UUID?>>>()

    private var retakePageId: UUID? = null
    private var retakeDocId: UUID? = null

    private var collectorJob: Job? = null

    /**
     * @return true if the camera is in retake mode.
     */
    fun isRetakeMode() = retakeDocId != null && retakePageId != null

    fun load(docId: UUID?, pageId: UUID?) {
        retakePageId = pageId ?: retakePageId
        retakeDocId = docId ?: retakeDocId

        // cancel the old collector's job in case the load function is called again.
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                getDocumentFlow(docId).collectLatest { docWithPages ->
                    observableDocumentWithPages.postValue(docWithPages)
                }
            }
            launch {
                // the previous flow might emit multiple unwanted state changes, whereas this flow
                // will just emit if the file hash has changed, i.e. a new thumbnail should be loaded.
                getDocumentFlow(docId).distinctUntilChanged { old, new ->
                    old?.pages?.lastOrNull()?.fileHash == new?.pages?.lastOrNull()?.fileHash
                }.collectLatest { docWithPages ->
                    observableThumbnail.postValue(docWithPages?.pages?.lastOrNull())
                }
            }
        }
    }

    /**
     * Determines the current document data flow, in particular, it distinguishes between a
     * requested document (this is the case when [docId] is provided), otherwise it fallbacks
     * to the active document.
     * @return a flow for the currently shown document.
     */
    private suspend fun getDocumentFlow(docId: UUID?): Flow<DocumentWithPages?> {
        if (docId != null) {
            val doc = documentRepository.getDocumentWithPages(docId)
            if (doc != null) {
                return documentRepository.getDocumentWithPagesAsFlow(docId)
            }
        }
        return documentRepository.getActiveDocumentAsFlow().distinctUntilChanged()
    }

    /**
     * Tries to save raw image data to the currently loaded [observableDocumentWithPages], adds
     * all necessary meta data to [Page] and appends it finally to the [Document].
     */
    fun saveRawImageData(data: ByteArray, orientation: Int, cameraDpi: Int, location: Location?) {
        viewModelScope.launch(Dispatchers.IO) {
            observableImageLoadingProgress.postValue(true)
            val isRetakeMode = isRetakeMode()
            val doc: Document
            if (isRetakeMode) {
                // check if the ids are still valid
                val retakePageId = retakePageId ?: run {
                    savingHasFailed()
                    return@launch
                }
                val page = documentRepository.getPageById(retakePageId) ?: run {
                    savingHasFailed()
                    return@launch
                }
                doc = documentRepository.getDocumentWithPages(page.docId)?.document ?: run {
                    savingHasFailed()
                    return@launch
                }
                if (retakeDocId != doc.id) {
                    savingHasFailed()
                    return@launch
                }
            } else {
                doc = (observableDocumentWithPages.value?.document
                    ?: documentRepository.createNewActiveDocument())
            }

            val exifMetaData = ImageExifMetaData(
                exifOrientation = orientation,
                exifSoftware = app.resources.getString(R.string.app_name),
                exifArtist = preferencesHandler.exifArtist,
                exifCopyRight = preferencesHandler.exifCopyRight,
                location = location?.let {
                    ExifLocation(
                        // Taken from http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android (post by fabien):
                        GPS.convert(it.latitude),
                        GPS.latitudeRef(it.latitude),
                        GPS.convert(it.longitude),
                        GPS.longitudeRef(it.longitude)
                    )
                },
                resolution = kotlin.run {
                    if (cameraDpi != -1) {
                        return@run null
                    } else {
                        // TODO: Is this even correct?
                        val rational = Rational(cameraDpi, 1)
                        ExifResolution(rational.toString(), rational.toString())
                    }
                }
            )
            val result = documentRepository.saveNewImageForDocument(
                doc.id,
                data,
                if (isRetakeMode) retakePageId else null,
                exifMetaData
            )
            when (result) {
                is Failure -> {
                    Timber.e(result.exception, "New image failed to be persisted")
                }
                is Success -> {
                    if (isRetakeMode) {
                        initiateGallery()
                    }
                }
            }
            observableTookImage.postValue(Event(result))
            observableImageLoadingProgress.postValue(false)
        }
    }

    /**
     * Initiates the gallery, but ignores the operation if no current document is available.
     */
    fun initiateGallery() {
        val id = observableDocumentWithPages.value?.document?.id ?: return
        observableOpenGallery.postValue(Event(Pair(id, retakePageId)))
        // clear the retake ids
        removeRetakeMode()
    }

    private fun savingHasFailed() {
        observableImageLoadingProgress.postValue(false)
        observableTookImage.postValue(Event(Failure(Exception("saving has failed!"))))
    }

    private fun removeRetakeMode() {
        retakeDocId = null
        retakePageId = null
    }
}
