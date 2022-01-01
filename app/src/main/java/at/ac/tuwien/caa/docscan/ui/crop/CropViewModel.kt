package at.ac.tuwien.caa.docscan.ui.crop

import android.graphics.PointF
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.getSingleBoundaryPoints
import at.ac.tuwien.caa.docscan.db.model.setSinglePageBoundary
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.ImageProcessorRepository
import at.ac.tuwien.caa.docscan.ui.crop.CropViewActivity.Companion.EXTRA_PAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class CropViewModel(
    extras: Bundle,
    private val documentRepository: DocumentRepository,
    private val imageProcessorRepository: ImageProcessorRepository,
    private val fileHandler: FileHandler,
    val preferencesHandler: PreferencesHandler
) : ViewModel() {

    val page = extras.getParcelable<Page>(EXTRA_PAGE)!!
    val observableInitBackNavigation = MutableLiveData<Event<Unit>>()
    val observableShowCroppingInfo = MutableLiveData<Event<Unit>>()
    val observableLoadingProgress = MutableLiveData<Boolean>()

    // note that the very first value of the model will force the image to load into the imageview.
    val observableModel = MutableLiveData<CropModel>()

    var isRotating = false

    private val file = fileHandler.createCacheFile(page.id)
    var model = CropModel(
        page.id,
        file,
        page.rotation,
        page.rotation,
        calculateImageResolution(file, page.rotation),
        page.getSingleBoundaryPoints()
    )

    init {
        init()
    }

    private fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            observableLoadingProgress.postValue(true)
            val pageFile = fileHandler.getFileByPage(page)
                ?: kotlin.run {
                    observableInitBackNavigation.postValue(Event(Unit))
                    return@launch
                }

            // clear the currently cached file
            file.safelyDelete()

            // create a copy of the file, so any manipulations can be easily reverted.
            fileHandler.safelyCopyFile(
                pageFile,
                model.file
            )
            observableLoadingProgress.postValue(false)
            observableModel.postValue(model)
        }

    }

    fun navigateBack() {
        viewModelScope.launch(Dispatchers.IO) {
            file.safelyDelete()
            observableInitBackNavigation.postValue(Event(Unit))
        }
    }

    fun rotateBy90Degree(croppingPoints: List<PointF>) {
        if (isRotating) {
            return
        }
        isRotating = true
        viewModelScope.launch(Dispatchers.IO) {
            model.points = croppingPoints
            model.previousRotation = model.rotation
            model.rotation = model.rotation.rotateBy90Clockwise()
            imageProcessorRepository.rotateFile(model.file, model.rotation)
            model.meta = calculateImageResolution(model.file, model.rotation)
            observableModel.postValue(model)
        }
    }

    fun save(croppingPoints: List<PointF>) {
        viewModelScope.launch(Dispatchers.IO) {
            model.points = croppingPoints
            imageProcessorRepository.replacePageFileBy(
                pageId = page.id,
                cachedFile = model.file,
                rotation = model.rotation,
                croppingPoints = model.points
            )
            model.file.safelyDelete()

            if (preferencesHandler.showCroppingInfo) {
                observableShowCroppingInfo.postValue(Event(Unit))
            } else {
                observableInitBackNavigation.postValue(Event(Unit))
            }
        }
    }

    fun updateCroppingPoints(croppingPoints: List<PointF>) {
        model.points = croppingPoints
    }
}

data class CropModel(
    val id: UUID,
    val file: File,
    var previousRotation: Rotation,
    var rotation: Rotation,
    var meta: ImageMeta,
    var points: List<PointF>
)

data class ImageMeta(val width: Int, val height: Int, val aspectRatio: Double)
