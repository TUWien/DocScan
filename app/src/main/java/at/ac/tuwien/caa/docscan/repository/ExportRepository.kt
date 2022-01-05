package at.ac.tuwien.caa.docscan.repository

import android.content.Context
import android.net.Uri
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PdfCreator
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.logic.*
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class ExportRepository(
        private val context: Context,
        private val documentDao: DocumentDao,
        private val pageDao: PageDao,
        private val fileHandler: FileHandler,
        private val preferencesHandler: PreferencesHandler
) {

    suspend fun exportDoc(documentId: UUID, exportFormat: ExportFormat): Resource<Unit> {
        val outputFile = fileHandler.createCacheFileForExport(UUID.randomUUID())
        try {
            return exportDocInternal(documentId, exportFormat, outputFile)
        } catch (e: CancellationException) {
            Timber.i("The export job has been cancelled - tearing down export!")
            withContext(NonCancellable) {
                tearDownExport(documentId, outputFile, isCancelled = true)
            }
            throw e
        }
    }

    private suspend fun exportDocInternal(documentId: UUID, exportFormat: ExportFormat, outputFile: File): Resource<Unit> {
        Timber.d("Starting export for $documentId")
        // 1. Retrieve the current document with its pages.
        val documentWithPages = documentDao.getDocumentWithPages(documentId) ?: kotlin.run {
            return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        }
        withContext(Dispatchers.IO) {

            val textBlocks: MutableList<Text>?
            if (exportFormat == ExportFormat.PDF_WITH_OCR) {
                textBlocks = mutableListOf()
                val deferredResults = mutableListOf<Deferred<Resource<Text>>>()
                documentWithPages.pages.forEach { page ->
                    val deferredResult = async {
                        val file = fileHandler.getFileByPage(page)
                                ?: return@async DBErrorCode.DOCUMENT_PAGE_FILE_FOR_EXPORT_MISSING.asFailure()
                        PdfCreator.analyzeFileWithOCR(context, Uri.fromFile(file))
                        // TODO: Add new page
                    }
                    deferredResults.add(deferredResult)
                }

                deferredResults.awaitAll().forEach {
                    when (it) {
                        is Failure -> {
                            return@withContext IOErrorCode.ML_KIT_OCR_ANALYSIS_FAILED.asFailure<Unit>(it.exception)
                        }
                        is Success -> {
                            textBlocks.add(it.data)
                        }
                    }
                }
            } else {
                textBlocks = null
            }

            val filesForExport = documentWithPages.pages.map { page ->
                val pageFile = fileHandler.getFileByPage(page)
                        ?: return@withContext IOErrorCode.FILE_MISSING
                PdfCreator.PDFCreatorFileWrapper(pageFile, page.rotation)
            }
            when (val pdfCreatorResult = PdfCreator.savePDF(outputFile, filesForExport, textBlocks)) {
                is Failure -> {
                    outputFile.safelyDelete()
                    return@withContext Failure<Unit>(pdfCreatorResult.exception)
                }
                is Success -> {
                    // TODO: Check write permissions of the uri folder
                    // TODO: Copy file to target output folder!
                    outputFile.safelyDelete()
                }
            }
        }
        tearDownExport(documentId, outputFile)
        return Success(Unit)
    }

    private suspend fun tearDownExport(documentId: UUID, outputFile: File, isCancelled: Boolean = false) {
        outputFile.safelyDelete()
        pageDao.updatePageExportStateForDocument(
                documentId,
                if (isCancelled) ExportState.NONE else ExportState.DONE
        )

        tryToUnlockDoc(documentId, null)
    }
}
