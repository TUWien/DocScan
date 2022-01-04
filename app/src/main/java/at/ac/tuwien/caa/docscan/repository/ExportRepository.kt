package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PdfCreator
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.logic.ExportFormat
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class ExportRepository(
        private val documentDao: DocumentDao,
        private val pageDao: PageDao,
        private val fileHandler: FileHandler
) {

    suspend fun exportDoc(documentId: UUID, exportFormat: ExportFormat): Resource<Unit> {
        try {
            return exportDocInternal(documentId, exportFormat)
        } catch (e: CancellationException) {
            Timber.i("The export job has been cancelled - tearing down export!")
            withContext(NonCancellable) {
                tearDownExport(documentId, isCancelled = true)
            }
            throw e
        }
    }

    private fun exportDocInternal(documentId: UUID, exportFormat: ExportFormat): Resource<Unit> {
//        when (exportFormat) {
//            ExportFormat.PDF -> PdfCreator.createPdfWithoutOCR(pdfName, files, this, context)
//            ExportFormat.PDF_WITH_OCR -> PdfCreator.createPdfWithOCR(pdfName, files, this, context)
//            ExportFormat.ZIP -> {
//
//            }
//        }
        tearDownExport(documentId)
        return Success(Unit)
    }

    private fun tearDownExport(documentId: UUID, isCancelled: Boolean = false) {
        pageDao.updatePageExportStateForDocument(
                documentId,
                if (isCancelled) ExportState.NONE else ExportState.DONE
        )
    }
}