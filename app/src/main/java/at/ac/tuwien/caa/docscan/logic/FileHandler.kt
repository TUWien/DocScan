package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
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
        const val FOLDER_EXPORTS = "exports"
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
     * @return the internal cache temp folder.
     */
    private fun getExportCacheFolder() =
        File(context.cacheDir.absolutePath + File.separator + FOLDER_EXPORTS)

    /**
     * Post-Condition: No guarantees if the documents folder exists.
     * @return the file reference to the root's documents folder
     */
    private fun getDocumentsFolder() =
        File(context.filesDir.absolutePath + File.separator + FOLDER_DOCUMENTS)

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
     * Pre-Condition: [this] [Uri] has write access.
     * @return [FileOutputStream] of [Uri].
     */
    @Throws(Exception::class)
    private fun Uri.fileOutputStream(): FileOutputStream {
        return FileOutputStream(
            context.contentResolver.openFileDescriptor(
                this,
                "rw",
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

    fun createCacheFile(fileId: UUID, fileType: PageFileType = PageFileType.JPEG): File {
        val tempFolder = getCacheTempFolder().createFolderIfNecessary()
        val file =
            File(tempFolder.absolutePath + File.separator + fileId.toString() + "." + fileType.extension)
        // delete the previous cached file
        file.safelyDelete()
        // create it if necessary
        file.createFileIfNecessary()
        return file
    }

    fun createCacheFileForExport(fileId: UUID, fileType: PageFileType = PageFileType.PDF): File {
        val tempFolder = getExportCacheFolder().createFolderIfNecessary()
        val file =
            File(tempFolder.absolutePath + File.separator + fileId.toString() + "." + fileType.extension)
        // delete the previous cached file
        file.safelyDelete()
        // create it if necessary
        file.createFileIfNecessary()
        return file
    }

    fun createDocumentFile(documentId: UUID, fileId: UUID, fileType: PageFileType): File {
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

    fun copyFileResource(from: File, to: File, deleteOnFail: Boolean = false): Resource<Unit> {
        return try {
            copyFile(from, to, deleteOnFail)
            Success(Unit)
        } catch (exception: Exception) {
            IOErrorCode.FILE_COPY_ERROR.asFailure(exception)
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

    @Throws(Exception::class)
    fun copyFileToUri(from: File, to: Uri) {
        try {
            from.inputStream().use { input ->
                to.fileOutputStream().use { output ->
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

    private fun getImageFileByPage(docId: UUID, fileId: UUID): File? {
        val file =
            getDocumentFileById(
                docId.toString(),
                fileId.toString() + "." + PageFileType.JPEG.extension
            )
        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    fun getFileByPage(page: Page?): File? {
        if (page == null) return null
        return getImageFileByPage(page.docId, page.id)
    }

    fun getFileByPageResource(page: Page?): Resource<File> {
        val file = getFileByPage(page)
        return if (file == null) {
            IOErrorCode.FILE_MISSING.asFailure()
        } else {
            Success(file)
        }
    }

    fun deleteEntireDocumentFolder(docId: UUID) {
        getDocumentFolderById(docId.toString()).safelyRecursiveDelete()
    }

    fun getUriByPageResource(page: Page, outputFileName: String): Resource<Uri> {
        val file = when (val fileResource = getFileByPageResource(page)) {
            is Failure -> {
                return Failure(fileResource.exception)
            }
            is Success -> {
                fileResource.data
            }
        }

        return try {
            Success(getUri(file, outputFileName))
        } catch (e: Exception) {
            IOErrorCode.SHARE_URI_FAILED.asFailure(e)
        }
    }

    @Throws(Exception::class)
    private fun getUri(file: File, fileName: String): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file,
            fileName
        )
    }
}

/**
 * Computes a MD5 file hash.
 * - The has needs to be in lowercase format, which is expected by the transkribus backend.
 * @return the hash string for the file, empty string if the hashing should fail
 */
fun File.getFileHash(): String {
    return try {
        return calcHash().toHexString().lowercase()
    } catch (e: Exception) {
        Timber.e("Computing hash of file: $name")
        ""
    }
}

private fun File.calcHash(algorithm: String = "MD5", bufferSize: Int = 1024): ByteArray {
    this.inputStream().use { input ->
        val buffer = ByteArray(bufferSize)
        val digest = MessageDigest.getInstance(algorithm)

        read@ while (true) {
            when (val bytesRead = input.read(buffer)) {
                -1 -> break@read
                else -> digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest()
    }
}

private fun ByteArray.toHexString(): String {
    return this.fold(StringBuilder()) { result, b -> result.append(String.format("%02X", b)) }
        .toString()
}

enum class PageFileType(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpg"),
    PDF("pdf", "application/pdf");

    companion object {
        fun getFileTypeByExtension(extension: String?): PageFileType {
            extension ?: return JPEG
            values().forEach { fileType ->
                if (fileType.extension == extension) {
                    return JPEG
                }
            }
            return JPEG
        }
    }
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
