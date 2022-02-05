package at.ac.tuwien.caa.docscan.ui.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import at.ac.tuwien.caa.docscan.ui.segmentation.ImageUtils.Companion.convertByteBufferMaskToBitmap
import at.ac.tuwien.caa.docscan.ui.segmentation.model.MetaResult
import at.ac.tuwien.caa.docscan.ui.segmentation.model.ModelExecutionResult
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * This class is mainly based on the implementation of the official examples:
 * https://github.com/tensorflow/examples/tree/master/lite/examples/image_segmentation/android
 * Some improvements and built-in tensorflow lite classes has been used to simplify the necessary
 * installation.
 *
 * The task_api is currently not used, since the trained tflite models used an older tf version,
 * for which it's not possible to add the necessary meta data. Furthermore, GPU processing
 * is currently not supported by the task_api.
 *
 * @author matejbart
 */
class SegmentationExecutor {

    companion object {
        private const val TAG = "SegmentationExecutor"
    }

    /**
     * @param imageFilePath the path of the image on which the segmentation is performed.
     * @param size the input size of the model. it's assumed that weight == height.
     * @param labels the classes which are used by model.
     * @param model the meta info model.
     * @param useGPU if true, then the GPU delegate is used.
     * @param numThreads num of threads on which the segmentation is performed.
     *
     * @return [ModelExecutionResult] which contains multiple bitmaps and meta infos.
     */
    @Throws(Exception::class)
    fun execute(
        context: Context,
        inputImage: Bitmap,
        size: Int = 513,
        labels: List<Label>,
        model: TFLiteModel,
        useGPU: Boolean = true,
        numThreads: Int = 4
    ): ModelExecutionResult {

        val imageHeight = inputImage.height
        val imageWidth = inputImage.width

        // 1. scale the image down to the desired input size of the model
        // 2. convert the image into a UINT8 representation
        val (scaledInputImage, downScaleInputImageTaskTime) = task {
            val imageProcessor =
                ImageProcessor.Builder()
                    // Center crop the image to the largest square possible
                    .add(ResizeOp(size, size, ResizeOp.ResizeMethod.BILINEAR))
                    .build()
            // convert to UINT8
            val tfImage = TensorImage(DataType.UINT8)
            tfImage.load(inputImage)
            imageProcessor.process(tfImage)
        }

        // 3. load the interpreter
        val (interpreter, loadModelTaskTime) = task {
            loadInterpreter(context, model.getRelativeAssetFolderPath(), useGPU, numThreads)
        }

        // 4. allocate tensor output buffer
        val (outputTensorBufferEmpty, allocateOutputTensorBufferTaskTime) = task {
            ByteBuffer.allocateDirect(1 * size * size * 4).apply {
                order(ByteOrder.nativeOrder())
            }
        }

        // 5. run inference on interpreter
        val (outputTensorBufferWithPredictions, inferenceTaskTime) = task {
            interpreter.run(scaledInputImage.buffer, outputTensorBufferEmpty)
            outputTensorBufferEmpty
        }

        // 6. flatten the mask (i.e. read out the label predictions)
        val (resultBitmapMask, flattenMaskTaskTime) = task {
            convertByteBufferMaskToBitmap(
                outputTensorBufferWithPredictions,
                size,
                labels.map {
                    it.colorInt
                }.toIntArray()
            )
        }

        // re-scale mask
        val (resultBitmap, rescaleMaskTaskTime) = task {
            stackTwoBitmaps(
                Bitmap.createScaledBitmap(
                    resultBitmapMask,
                    imageWidth,
                    imageHeight,
                    true
                ), inputImage
            )
        }

        try {
            interpreter.close()
        } catch (e: Exception) {
            Timber.e("Failed to close interpreter!", e)
        }

        return ModelExecutionResult(
            resultBitmap,
            scaledInputImage.bitmap,
            resultBitmapMask,
            MetaResult(
                model = model,
                isGPU = useGPU,
                numThreads = numThreads,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                modelInputSize = size,
                decodeImageFromFileTaskTime = -1,
                downScalingTaskTime = downScaleInputImageTaskTime,
                loadInterpreterTaskTime = loadModelTaskTime,
                allocateOutputBufferTaskTime = allocateOutputTensorBufferTaskTime,
                inferenceTaskTime = inferenceTaskTime,
                flattenMaskTaskTime = flattenMaskTaskTime,
                rescalingTaskTime = rescaleMaskTaskTime
            ),
            labels
        )
    }

    /**
     * Loads the interpreter based on the modelName.
     */
    @Throws(IOException::class)
    private fun loadInterpreter(
        context: Context,
        modelName: String,
        useGPU: Boolean = true,
        numThreads: Int = 4
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(numThreads)

        // add gpu delegate if activated
        if (useGPU) {
            tfliteOptions.addDelegate(GpuDelegate())
        }

        // load the tflite model from file, the function assumes that the modelname is the relatvie
        // path of the model in the asset folder.
        return Interpreter(
            FileUtil.loadMappedFile(
                context, modelName
            ), tfliteOptions
        )
    }

    /**
     * Stacks two bitmaps on top of each other.
     */
    private fun stackTwoBitmaps(foreground: Bitmap, background: Bitmap): Bitmap {
        val mergedBitmap =
            Bitmap.createBitmap(foreground.width, foreground.height, foreground.config)
        val canvas = Canvas(mergedBitmap)
        canvas.drawBitmap(background, 0.0f, 0.0f, null)
        canvas.drawBitmap(foreground, 0.0f, 0.0f, null)
        return mergedBitmap
    }

    /**
     * A wrapper for measuring the time of a [job].
     */
    private fun <T> task(job: () -> T): Pair<T, Long> {
        val timeStampBefore = System.currentTimeMillis()
        val result = job.invoke()
        val timeStampNow = System.currentTimeMillis() - timeStampBefore
        return Pair(result, timeStampNow)
    }
}
