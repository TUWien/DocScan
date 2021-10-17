package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * A utility class for file management
 * @author matejbart
 */
class FileHandler(private val context: Context) {

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
                Timber.e(e, "Failed to open/parse $it")
            }
        }
        return list
    }

    fun createCacheFile(fileId: UUID, fileType: FileType = FileType.JPEG): File {
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

    @Throws(Exception::class)
    fun copyUriToFile(from: Uri, to: File) {
        try {
            to.outputStream().use { output ->
                from.fileInputStream().use { input ->
                    input.copyTo(output)
                }
            }
        } catch (exception: Exception) {
            throw exception
        }
    }

    @Throws(IOException::class)
    fun readBytes(uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

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

    fun deleteEntireDocumentFolder(docId: UUID) {
        getDocumentFolderById(docId.toString()).safelyRecursiveDelete()
    }

    fun getUriByPage(page: Page): Uri? {
        val file = getFileByPage(page) ?: return null
        return try {
            getUri(file)
        } catch (e: Exception) {
            Timber.w("Unable to retrieve uri from page!", e)
            null
        }
    }

    private fun getUri(file: File): Uri? {
        return FileProvider.getUriForFile(context, "at.ac.tuwien.caa.fileprovider", file)
    }
}

/**
 * Computes a file hash with crc32, this is important for image files in order to detect
 * changes via the DB.
 * @return the hash string for the file, empty string if the hashing should fail
 */
fun File.getFileHash(): String {
    return try {
        Files.asByteSource(this).hash(Hashing.crc32()).toString()
    } catch (e: Exception) {
        Timber.e("Computing hash of file: $name")
        ""
    }
}

enum class FileType(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpg")
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
