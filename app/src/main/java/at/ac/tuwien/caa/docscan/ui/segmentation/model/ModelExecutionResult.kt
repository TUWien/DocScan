package at.ac.tuwien.caa.docscan.ui.segmentation.model

import android.graphics.Bitmap
import at.ac.tuwien.caa.docscan.ui.segmentation.Label

/**
 * @author matejbart
 */
data class ModelExecutionResult(
    val bitmapResult: Bitmap,
    val bitmapScaledOriginal: Bitmap,
    val bitmapScaledMaskOnly: Bitmap,
    val meta: MetaResult,
    val labels: List<Label>
)

/**
 * @author matejbart
 */
data class MetaResult(
    val model: TFLiteModel,
    val isGPU: Boolean,
    val numThreads: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val modelInputSize: Int,
    val decodeImageFromFileTaskTime: Long,
    val downScalingTaskTime: Long,
    val loadInterpreterTaskTime: Long,
    val allocateOutputBufferTaskTime: Long,
    val inferenceTaskTime: Long,
    val flattenMaskTaskTime: Long,
    val rescalingTaskTime: Long,
) {
    val totalTaskTime =
        (decodeImageFromFileTaskTime + downScalingTaskTime + loadInterpreterTaskTime + allocateOutputBufferTaskTime + inferenceTaskTime + flattenMaskTaskTime + rescalingTaskTime).toString() + " ms"

    val inferenceTaskTimeAsString =
        (inferenceTaskTime).toString() + " ms (inference)"

    val imageSize = "$imageWidth x $imageHeight"
}
