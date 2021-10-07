package at.ac.tuwien.caa.docscan.repository

import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import androidx.room.withTransaction
import at.ac.tuwien.caa.docscan.camera.ImageExifMetaData
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
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
    private val db: AppDatabase
) {

    fun getPageByIdAsFlow(pageId: UUID) = documentDao.getPageAsFlow(pageId)

    fun getPageById(pageId: UUID) = documentDao.getPage(pageId)

    fun getDocumentWithPagesAsFlow(documentId: UUID) = documentDao.getDocumentWithPagesAsFlow(documentId)

    suspend fun getDocumentWithPages(documentId: UUID) = documentDao.getDocumentWithPages(documentId)

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

        val newPage = Page(id = newFileId, docId = document.id, number = pageNumber)

        // 4. Update file in database (create or update)
        db.withTransaction {
            // update document
            documentDao.insertDocument(document)
            // insert the new page
            pageDao.insertPage(newPage)
        }

        return Success(data = newPage)
    }

    private fun applyExifData(file: File, exifMetaData: ImageExifMetaData) {
        try {
            val exif = ExifInterface(file)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                exifMetaData.exifOrientation.toString()
            )
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, exifMetaData.exifSoftware)
            exifMetaData.exifArtist?.let {
                exif.setAttribute(ExifInterface.TAG_ARTIST, it)
            }
            exifMetaData.exifCopyRight?.let {
                exif.setAttribute(ExifInterface.TAG_COPYRIGHT, it)
            }
            exifMetaData.location?.let {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, it.gpsLat)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, it.gpsLatRef)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, it.gpsLon)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, it.gpsLonRef)
            }
            exifMetaData.resolution?.let {
                exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, it.x)
                exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, it.y)
            }
            exif.saveAttributes()
        } catch (exception: Exception) {
            Timber.e(exception, "Couldn't save exif attributes!")
        }
    }

}