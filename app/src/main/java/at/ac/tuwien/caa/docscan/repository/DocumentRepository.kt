package at.ac.tuwien.caa.docscan.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import androidx.work.*
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.exception.DBDocumentDuplicate
import at.ac.tuwien.caa.docscan.db.exception.DBDocumentLocked
import at.ac.tuwien.caa.docscan.db.exception.DBDuplicateAction
import at.ac.tuwien.caa.docscan.db.exception.DocumentLockStatus
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
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
import java.util.concurrent.TimeUnit

class DocumentRepository(
    private val fileHandler: FileHandler,
    private val pageDao: PageDao,
    private val documentDao: DocumentDao,
    private val db: AppDatabase,
    private val imageProcessorRepository: ImageProcessorRepository,
    private val workManager: WorkManager
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

    // TODO: Check constraints
    @WorkerThread
    suspend fun deletePages(pages: List<Page>) {
        withContext(NonCancellable) {
            documentDao.deletePages(pages)
            pages.forEach {
                fileHandler.getFileByPage(it)?.safelyDelete()
            }
        }
    }

    // TODO: Check constraints
    @WorkerThread
    suspend fun deletePage(page: Page) {
        withContext(NonCancellable) {
            documentDao.deletePage(page)
            fileHandler.getFileByPage(page)?.safelyDelete()
        }
    }

    // TODO: Check constraints
    @WorkerThread
    suspend fun updatePage(page: Page) {
        withContext(NonCancellable) {
            pageDao.insertPage(page)
        }
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
        withContext(NonCancellable) {
            db.runInTransaction {
                fileHandler.deleteEntireDocumentFolder(documentWithPages.document.id)
                pageDao.deletePages(documentWithPages.pages)
                documentDao.deleteDocument(documentWithPages.document)
            }
        }
        return Success(Unit)
    }

    @WorkerThread
    suspend fun uploadDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        val documentWithPagesFresh =
            documentDao.getDocumentWithPages(documentId = documentWithPages.document.id)
                // TODO: wrong error
                ?: return Failure(
                    DBDuplicateAction()
                )
        if (documentWithPagesFresh.isLocked()) {
            // TODO: The failure is wrong here, this needs to be determined!
            return Failure(DBDocumentLocked(DocumentLockStatus.UPLOAD))
        } else {
            // TODO: Check some preliminary conditions, like if the user is authenticated etc. before doing this.
            pageDao.updateUploadStateForDocument(
                documentWithPagesFresh.document.id,
                UploadState.UPLOAD_IN_PROGRESS
            )
        }
        val tag = "transkribus_image_uploader"
        val uploadPhotosWorkRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
            .addTag(tag)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // the backoff criteria is set to quite a short time, since a retry is only performed if there is an
            // internet connection, i.e. if the user should be offline, the work is not retried due to the connected
            // constraint, but as soon as the user gets online, this will take only 5seconds to upload.
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                5,
                TimeUnit.SECONDS
            )
            .build()

        Timber.i("Requesting WorkManager to queue UploadWorker")
        // the existing policy needs to be always KEEP, otherwise it may be possible that multiple work requests
        // are started, e.g. REPLACE would cancel work, but if this function is called a lot of times, cancelled
        // work would still be ongoing, therefore we need to ensure that this is set to KEEP, so that only one request
        // is performed at a time.
        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, uploadPhotosWorkRequest)
        return Success(Unit)
    }

    @WorkerThread
    suspend fun processDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        // TODO: check pre-conditions & Run constraints check
        imageProcessorRepository.cropDocument(documentWithPages.document)
        return Success(Unit)
    }

    @WorkerThread
    suspend fun rotatePagesBy90(pages: List<Page>): Resource<Unit> {
        imageProcessorRepository.rotatePages90CW(pages)
        return Success(Unit)
    }

    @WorkerThread
    suspend fun exportDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        // TODO: Run constraints check
        return Failure(DBDocumentDuplicate())
    }

    @WorkerThread
    fun createNewActiveDocument(): Document {
        Timber.d("creating new active document!")
        // TODO: What should be the document's title in the default case?
        val doc = Document(
            id = UUID.randomUUID(),
            "Untitled document",
            true,
            null
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
            Failure(DBDocumentDuplicate())
        }
    }

    /**
     * TODO: This is currently just for debugging purposes, there are no checks and the implmentation
     * TODO: can be improved
     */
    @WorkerThread
    suspend fun saveNewImportedImageForDocument(
        document: Document,
        uris: List<Uri>
    ) {
        withContext(NonCancellable) {
            uris.forEach { uri ->
                try {
                    fileHandler.readBytes(uri)?.let { bytes ->
                        saveNewImageForDocument(
                            document,
                            bytes,
                            null,
                            ImageExifMetaData(
                                // TODO: this is wrong and needs to be read out from the file
                                Rotation.ORIENTATION_NORMAL.exifOrientation,
                                "test",
                                null,
                                null,
                                null,
                                null
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    @WorkerThread
    suspend fun saveNewImageForDocument(
        document: Document,
        data: ByteArray,
        fileId: UUID? = null,
        exifMetaData: ImageExifMetaData
    ): Resource<Page> {
        Timber.d("Starting to save new image for document: ${document.title}")
        // TODO: Make a check here, if there is enough storage to save the file.
        // if fileId is provided, then it means that a file is being replaced.
        val newFileId = fileId ?: UUID.randomUUID()
        val documentId = document.id
        val file = fileHandler.createDocumentFile(documentId, newFileId, FileType.JPEG)

        var tempFile: File? = null
        // 1. make a safe copy of the current file if it exists to rollback changes in case of something fails.
        if (file.exists()) {
            tempFile = fileHandler.createCacheFile(newFileId, FileType.JPEG)
            try {
                fileHandler.copyFile(file, tempFile)
            } catch (e: Exception) {
                tempFile.safelyDelete()
                return Failure(Exception("TODO: Add correct error here!"))
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
            return Failure(Exception("TODO: Add correct error here!"))
        }

        // 3. apply exif meta data to file
        applyExifData(file, exifMetaData)

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
            Rotation.getRotationByExif(exifMetaData.exifOrientation),
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

        // 5. Spawn the page detection task
        imageProcessorRepository.spawnPageDetection(newPage)

        return Success(data = newPage)
    }
}
