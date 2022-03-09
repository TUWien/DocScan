@file:Suppress("BlockingMethodInNonBlockingContext")

package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.text.format.Formatter
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.work.WorkInfo
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.ui.segmentation.model.TFLiteModel
import at.ac.tuwien.caa.docscan.worker.DocScanWorkInfo
import at.ac.tuwien.caa.docscan.worker.getCurrentWorkerJobStates
import com.google.gson.GsonBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.*
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A utility class for file management
 * @author matejbart
 */
class FileHandler(private val context: Context, private val storageManager: StorageManager) {

    private val gson by lazy { GsonBuilder().create() }

    companion object {

        private const val ASSET_FOLDER_SEGMENTATION = "segmentation"
        private val ASSET_FOLDER_SEGMENTATION_META =
            ASSET_FOLDER_SEGMENTATION + File.separator + "meta"
        val ASSET_FOLDER_SEGMENTATION_MODELS =
            ASSET_FOLDER_SEGMENTATION + File.separator + "models"
        const val FOLDER_DOCUMENTS = "documents"
        const val FOLDER_EXPORTS = "exports"
        const val FOLDER_LOGS = "logs"
        const val FOLDER_TEMP = "temp"

        // the reason for using two dedicated files is that if one exceeds a certain limit,
        // then the second one will be taken and if both are exceeding, then the newest one (B)
        // replaces the old one (A).
        const val FILE_INTERNAL_FILE_LOG_A = "internal_log_A.txt"
        const val FILE_INTERNAL_FILE_LOG_B = "internal_log_B.txt"

        const val NUM_BYTES_REQUIRED_FOR_MIGRATION = 1024 * 1024 * 100L
        const val MAX_NUM_BYTES_REQUIRED_FOR_LOG_FILE = 1024 * 1024 * 50L

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
     * Post-Condition: No guarantees if the documents folder exists.
     * @return the file reference to the root's logs folder.
     */
    private fun getLogsFolder() =
        File(context.filesDir.absolutePath + File.separator + FOLDER_LOGS)

    /**
     * Post-Condition: No guarantees if the specific documents folder exists.
     * @return the file reference to the specific document folder.
     */
    private fun getDocumentFolderById(id: String): File {
        val docFolder = getDocumentsFolder()
        return File(docFolder.absolutePath + File.separator + id)
    }

    /**
     * Represents a mutex for appending log output to guarantee thread-safety.
     */
    private val logMutex = Mutex()

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

    private fun getLogoutPutFiles(): Pair<File, File> {
        val logFolder = getLogsFolder().createFolderIfNecessary()
        val fileA =
            File(logFolder.absolutePath + File.separator + FILE_INTERNAL_FILE_LOG_A).createFileIfNecessary()
        val fileB =
            File(logFolder.absolutePath + File.separator + FILE_INTERNAL_FILE_LOG_B).createFileIfNecessary()

        return Pair(fileA, fileB)
    }

    private fun getOutputLogFile(): File {
        val logFiles = getLogoutPutFiles()
        val fileA = logFiles.first
        val fileB = logFiles.second

        return if (fileA.length() < MAX_NUM_BYTES_REQUIRED_FOR_LOG_FILE) {
            fileA
        } else {
            if (fileB.length() < MAX_NUM_BYTES_REQUIRED_FOR_LOG_FILE) {
                fileB
            } else {
                // if both files are already exceeded, then copy B to A, delete B and return B.
                fileB.copyTo(fileA, overwrite = true)
                fileB.safelyDelete()
                fileB.createFileIfNecessary()
                fileB
            }
        }
    }

    @WorkerThread
    suspend fun appendToLog(text: String, throwable: Throwable?) {
        logMutex.withLock {
            PrintWriter(FileOutputStream(getOutputLogFile(), true)).use {
                it.println(text)
                throwable?.printStackTrace(it)
                it.flush()
            }
        }
    }

    @WorkerThread
    private fun copyLines(bufferedReader: BufferedReader, printWriter: PrintWriter) {
        var line = bufferedReader.readLine()
        while (line != null) {
            printWriter.println(line)
            line = bufferedReader.readLine()
        }
    }

    fun getSizeOf(documentWithPages: DocumentWithPages): Long {
        var bytes = 0L
        documentWithPages.pages.forEach { page ->
            bytes += getFileByPage(page)?.length() ?: 0L
        }
        return bytes
    }

    /**
     * @return an uri which is a zip of several .txt log files.
     *
     * - a text files which consists of the captured logging from custom timber tree.
     * - a file which contains several infos about the used device (see [appendDeviceInfo])
     * - a file which contains infos about pending worker manager jobs.
     */
    @WorkerThread
    suspend fun exportLogAsZip(): Resource<Uri> {
        logMutex.withLock {
            val outputFileName = "log"
            val tempZipFile = createCacheFile(UUID.randomUUID(), PageFileType.ZIP)
            val logFiles = getLogoutPutFiles()
            try {
                tempZipFile.outputStream().buffered().use { buffered ->
                    ZipOutputStream(buffered).use { out ->
                        out.bufferedWriter().use {
                            val entry = ZipEntry(outputFileName + "." + PageFileType.TXT.extension)
                            out.putNextEntry(entry)
                            PrintWriter(it).use { writer ->
                                logFiles.first.bufferedReader().use { reader ->
                                    copyLines(reader, writer)
                                }
                                logFiles.second.bufferedReader().use { reader ->
                                    copyLines(reader, writer)
                                }
                                writer.flush()
                                out.closeEntry()
                                val deviceDataEntry =
                                    ZipEntry("device_info" + "." + PageFileType.TXT.extension)
                                out.putNextEntry(deviceDataEntry)
                                writer.appendDeviceInfo()
                                writer.flush()
                                out.closeEntry()

                                val workerManagerStates =
                                    ZipEntry("worker_jobs" + "." + PageFileType.TXT.extension)
                                out.putNextEntry(workerManagerStates)
                                writer.appendWorkerManagerInfo(getCurrentWorkerJobStates(context))
                                writer.flush()
                                out.closeEntry()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Export of logs has failed!")
                return IOErrorCode.APPLY_EXIF_ROTATION_ERROR.asFailure(e)
            }
            val resource = getUriResource(
                tempZipFile,
                "docscan_log_${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}" + "." + PageFileType.ZIP.extension
            )
            when (resource) {
                is Failure -> {
                    tempZipFile.safelyDelete()
                    return resource
                }
                is Success -> {
                    // ignore
                }
            }
            return resource
        }
    }

    private fun PrintWriter.appendDeviceInfo() {
        println("Version ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
        println("Manufacturer:${Build.MANUFACTURER}")
        println("Device:${Build.DEVICE} Model(${Build.MODEL}) Id(${Build.ID})")
        println("Android API Version:${Build.VERSION.SDK_INT}")
        println(
            "Internal Storage Space: ${
                getInternalSpace(context.filesDir).asHumanReadableStat(
                    context
                )
            }"
        )
    }

    /**
     * Appends the work infos for all worker jobs, for which the state is not SUCCEEDED.
     */
    private fun PrintWriter.appendWorkerManagerInfo(docScanWorkInfos: List<DocScanWorkInfo>) {
        var currentTag: String? = null
        docScanWorkInfos.forEach { docScanWorkInfo ->
            if (currentTag != docScanWorkInfo.tag) {
                println("***************************")
                println("*** ${docScanWorkInfo.tag} ***")
                println("***************************")
            }
            if (docScanWorkInfo.workInfo.state != WorkInfo.State.SUCCEEDED) {
                println("Job:${docScanWorkInfo.jobId}, WorkInfo{state=${docScanWorkInfo.workInfo.state.name}, runAttemptCount=${docScanWorkInfo.workInfo.runAttemptCount}}")
            }
            currentTag = docScanWorkInfo.tag
        }
    }

    suspend fun clearLogs() {
        logMutex.withLock {
            getLogsFolder().safelyRecursiveDelete()
        }
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
     * @return true if the device has at least [NUM_BYTES_REQUIRED_FOR_MIGRATION] bytes available.
     */
    fun hasDeviceEnoughSpaceForMigration(): Boolean {
        return isInternalSpaceAvailable(NUM_BYTES_REQUIRED_FOR_MIGRATION, getDocumentsFolder())
    }

    private fun isInternalSpaceAvailable(
        @Suppress("SameParameterValue") requiredBytes: Long,
        targetFolder: File
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val uuid = storageManager.getUuidForPath(targetFolder)
            val availableBytes = storageManager.getAllocatableBytes(uuid)
            if (availableBytes >= requiredBytes) {
                storageManager.allocateBytes(uuid, requiredBytes)
            }
            true
        } else {
            val availableBytes = targetFolder.usableSpace
            availableBytes >= requiredBytes
        }
    }

    private fun getInternalSpace(targetFolder: File): Space {
        return Space(targetFolder.totalSpace, targetFolder.usableSpace, targetFolder.freeSpace)
    }

    private data class Space(
        val totalStorageSpace: Long,
        val usableStorageSize: Long,
        val freeStorageSize: Long
    ) {
        fun asHumanReadableStat(context: Context): String {
            Formatter.formatFileSize(context, totalStorageSpace)
            return "total:${
                Formatter.formatFileSize(
                    context,
                    totalStorageSpace
                )
            }, usable:${
                Formatter.formatFileSize(
                    context,
                    usableStorageSize
                )
            }, free:${Formatter.formatFileSize(context, freeStorageSize)}"
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
                to.outputStream(context).use { output ->
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
            Timber.e(e, "Failed to retrieve the file for absolute path: $absolutePath!")
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

        return getUriResource(file, outputFileName)
    }

    private fun getUriResource(file: File, fileName: String): Resource<Uri> {
        return try {
            Success(getUri(file, fileName))
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
        Timber.e(e, "Computing hash of file: $name has failed!")
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
    ZIP("zip", "application/zip"),
    TXT("txt", "text/plain"),
    PDF("pdf", "application/pdf");

    companion object {
        fun getFileTypeByMimeType(mimeType: String): PageFileType? {
            values().forEach { fileType ->
                if (fileType.mimeType == mimeType) {
                    return fileType
                }
            }
            return null
        }
    }
}

fun File.safeExists(): Boolean {
    return try {
        exists()
    } catch (e: Exception) {
        Timber.e("exists check has failed", e)
        false
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


/**
 * Pre-Condition: [this] [Uri] has write access.
 * @return [FileOutputStream] of [Uri].
 */
@Throws(Exception::class)
fun Uri.outputStream(context: Context): OutputStream {
    return FileOutputStream(
        context.contentResolver.openFileDescriptor(
            this,
            "rw",
            null
        )!!.fileDescriptor
    )
}
