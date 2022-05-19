package at.ac.tuwien.caa.docscan.repository

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import androidx.work.WorkManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.ExportFileDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.extensions.DocumentContractNotifier
import at.ac.tuwien.caa.docscan.extensions.checksGoogleAPIAvailability
import at.ac.tuwien.caa.docscan.extensions.deleteFile
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.worker.ExportWorker
import at.ac.tuwien.caa.docscan.worker.UploadWorker
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

class DocumentRepository(
    private val context: Context,
    private val preferencesHandler: PreferencesHandler,
    private val fileHandler: FileHandler,
    private val pageDao: PageDao,
    private val documentDao: DocumentDao,
    private val exportFileDao: ExportFileDao,
    private val db: AppDatabase,
    private val imageProcessorRepository: ImageProcessorRepository,
    private val workManager: WorkManager,
    private val userRepository: UserRepository
) {

    fun getPageByIdAsFlow(pageId: UUID) = documentDao.getPageAsFlow(pageId)

    fun getPageById(pageId: UUID) = documentDao.getPage(pageId)

    fun getDocumentWithPagesAsFlow(documentId: UUID) =
        documentDao.getDocumentWithPagesAsFlow(documentId).sortByNumber()

    suspend fun getDocumentWithPages(documentId: UUID) =
        documentDao.getDocumentWithPages(documentId)?.sortByNumber()

    @WorkerThread
    suspend fun getDocumentResource(documentId: UUID): Resource<Document> {
        val document = documentDao.getDocument(documentId)
            ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        return Success(document)
    }

    fun getAllDocumentsAsFlow() = documentDao.getAllDocumentWithPagesAsFlow()

    fun getActiveDocumentAsFlow(): Flow<DocumentWithPages?> {
        return documentDao.getActiveDocumentasFlow().sortByNumber()
    }

    /**
     * Tries to sanitize documents and their states in order, basically all locked documents and
     * pages are trying to be resolved:
     * - Documents/Pages for which the upload has been already ongoing or scheduled, the worker job
     * is retried again, in this case the document is not unlocked.
     * - If the Document/Pages have been exporting previously, then the export job has been interrupted
     * which cannot be retried, therefore, the export state is reset and possible exported files
     * are also deleted.
     * - If the document/pages are locked due to any other occurrence, then they are simply unlocked.
     */
    @WorkerThread
    suspend fun sanitizeDocuments(): Resource<Unit> {
        Timber.i("Starting sanitizeDocuments...")
        documentDao.getAllLockedDocumentWithPages().forEach {
            if (it.document.lockState == LockState.PARTIAL_LOCK) {
                it.pages.forEach { page ->
                    if (page.isProcessing()) {
                        Timber.i("Page ${page.id} is reset to draft state.")
                        pageDao.updatePageProcessingState(page.id, PostProcessingState.DRAFT)
                    }
                    tryToUnlockDoc(it.document.id, page.id)
                }
            } else if (it.document.lockState == LockState.FULL_LOCK) {
                when {
                    it.isUploadInProgress() || it.isUploadScheduled() -> {
                        UploadWorker.spawnUploadJob(
                            workManager,
                            it.document.id,
                            preferencesHandler.isUploadOnMeteredNetworksAllowed
                        )
                    }
                    it.isExporting() -> {
                        // exporting operation are usually very fast, if this should ever occur
                        // then the doc is set to not exported.
                        pageDao.updatePageExportStateForDocument(it.document.id, ExportState.NONE)

                        // get all export files that are being processed for the current docId,
                        // the docId is not unique at this place as they might be several exported
                        // files for a single document.
                        exportFileDao.getProcessingExportFiles(it.document.id)
                            .forEach { exportFile ->
                                // try to delete the file that has been associated with the export file.
                                exportFile.fileUri?.let { documentUri ->
                                    deleteFile(context, documentUri)
                                }
                                // delete the export file from the database - if the corresponding file
                                // might still exist, then this will get correctly refreshed as soon as
                                // the user visits the export fragment.
                                exportFileDao.deleteExportFileByFileName(exportFile.fileName)

                                // notify the UI as well to refresh in case the user quickly navigated
                                // to the export fragments.
                                DocumentContractNotifier.observableDocumentContract.postValue(
                                    Event(
                                        Unit
                                    )
                                )
                            }
                        tryToUnlockDoc(it.document.id, null)
                    }
                    else -> {
                        tryToUnlockDoc(it.document.id, null)
                    }
                }
            }
        }

        // they might be still some export files around that are still in processing state
        // this call will update them all to false.
        exportFileDao.updatesAllProcessingStatesToFalse()
        return Success(Unit)
    }

    @WorkerThread
    suspend fun deletePages(pages: List<Page>): Resource<Unit> {
        Timber.i("delete pages, n=${pages.size} for doc ${pages.firstOrNull()?.docId}")
        // TODO: OPTIMIZATION: When adding/removing pages, add a generic check to adapt the page number correctly.
        pages.forEach {
            val result = performPageOperation(it.docId, it.id, operation = { _, page ->
                documentDao.deletePage(page)
                fileHandler.getFileByPage(it)?.safelyDelete()
                return@performPageOperation Success(Unit)
            })
            when (result) {
                is Failure -> {
                    return Failure(result.exception)
                }
                is Success -> {
                    // ignore - continue loop
                }
            }
        }
        // every time a doc is modified, the upload state has to be reset.
        pages.firstOrNull()?.let {
            clearUploadStateFor(it.docId)
        }
        return Success(Unit)
    }

    @WorkerThread
    suspend fun deletePage(page: Page?): Resource<Unit> {
        Timber.i("Delete page ${page?.id}")
        page ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        return deletePages(listOf(page))
    }

    @WorkerThread
    fun setDocumentAsActive(documentId: UUID) {
        db.runInTransaction {
            documentDao.setAllDocumentsInactive()
            documentDao.setDocumentActive(documentId = documentId)
        }
    }

    @WorkerThread
    suspend fun checkPageLock(page: Page?): Resource<Page> {
        page ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        return isPageLocked(page.docId, page.id)
    }

    @WorkerThread
    suspend fun removeDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        Timber.i("delete doc ${documentWithPages.document.id}")
        return performDocOperation(documentWithPages.document.id, operation = { doc ->
            fileHandler.deleteEntireDocumentFolder(doc.id)
            pageDao.deletePages(documentWithPages.pages)
            documentDao.deleteDocument(documentWithPages.document)
            Success(Unit)
        })
    }

    @WorkerThread
    suspend fun shareDocument(documentId: UUID): Resource<List<Uri>> {
        Timber.i("share doc ${documentId}")
        val doc = documentDao.getDocumentWithPages(documentId)
            ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        when (val checkLockResult = checkLock(doc.document, null)) {
            is Failure -> {
                return Failure(checkLockResult.exception)
            }
            is Success -> {
                val urisResults = doc.pages.map {
                    val uriResource = fileHandler.getUriByPageResource(
                        it,
                        doc.document.getFileName(it.index + 1, it.fileType)
                    )
                    when (uriResource) {
                        is Failure -> {
                            return Failure(uriResource.exception)
                        }
                        is Success -> {
                            uriResource.data
                        }
                    }
                }
                return Success(urisResults)
            }
        }
    }

    @WorkerThread
    suspend fun cancelDocumentUpload(documentId: UUID): Resource<Unit> {
        Timber.i("cancel upload for document: $documentId")
        val docWithPages = documentDao.getDocumentWithPages(documentId)
        // non-null checks are not performed here, this is for safety reasons so that the upload worker
        // is always cancelled, despite the fact if the document does not exist anymore.
        if (docWithPages?.isUploaded() == false) {
            // at this point, we do not know if the worker is currently just scheduled or running
            // if if it's just scheduled, than we need to repair the states first.
            UploadWorker.cancelWorkByDocumentId(workManager, documentId)
            clearUploadStateFor(documentId)
        }
        tryToUnlockDoc(documentId, null)
        return Success(Unit)
    }

    private suspend fun clearUploadStateFor(documentId: UUID) {
        // clear the uploadId from the doc
        documentDao.updateUploadIdForDoc(documentId, null)
        // clear the upload file names
        pageDao.clearDocumentPagesUploadFileNames(documentId)
        // update upload state to none
        pageDao.updateUploadStateForDocument(documentId, UploadState.NONE)
    }

    /**
     * Cancels all uploads in the state [UploadState.SCHEDULED] and [UploadState.UPLOAD_IN_PROGRESS]
     */
    @WorkerThread
    suspend fun cancelAllPendingUploads(): Resource<Boolean> {
        Timber.i("cancel all pending uploads")
        var cancellationsPerformed = false
        pageDao.getAllDocIdsWithPendingUploadState().forEach {
            cancelDocumentUpload(it)
            cancellationsPerformed = true
        }
        return Success(cancellationsPerformed)
    }

    @WorkerThread
    suspend fun uploadDocument(
        documentWithPages: DocumentWithPages,
        skipAlreadyUploadedRestriction: Boolean = false,
        skipCropRestriction: Boolean = false
    ): Resource<Unit> {
        Timber.i("init upload for doc ${documentWithPages.document.id}")
        val docWithPages = documentDao.getDocumentWithPages(documentWithPages.document.id)
            ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()

        if (docWithPages.pages.isEmpty()
        ) {
            return DBErrorCode.NO_IMAGES_TO_UPLOAD.asFailure()
        }

        if (!skipAlreadyUploadedRestriction && docWithPages.isUploaded()
        ) {
            return DBErrorCode.DOCUMENT_ALREADY_UPLOADED.asFailure()
        }

        // if forced, then reset upload state to NONE
        pageDao.updateUploadStateForDocument(
            documentWithPages.document.id,
            UploadState.NONE
        )

        if (!skipCropRestriction && documentDao.getDocumentWithPages(documentWithPages.document.id)
                ?.isCropped() != true
        ) {
            return DBErrorCode.DOCUMENT_NOT_CROPPED.asFailure()
        }

        when (val result = userRepository.checkLogin()) {
            is Failure -> {
                return Failure(result.exception)
            }
            is Success -> {
                // ignore - since user is logged in and therefore the upload can be started.
            }
        }

        when (val result = lockDocForLongRunningOperation(documentWithPages.document.id)) {
            is Failure -> {
                // TODO: Add reason why the doc is locked
                return Failure(result.exception)
            }
            is Success -> {
                // ignore, and perform check
            }
        }
        spawnUploadJob(documentWithPages.document.id)
        return Success(Unit)
    }

    /**
     * Pre-Condition:
     * - document is already locked
     */
    private fun spawnUploadJob(documentId: UUID) {
        pageDao.updateUploadStateForDocument(
            documentId,
            UploadState.SCHEDULED
        )
        UploadWorker.spawnUploadJob(
            workManager,
            documentId,
            preferencesHandler.isUploadOnMeteredNetworksAllowed
        )
    }

    @WorkerThread
    suspend fun exportDocument(
        documentWithPages: DocumentWithPages,
        skipCropRestriction: Boolean = false,
        exportFormat: ExportFormat
    ): Resource<Unit> {
        Timber.i("export doc ${documentWithPages.document.id} with format ${exportFormat.name}")
        if (!PermissionHandler.isPermissionGiven(context, preferencesHandler.exportDirectoryUri)) {
            return IOErrorCode.EXPORT_FILE_MISSING_PERMISSION.asFailure()
        }

        if (exportFormat == ExportFormat.PDF_WITH_OCR && !checksGoogleAPIAvailability(context)) {
            return IOErrorCode.EXPORT_GOOGLE_PLAYSTORE_NOT_INSTALLED_FOR_OCR.asFailure()
        }

        if (!skipCropRestriction && documentDao.getDocumentWithPages(documentWithPages.document.id)
                ?.isCropped() != true
        ) {
            return DBErrorCode.DOCUMENT_NOT_CROPPED.asFailure()
        }

        when (val result = lockDocForLongRunningOperation(documentWithPages.document.id)) {
            is Failure -> {
                return Failure(result.exception)
            }
            is Success -> {
                // ignore, and perform check
            }
        }
        pageDao.updatePageExportStateForDocument(
            documentWithPages.document.id,
            ExportState.EXPORTING
        )
        ExportWorker.spawnExportJob(workManager, documentWithPages.document.id, exportFormat)
        return Success(Unit)
    }

    @WorkerThread
    suspend fun cropDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        Timber.i("crop doc ${documentWithPages.document.id}")
        return when (val result = lockDocForLongRunningOperation(documentWithPages.document.id)) {
            is Failure -> {
                Failure(result.exception)
            }
            is Success -> {
                imageProcessorRepository.cropDocument(documentWithPages.document)
                // every time a doc is modified, the upload state has to be reset.
                clearUploadStateFor(documentWithPages.document.id)
                Success(Unit)
            }
        }
    }

    @WorkerThread
    suspend fun rotatePagesBy90(pages: List<Page>): Resource<Unit> {
        val firstPage = pages.firstOrNull() ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        return performDocOperation(firstPage.docId, operation = {
            withContext(NonCancellable) {
                imageProcessorRepository.rotatePages90CW(pages)
                // every time a doc is modified, the upload state has to be reset.
                pages.firstOrNull()?.let {
                    clearUploadStateFor(it.docId)
                }
            }
            Success(Unit)
        })
    }

    @WorkerThread
    fun createNewActiveDocument(): Document {
        Timber.d("creating new active document!")
        val doc = Document(
            id = UUID.randomUUID(),
            context.getString(R.string.document_default_name),
            isActive = true
        )
        documentDao.insertDocument(doc)
        return doc
    }

    @WorkerThread
    suspend fun createDocument(document: Document): Resource<Document> {
        Timber.i("create doc ${document.id}")
        return createOrUpdateDocument(document)
    }

    @WorkerThread
    suspend fun updateDocument(document: Document): Resource<Document> {
        Timber.i("update doc ${document.id}")
        return performDocOperation(document.id, operation = {
            createOrUpdateDocument(document)
        })
    }

    @WorkerThread
    private suspend fun createOrUpdateDocument(document: Document): Resource<Document> {
        val docsByNewTitle = documentDao.getDocumentsByTitle(documentTitle = document.title)
        return if (docsByNewTitle.isEmpty() || (docsByNewTitle.size == 1 && docsByNewTitle[0].id == document.id)) {
            db.runInTransaction {
                if (document.isActive) {
                    documentDao.setAllDocumentsInactive()
                }
                documentDao.insertDocument(document)
            }
            Success(document)
        } else {
            DBErrorCode.DUPLICATE.asFailure()
        }
    }

    /**
     * Currently used only for debugging purposes.
     */
    @WorkerThread
    suspend fun saveNewImportedImageForDocument(
        document: Document,
        uris: List<Uri>
    ): Resource<Unit> {
        return withContext(NonCancellable) {
            uris.forEach { uri ->
                try {
                    fileHandler.readBytes(uri)?.let { bytes ->
                        val resource = saveNewImageForDocument(
                            document.id,
                            bytes,
                            null,
                            null
                        )
                        when (resource) {
                            is Failure -> {
                                return@withContext Failure<Unit>(resource.exception)
                            }
                            is Success -> {
                                // ignore
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            return@withContext Success(Unit)
        }
    }

    @WorkerThread
    suspend fun saveNewImageForDocument(
        documentId: UUID,
        data: ByteArray,
        fileId: UUID? = null,
        exifMetaData: ImageExifMetaData?
    ): Resource<Page> {
        Timber.d("save new image for doc $documentId")
        val document = documentDao.getDocument(documentId) ?: kotlin.run {
            return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        }
        if (document.lockState == LockState.FULL_LOCK) {
            return DBErrorCode.DOCUMENT_LOCKED.asFailure()
        }
        // TODO: Make a check here, if there is enough storage to save the file.
        // if fileId is provided, then it means that a file is being replaced.
        val newFileId = fileId ?: UUID.randomUUID()
        val file = fileHandler.createDocumentFile(documentId, newFileId, PageFileType.JPEG)

        var tempFile: File? = null
        // 1. make a safe copy of the current file if it exists to rollback changes in case of something fails.
        if (file.exists()) {
            tempFile = fileHandler.createCacheFile(newFileId, PageFileType.JPEG)
            try {
                fileHandler.copyFile(file, tempFile)
            } catch (e: Exception) {
                tempFile.safelyDelete()
                if (fileId == null) {
                    file.safelyDelete()
                }
                return IOErrorCode.FILE_COPY_ERROR.asFailure(e)
            }
        }

        // 2. process byte array into file
        try {
            fileHandler.copyByteArray(data, file)
            // in case we used the temp file, delete it safely
            tempFile?.safelyDelete()
        } catch (e: Exception) {
            // rollback
            tempFile?.let {
                fileHandler.safelyCopyFile(it, file)
            }
            return IOErrorCode.FILE_COPY_ERROR.asFailure(e)
        }

        // TODO: Apply exif only here, not in the viewModels
        // 3. apply external exif data if available, otherwise assume that exif is already set.
        val rotation = if (exifMetaData != null) {
            applyExifData(file, exifMetaData)
            Rotation.getRotationByExif(exifMetaData.exifOrientation)
        } else {
            getRotation(file)
        }

        // TODO: When adding/removing pages, add a generic check to adapt the page number correctly.
        // for a replacement, just take the number of the old page.
        // for a new page, take the max number and add + 1 to it.
        val pageNumber =
            pageDao.getPageById(newFileId)?.index ?: (pageDao.getPagesByDoc(documentId)
                .maxByOrNull { page -> page.index })?.index?.let {
                    // increment if there is an existing page
                    it + 1
                } ?: 0

        val newPage = Page(
            newFileId,
            document.id,
            file.getFileHash(),
            pageNumber,
            rotation,
            PageFileType.JPEG,
            PostProcessingState.DRAFT,
            ExportState.NONE,
            SinglePageBoundary.getDefault()
        )

        // 4. Update file in database (create or update)
        db.withTransaction {
            // update document
            documentDao.insertDocument(document)
            // insert the new page
            pageDao.insertPage(newPage)
        }

        // 5. Add a partial lock and spawn page detection
        lockDoc(document.id, newPage.id)
        imageProcessorRepository.spawnPageDetection(newPage)

        return Success(data = newPage)
    }
}
