package at.ac.tuwien.caa.docscan.ui.segmentation

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.ui.segmentation.model.ModelExecutionResult
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @author matejbart
 */
class SegmentationViewModel(extras: Bundle, val app: DocScanApp, val fileHandler: FileHandler) :
    ViewModel() {

    // retrieve the image file path
    private val imageAbsolutePath: String =
        extras.getString(SegmentationActivity.EXTRA_IMAGE_FILE_PATH)!!

    val observableImagePath = MutableLiveData<Event<String>>()
    val observableResults = MutableLiveData<List<ModelExecutionResult>>()
    val observableProgress = MutableLiveData<Boolean>()

    //    val observableMeta = MutableLiveData<MetaResult>()
    val observableError = MutableLiveData<Event<Exception>>()
    private val observableModels = MutableLiveData<List<TFLiteModel>>()
    val models =
        fileHandler.getAllTFModelsFromAssets().sortedByDescending { model -> model.inputSize }

    // default case, perform segmentation with first model in the list.
    var latestModel = models.first()

    private var currentJob: Job? = null

    /**
     * Represents the labels, the ids of the classes are defined by its order index.
     * E.g. class page -> 1
     */
    private val labels = listOf(
        Label("Background", Color.parseColor("#80000000")),
        Label("Page", Color.parseColor("#8000FF00")),
        Label("PageSplit", Color.parseColor("#80FF0000")),
        Label("Finger", Color.parseColor("#800000FF"))
    )

    init {
        observableImagePath.value = Event(imageAbsolutePath)
        observableModels.value = models
        performSegmentation(listOf(latestModel), true)
    }

    fun performSegmentationByModelName(
        modelFileName: String? = null,
        isGPU: Boolean,
        isAll: Boolean
    ) {
        // cancel previous jobs
        if (currentJob?.isActive == true) {
            currentJob?.cancel()
        }
        modelFileName?.let {
            latestModel = models.find { it.title == modelFileName }!!
        }
        currentJob = performSegmentation(
            if (isAll) {
                models
            } else {
                listOf(latestModel)
            }, isGPU
        )
    }

    /**
     * Runs the segmentation on the defined [models].
     */
    private fun performSegmentation(models: List<TFLiteModel>, useGPU: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            observableProgress.postValue(true)
            val result = work(models, useGPU)
            if (!isActive) {
                return@launch
            }
            observableResults.postValue(result)
            observableProgress.postValue(false)
        }

    private fun work(models: List<TFLiteModel>, useGPU: Boolean): List<ModelExecutionResult> {
        val list = mutableListOf<ModelExecutionResult>()
        try {
            val inputBitmap = BitmapFactory.decodeFile(imageAbsolutePath)
            models.forEach { model ->
                try {
                    val result = SegmentationExecutor().execute(
                        app,
                        inputBitmap,
                        513,
                        labels,
                        model,
                        useGPU
                    )
                    list.add(result)
                } catch (e: Exception) {
                    Timber.e(e, "Segmentation has failed!")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Decoding of input image has failed!")

        }
        return list
    }
}
