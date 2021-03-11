package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.CameraActivity
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @author matejbart
 */
object FileHandler {

    private val TAG = "FileHandler"
    private val gson by lazy { GsonBuilder().create() }

    private const val ASSET_FOLDER_SEGMENTATION = "segmentation"
    private val ASSET_FOLDER_SEGMENTATION_META = ASSET_FOLDER_SEGMENTATION + File.separator + "meta"
    val ASSET_FOLDER_SEGMENTATION_MODELS =
        ASSET_FOLDER_SEGMENTATION + File.separator + "models"

    /**
     * Saves [newImage] into the internal storage with the help of [DocumentStorage].
     */
    @Throws(Exception::class)
    fun saveFile(context: Context, newImage: Uri) {
        try {
            val resolver = context.contentResolver
            val targetUri = CameraActivity.getFileName(context.getString(R.string.app_name))
            resolver.openFileDescriptor(targetUri, "w", null)?.let {
                val outputStream = FileOutputStream(it.fileDescriptor)
                val inputStream = newImage.fileInputStream(context)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()
                it.close()
            }

            val file = File(targetUri.path!!)
            if (!DocumentStorage.getInstance(context).addToActiveDocument(file)) {
                DocumentStorage.getInstance(context).generateDocument(file, context)
            }
            DocumentStorage.saveJSON(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image!", e)
        }
    }

    /**
     * Pre-Condition: [this] [Uri] has at least read access.
     * @return [FileInputStream] of [Uri].
     */
    @Throws(Exception::class)
    private fun Uri.fileInputStream(context: Context): FileInputStream {
        return FileInputStream(
            context.contentResolver.openFileDescriptor(
                this,
                "r",
                null
            )!!.fileDescriptor
        )
    }

    /**
     * @return a list of file names of the [subFolder] in the assets.
     */
    @Suppress("SameParameterValue")
    private fun getAssetFiles(context: Context, subFolder: String): List<String> {
        return try {
            context.resources.assets.list(subFolder)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get list of assets for $subFolder", e)
            emptyList()
        }
    }

    /**
     * @return all tf lite models from the assets folder.
     */
    fun getAllTFModelsFromAssets(context: Context): List<TFLiteModel> {
        val list = mutableListOf<TFLiteModel>()
        getAssetFiles(context, ASSET_FOLDER_SEGMENTATION_META).forEach {
            try {
                // open meta json
                val json =
                    context.assets.open(ASSET_FOLDER_SEGMENTATION_META + File.separator + it)
                        .bufferedReader()
                        .readText()
                list.add(gson.fromJson(json, TFLiteModel::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open/parse $it", e)
            }
        }
        return list
    }
}
