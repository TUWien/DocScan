package at.ac.tuwien.caa.docscan.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import androidx.work.*
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.sync.UploadWorker
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

class DocumentRepository(
    private val fileHandler: FileHandler,
    private val pageDao: PageDao,
    private val documentDao: DocumentDao,
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
    suspend fun getDocument(documentId: UUID) = documentDao.getDocument(documentId)

    fun getAllDocuments() = documentDao.getAllDocumentWithPagesAsFlow()

    fun getActiveDocumentAsFlow(): Flow<DocumentWithPages?> {
        return documentDao.getActiveDocumentasFlow().sortByNumber()
    }

    fun getActiveDocument() = documentDao.getActiveDocument()

    @WorkerThread
    suspend fun deletePages(pages: List<Page>) {
        pages.forEach {
            performPageOperation(it.docId, it.id, operation = { doc, page ->
                documentDao.deletePage(page)
                fileHandler.getFileByPage(it)?.safelyDelete()
                return@performPageOperation Success(Unit)
            })
        }
    }

    @WorkerThread
    suspend fun deletePage(page: Page) {
        deletePages(listOf(page))
    }

    @WorkerThread
    suspend fun setDocumentAsActive(documentId: UUID) {
        db.runInTransaction {
            documentDao.setAllDocumentsInactive()
            documentDao.setDocumentActive(documentId = documentId)
        }
    }

    @WorkerThread
    suspend fun removeDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        return performDocOperation(documentWithPages.document.id, operation = { doc ->
            fileHandler.deleteEntireDocumentFolder(doc.id)
            pageDao.deletePages(documentWithPages.pages)
            documentDao.deleteDocument(documentWithPages.document)
            Success(Unit)
        })
    }

    @WorkerThread
    suspend fun uploadDocument(
        documentWithPages: DocumentWithPages,
        forceUpload: Boolean = false
    ): Resource<Unit> {
        if (documentDao.getDocumentWithPages(documentWithPages.document.id)?.isUploaded() == true) {
            // if forced, then reset upload state to NONE
            if (forceUpload) {
                pageDao.updateUploadStateForDocument(
                    documentWithPages.document.id,
                    UploadState.NONE
                )
            } else {
                return DBErrorCode.DOCUMENT_ALREADY_UPLOADED.asFailure()
            }
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
        pageDao.updateUploadStateForDocument(
            documentWithPages.document.id,
            UploadState.UPLOAD_IN_PROGRESS
        )
        UploadWorker.spawnUploadJob(workManager, documentWithPages.document.id)
        return Success(Unit)
    }

    @WorkerThread
    suspend fun processDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        return when (val result = lockDocForLongRunningOperation(documentWithPages.document.id)) {
            is Failure -> {
                Failure(result.exception)
            }
            is Success -> {
                imageProcessorRepository.cropDocument(documentWithPages.document)
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
            }
            Success(Unit)
        })
    }

    @WorkerThread
    suspend fun exportDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        return when (val result = lockDocForLongRunningOperation(documentWithPages.document.id)) {
            is Failure -> {
                Failure(result.exception)
            }
            is Success -> {
                // TODO: Spawn export job
                Success(Unit)
            }
        }
    }

    @WorkerThread
    fun createNewActiveDocument(): Document {
        Timber.d("creating new active document!")
        // TODO: What should be the document's title in the default case?
        val doc = Document(
            id = UUID.randomUUID(),
            "Untitled document",
            true
        )
        documentDao.insertDocument(doc)
        return doc
    }

    @WorkerThread
    fun createOrUpdateDocument(document: Document): Resource<Document> {
        val docByNewTitle = documentDao.getDocumentByTitle(documentTitle = document.title)
        return if (docByNewTitle == null || docByNewTitle.id == document.id) {
            db.runInTransaction {
                //TODO: Check this, this should make it only active if it'S a new one
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
        return performDocOperation(document.id, operation = {
            withContext(NonCancellable) {
                uris.forEach { uri ->
                    try {
                        fileHandler.readBytes(uri)?.let { bytes ->
                            saveNewImageForDocument(
                                document,
                                bytes,
                                null,
                                null
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
            Success(Unit)
        })
    }

    @WorkerThread
    suspend fun saveNewImageForDocument(
        document: Document,
        data: ByteArray,
        fileId: UUID? = null,
        exifMetaData: ImageExifMetaData?
    ): Resource<Page> {
        Timber.d("Starting to save new image for document: ${document.title}")
        // TODO: Make a check here, if there is enough storage to save the file.
        // if fileId is provided, then it means that a file is being replaced.
        val newFileId = fileId ?: UUID.randomUUID()
        val documentId = document.id
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
            pageDao.getPageById(newFileId)?.number ?: (pageDao.getPagesByDoc(documentId)
                .maxByOrNull { page -> page.number })?.number?.let {
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
