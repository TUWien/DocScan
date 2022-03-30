package at.ac.tuwien.caa.docscan.export

import android.content.Context
import android.net.Uri
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.getFileName
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.asFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipCreator {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun saveAsZip(
        context: Context,
        outputUri: Uri,
        documentWithPages: DocumentWithPages,
        fileHandler: FileHandler,
    ): Resource<Unit> {
        context.contentResolver.openOutputStream(outputUri, "rw").use { outputStream ->
            return withContext(Dispatchers.IO) {
                try {
                    outputStream ?: throw IllegalStateException("Outputstream is null!")
                    outputStream.buffered().use { buffered ->
                        ZipOutputStream(buffered).use { out ->
                            out.bufferedWriter().use {
                                for ((index, page) in documentWithPages.pages.sortedBy { page -> page.index }
                                    .withIndex()) {
                                    val file = fileHandler.getFileByPage(page)
                                        ?: throw IllegalStateException("file is null!")
                                    val entry = ZipEntry(
                                        documentWithPages.document.getFileName(
                                            index + 1,
                                            page.fileType
                                        )
                                    )
                                    out.putNextEntry(entry)
                                    file.inputStream().use { input ->
                                        input.copyTo(out)
                                    }
                                    out.closeEntry()
                                }
                                if (!isActive) {
                                    throw CancellationException()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // if this has happened because of a cancellation, then this needs to be re-thrown
                    if (e is CancellationException) {
                        throw e
                    } else {
                        Timber.e(e, "Export of ZIP has failed!")
                        return@withContext IOErrorCode.EXPORT_CREATE_ZIP_FAILED.asFailure(e)
                    }
                }
                return@withContext Success(Unit)
            }
        }
    }
}
