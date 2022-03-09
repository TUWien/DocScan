package at.ac.tuwien.caa.docscan.repository

import android.content.Context
import android.net.Uri
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.export.PdfCreator
import at.ac.tuwien.caa.docscan.export.ZipCreator
import at.ac.tuwien.caa.docscan.extensions.DocumentContractNotifier
import at.ac.tuwien.caa.docscan.extensions.asURI
import at.ac.tuwien.caa.docscan.extensions.createFile
import at.ac.tuwien.caa.docscan.extensions.deleteFile
import at.ac.tuwien.caa.docscan.logic.*
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import timber.log.Timber
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

    suspend fun exportDoc(documentId: UUID, exportFormat: ExportFormat): Resource<String> {
        try {
            val resource = exportDocInternal(documentId, exportFormat)
            withContext(NonCancellable) {
                tearDownExport(documentId)
            }
            return resource
        } catch (e: CancellationException) {
            Timber.i("The export job has been cancelled - tearing down export!")
            withContext(NonCancellable) {
                tearDownExport(documentId, isCancelled = true)
            }
            throw e
        }
    }

    private suspend fun exportDocInternal(
        documentId: UUID,
        exportFormat: ExportFormat
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

        val outputResource = createFile(
            context,
            exportDirectory,
            exportFormat.getFileType().mimeType,
            "${documentWithPages.document.title}.${exportFormat.getFileType().extension}"
        )
        val output: Pair<Uri, String>
        when (outputResource) {
            is Failure -> {
                return Failure(outputResource.exception)
            }
            is Success -> {
                output = outputResource.data
            }
        }

        // inform that exported documents have changed
        exportFileRepository.insertOrUpdateFile(output.second, true)
        DocumentContractNotifier.observableDocumentContract.postValue(Event(Unit))

        return withContext(Dispatchers.IO) {

            val filesForExport = documentWithPages.pages.map { page ->
                val pageFile = fileHandler.getFileByPage(page)
                    ?: return@withContext IOErrorCode.FILE_MISSING.asFailure()
                PdfCreator.FileWrapper(pageFile, page.rotation)
            }

            val exportResource: Resource<Unit>
            try {
                when (exportFormat) {
                    ExportFormat.ZIP -> {
                        exportResource =
                            ZipCreator.saveAsZip(
                                context, output.first, documentWithPages,
                                fileHandler
                            )
                    }
                    ExportFormat.PDF, ExportFormat.PDF_WITH_OCR -> {
                        val textBlocks: List<Text>? =
                            if (exportFormat == ExportFormat.PDF_WITH_OCR) {
                                when (val result = ocr(documentWithPages)) {
                                    is Failure -> {
                                        return@withContext Failure(result.exception)
                                    }
                                    is Success -> {
                                        result.data
                                    }
                                }
                            } else {
                                null
                            }
                        exportResource =
                            PdfCreator.savePDF(context, output.first, filesForExport, textBlocks)
                    }
                }
            } catch (e: CancellationException) {
                // remove the exported file
                deleteFile(context, output.first)
                throw CancellationException()
            }

            when (exportResource) {
                is Failure -> {
                    Timber.e("Saving export file has failed!", exportResource.exception)
                    exportFileRepository.removeFile(output.second)
                    return@withContext Failure(exportResource.exception)
                }
                is Success -> {
                    // inform that exported documents have changed
                    exportFileRepository.insertOrUpdateFile(output.second, false)
                    DocumentContractNotifier.observableDocumentContract.postValue(Event(Unit))
                    return@withContext Success(output.second)
                }
            }
        }
    }

    private suspend fun ocr(documentWithPages: DocumentWithPages): Resource<List<Text>> {
        return withContext(Dispatchers.IO) {
            val textBlocks = mutableListOf<Text>()
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

            return@withContext Success(textBlocks)
        }
    }

    private suspend fun tearDownExport(
        documentId: UUID,
        isCancelled: Boolean = false
    ) {
        pageDao.updatePageExportStateForDocument(
            documentId,
            if (isCancelled) ExportState.NONE else ExportState.DONE
        )

        tryToUnlockDoc(documentId, null)
    }
}
