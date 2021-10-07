package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.net.Uri
import android.util.Log
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener


/**
 * @author matejbart
 */
class FileHandler(private val context: Context) {

    private val TAG = "FileHandler"
    private val gson by lazy { GsonBuilder().create() }

    companion object {

        private const val ASSET_FOLDER_SEGMENTATION = "segmentation"
        private val ASSET_FOLDER_SEGMENTATION_META =
            ASSET_FOLDER_SEGMENTATION + File.separator + "meta"
        val ASSET_FOLDER_SEGMENTATION_MODELS =
            ASSET_FOLDER_SEGMENTATION + File.separator + "models"
        const val FOLDER_DOCUMENTS = "documents"
        const val FOLDER_TEMP = "temp"
    }

    /**
     * Post-Condition: No guarantees if the documents folder exists.
     * @return the internal cache temp folder.
     */
    private fun getCacheTempFolder() =
        File(context.cacheDir.absolutePath + File.separator + FOLDER_TEMP)

    /**
     * Post-Condition: No guarantees if the documents folder exists.
     * @return the file reference to the root's documents folder
     */
    // TODO: Change this back to internal storage!
    private fun getDocumentsFolder() =
        File(context.getExternalFilesDir("DocScan")!!.absolutePath + File.separator + FOLDER_DOCUMENTS)

    /**
     * Post-Condition: No guarantees if the specific documents folder exists.
     * @return the file reference to the specific document folder.
     */
    private fun getDocumentFolderById(id: String): File {
        val docFolder = getDocumentsFolder()
        return File(docFolder.absolutePath + File.separator + id)
    }

    /**
     * Post-Condition: No guarantees if the specific file exists.
     * @return the file reference to the file for a specific document.
     */
    private fun getDocumentFileById(
        docId: String,
        fileId: String,
        createIfNecessary: Boolean = false
    ): File {
        val specificDocFolder = getDocumentFolderById(docId)
        if (createIfNecessary) {
            specificDocFolder.createFolderIfNecessary()
        }
        val file = File(specificDocFolder.absolutePath + File.separator + fileId)
        if (createIfNecessary) {
            file.createNewFile()
        }
        return file
    }

    /**
     * Saves [newImage] into the internal storage with the help of [DocumentStorage].
     */
    @Throws(Exception::class)
    fun saveFile(newImage: Uri) {
        // TODO: Importing images is just for debugging purposes, the storage handling has changed, this needs to be adapted
//        try {
//            val resolver = context.contentResolver
//            val targetUri = CameraActivity.getFileName(context.getString(R.string.app_name))
//            resolver.openFileDescriptor(targetUri, "w", null)?.let {
//                val outputStream = FileOutputStream(it.fileDescriptor)
//                val inputStream = newImage.fileInputStream()
//                inputStream.copyTo(outputStream)
//                outputStream.close()
//                inputStream.close()
//                it.close()
//            }
//
//            val file = File(targetUri.path!!)
//            if (!DocumentStorage.getInstance(context).addToActiveDocument(file)) {
//                DocumentStorage.getInstance(context).generateDocument(file, context)
//            }
//            DocumentStorage.saveJSON(context)
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to save image!")
//        }
    }

    /**
     * Pre-Condition: [this] [Uri] has at least read access.
     * @return [FileInputStream] of [Uri].
     */
    @Throws(Exception::class)
    private fun Uri.fileInputStream(): FileInputStream {
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
    private fun getAssetFiles(subFolder: String): List<String> {
        return try {
            context.resources.assets.list(subFolder)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get list of assets for $subFolder")
            emptyList()
        }
    }

    /**
     * @return all tf lite models from the assets folder.
     */
    fun getAllTFModelsFromAssets(): List<TFLiteModel> {
        val list = mutableListOf<TFLiteModel>()
        getAssetFiles(ASSET_FOLDER_SEGMENTATION_META).forEach {
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

    fun createCacheFile(fileId: UUID, fileType: FileType): File {
        val tempFolder = getCacheTempFolder().createFolderIfNecessary()
        return File(tempFolder.absolutePath + File.separator + fileId.toString() + "." + fileType.extension).createFileIfNecessary()
    }

    fun createDocumentFile(documentId: UUID, fileId: UUID, fileType: FileType): File {
        return getDocumentFileById(
            documentId.toString(),
            fileId = fileId.toString() + "." + fileType.extension,
            createIfNecessary = true
        )
    }

    fun safelyCopyFile(from: File, to: File) {
        try {
            copyFile(from, to)
        } catch (exception: Exception) {
            // ignore
        }
    }

    @Throws(Exception::class)
    fun copyFile(from: File, to: File, deleteOnFail: Boolean = false) {
        try {
            from.copyTo(to, overwrite = true)
        } catch (exception: Exception) {
            if (deleteOnFail) {
                // if it fails then delete the destination file
                to.safelyDelete()
            }
            throw exception
        }
    }

    @Throws(Exception::class)
    fun copyByteArray(from: ByteArray, to: File) {
        try {
            to.outputStream().use {
                it.write(from)
            }
        } catch (exception: Exception) {
            throw exception
        }
    }

    fun getFileByAbsolutePath(absolutePath: String): File? {
        val file = File(absolutePath)
        return try {
            if (file.exists() && file.canRead()) {
                file
            } else {
                null
            }
        } catch (e: Exception) {
            // TODO: Log Exception
            null
        }
    }

    fun getImageFileByPage(docId: UUID, fileId: UUID): File? {
        val file =
            getDocumentFileById(docId.toString(), fileId.toString() + "." + FileType.JPEG.extension)
        return if (file.exists()) {
            file
        } else {
            // TODO: Check if this can ever happen
            null
        }
    }

    fun getFileByPage(page: Page): File? {
        return getImageFileByPage(page.docId, page.id)
    }
}

enum class FileType(val extension: String) {
    JPEG("jpg")
}

private fun File.safelyRecursiveDelete() {
    try {
        this.deleteRecursively()
    } catch (exception: Exception) {
        // ignore or log
    }
}

fun File.safelyDelete(forceLog: Boolean = false) {
    try {
        this.delete()
    } catch (exception: Exception) {
        if (forceLog) {
            Timber.e("Unable to delete file!", exception)
        }
    }
}

private fun File.createFolderIfNecessary(): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    return this
}

private fun File.createFileIfNecessary(): File {
    if (!this.exists()) {
        this.createNewFile()
    }
    return this
}
