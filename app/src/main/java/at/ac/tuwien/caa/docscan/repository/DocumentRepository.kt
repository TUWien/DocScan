package at.ac.tuwien.caa.docscan.repository

import androidx.annotation.WorkerThread
import androidx.room.withTransaction
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.exception.DBDocumentDuplicate
import at.ac.tuwien.caa.docscan.db.exception.DBError
import at.ac.tuwien.caa.docscan.db.exception.DBException
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

class DocumentRepository(
    private val fileHandler: FileHandler,
    private val pageDao: PageDao,
    private val documentDao: DocumentDao,
    private val db: AppDatabase,
    private val imageProcessorRepository: ImageProcessorRepository
) {

    fun getPageByIdAsFlow(pageId: UUID) = documentDao.getPageAsFlow(pageId)

    fun getPageById(pageId: UUID) = documentDao.getPage(pageId)

    fun getDocumentWithPagesAsFlow(documentId: UUID) =
        documentDao.getDocumentWithPagesAsFlow(documentId)

    suspend fun getDocumentWithPages(documentId: UUID) =
        documentDao.getDocumentWithPages(documentId)

    @WorkerThread
    suspend fun getDocument(documentId: UUID) = documentDao.getDocument(documentId)

    fun getAllDocuments() = documentDao.getAllDocumentWithPages()

    fun getActiveDocumentAsFlow() = documentDao.getActiveDocumentasFlow()

    fun getActiveDocument() = documentDao.getActiveDocument()

    @WorkerThread
    suspend fun deletePage(page: Page) {
        withContext(NonCancellable) {
            documentDao.deletePage(page)
            fileHandler.getFileByPage(page)?.safelyDelete()
        }
    }

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
        // TODO: Run constraints check
        return Failure(DBDocumentDuplicate())
    }

    @WorkerThread
    suspend fun processDocument(documentWithPages: DocumentWithPages): Resource<Unit> {
        // TODO: Run constraints check
        return Failure(DBDocumentDuplicate())
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

    @WorkerThread
    suspend fun saveNewImageForActiveDocument(
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

        // determine the new page number, either take the same number if there was an replacement
        // or increment the page number of the last page in the document.
        // TODO: The numbers do not work correctly.
        val pageNumber =
            (pageDao.getPageById(newFileId) ?: pageDao.getPagesByDoc(newFileId)
                .maxByOrNull { page -> page.number })?.number?.let {
                // increment if there is an existing page
                it + 1
            } ?: 0

        val newPage = Page(
            newFileId,
            document.id,
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