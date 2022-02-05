package at.ac.tuwien.caa.docscan.repository

import android.content.Context
import android.net.Uri
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PdfCreator
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.extensions.DocumentContractNotifier
import at.ac.tuwien.caa.docscan.extensions.asURI
import at.ac.tuwien.caa.docscan.extensions.saveFile
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
    private val preferencesHandler: PreferencesHandler,
    private val exportFileRepository: ExportFileRepository
) {

    /**
     * TODO: Add more Timber.e etc. info tags for logging this internally.
     */
    suspend fun exportDoc(documentId: UUID, exportFormat: ExportFormat): Resource<String> {
        val outputFile = fileHandler.createCacheFileForExport(UUID.randomUUID())
        try {
            val resource = exportDocInternal(documentId, exportFormat, outputFile)
            withContext(NonCancellable) {
                tearDownExport(documentId, outputFile)
            }
            return resource
        } catch (e: CancellationException) {
            Timber.i("The export job has been cancelled - tearing down export!")
            withContext(NonCancellable) {
                tearDownExport(documentId, outputFile, isCancelled = true)
            }
            throw e
        }
    }

    private suspend fun exportDocInternal(
        documentId: UUID,
        exportFormat: ExportFormat,
        outputFile: File
    ): Resource<String> {
        Timber.d("Starting export for $documentId")
        // 1. Retrieve the current document with its pages.
        val documentWithPages = documentDao.getDocumentWithPages(documentId) ?: kotlin.run {
            return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        }
        val exportDirectory = preferencesHandler.exportDirectoryUri?.asURI() ?: kotlin.run {
            return IOErrorCode.EXPORT_FILE_MISSING_PERMISSION.asFailure()
        }
        if (!PermissionHandler.isPermissionGiven(context, exportDirectory.toString())) {
            return IOErrorCode.EXPORT_FILE_MISSING_PERMISSION.asFailure()
        }
        return withContext(Dispatchers.IO) {
            val textBlocks: MutableList<Text>?
            if (exportFormat == ExportFormat.PDF_WITH_OCR) {
                textBlocks = mutableListOf()
                val deferredResults = mutableListOf<Deferred<Resource<Text>>>()
                documentWithPages.pages.forEach { page ->
                    // Only for debugging purposes
                    //delay(1000)
                    val deferredResult = async {
                        val file = fileHandler.getFileByPage(page)
                            ?: return@async DBErrorCode.DOCUMENT_PAGE_FILE_FOR_EXPORT_MISSING.asFailure()
                        val result = PdfCreator.analyzeFileWithOCR(context, Uri.fromFile(file))
                        pageDao.updateExportState(page.id, ExportState.DONE)
                        result
                    }
                    deferredResults.add(deferredResult)
                }

                deferredResults.awaitAll().forEach {
                    when (it) {
                        is Failure -> {
                            return@withContext IOErrorCode.ML_KIT_OCR_ANALYSIS_FAILED.asFailure(
                                it.exception
                            )
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
                    ?: return@withContext IOErrorCode.FILE_MISSING.asFailure()
                PdfCreator.PDFCreatorFileWrapper(pageFile, page.rotation)
            }
            when (val pdfCreatorResult =
                PdfCreator.savePDF(outputFile, filesForExport, textBlocks)) {
                is Failure -> {
                    outputFile.safelyDelete()
                    return@withContext Failure(pdfCreatorResult.exception)
                }
                is Success -> {
                    val fileName =
                        "${documentWithPages.document.title}.${PageFileType.PDF.extension}"
                    val resource = saveFile(
                        context,
                        fileHandler,
                        outputFile,
                        exportDirectory,
                        fileName,
                        PageFileType.PDF.mimeType
                    )
                    outputFile.safelyDelete()
                    when (resource) {
                        is Failure -> {
                            Timber.e("Saving export file has failed!", resource.exception)
                            return@withContext Failure(resource.exception)
                        }
                        is Success -> {
                            exportFileRepository.addFile(resource.data)
                            // inform that exported documents have changed
                            DocumentContractNotifier.observableDocumentContract.postValue(Event(Unit))
                            return@withContext Success(resource.data)
                        }
                    }
                }
            }
        }
    }

    private suspend fun tearDownExport(
        documentId: UUID,
        outputFile: File,
        isCancelled: Boolean = false
    ) {
        outputFile.safelyDelete()
        pageDao.updatePageExportStateForDocument(
            documentId,
            if (isCancelled) ExportState.NONE else ExportState.DONE
        )

        tryToUnlockDoc(documentId, null)
    }
}
