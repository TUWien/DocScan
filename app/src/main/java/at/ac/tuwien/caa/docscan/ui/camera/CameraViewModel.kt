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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.*

class CameraViewModel(
    private val documentRepository: DocumentRepository,
    private val preferencesHandler: PreferencesHandler,
    private val app: DocScanApp
) : ViewModel() {

    val observableActiveDocument: MutableLiveData<DocumentWithPages?> = MutableLiveData()
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
        collectorJob = viewModelScope.launch {
            // if the doc can be found, then keep listening to it, only fallback to active if the
            // requested doc does not exist
            if (docId != null) {
                val doc = documentRepository.getDocumentWithPages(docId)
                if (doc != null) {
                    documentRepository.getDocumentWithPagesAsFlow(docId).distinctUntilChanged()
                        .collectLatest {
                            observableActiveDocument.postValue(it)
                        }
                    return@launch
                }
            }

            documentRepository.getActiveDocumentAsFlow().distinctUntilChanged().collectLatest {
                observableActiveDocument.postValue(it)
            }
        }
    }

    fun saveRawImageData(data: ByteArray, orientation: Int, cameraDpi: Int, location: Location?) {
        viewModelScope.launch(Dispatchers.IO) {
            observableImageLoadingProgress.postValue(true)
            val isRetakeMode = isRetakeMode()

            // TODO: This should be made less complicated
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
                doc = (observableActiveDocument.value?.document
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
                doc,
                data,
                if (isRetakeMode) retakePageId else null,
                exifMetaData
            )
            when (result) {
                is Failure -> {
                    FirebaseCrashlytics.getInstance().recordException(result.exception)
                    // TODO: Add error handling if necessary
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

    fun initiateGallery() {
        // TODO: handle case when document is not available!
        val id = observableActiveDocument.value?.document?.id ?: return
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
