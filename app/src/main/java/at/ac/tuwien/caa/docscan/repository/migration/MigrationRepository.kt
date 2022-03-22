package at.ac.tuwien.caa.docscan.repository.migration

import android.content.Context
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.boundary.asPoint
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.migration.domain.JsonStorage
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author matejbartalsky
 */
class MigrationRepository(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val fileHandler: FileHandler,
    private val preferencesHandler: PreferencesHandler
) {

    val gson: Gson by lazy {
        GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .disableHtmlEscaping()
            .create()
    }

    /**
     * Migrates the json data along with their image references into an internal database and file
     * storage.
     *
     * In previous versions of this app, the data storage was handled by multiple json files in the
     * internal storage:
     * - "documentstorage.json" the main json file which consists of documents,
     * pages and references to the images.
     * - "documentstorage_bu.json" same as the main json file, but which has
     * been saved in the public directory, so that images can be restored.
     * - "syncstorage.json" the main json file which consists of documents,
     * pages and references to the images. This is ignored and deleted in the migration.
     *
     * The main json file is now migrated into a SQL database.
     *
     * The files (images) were saved in the public storage, however, due to android's scoped storage
     * dealing with public files got more complicated and therefore it has been decided to migrate
     * these files into the internal storage:
     * - The internal storage prevents someone else from modifying/deleting and renaming the files,
     * this is advantageous, especially when complex data is being processed.
     * - Especially with the usage of a database which holds the metdata of the file, modifying a
     * file from the outside would have bad consequences.
     * - The files can be still shared or exported.
     *
     */
    suspend fun migrateJsonDataToDatabase(context: Context): Resource<Unit> {
        if (!preferencesHandler.shouldPerformDBMigration) {
            return Success(Unit)
        }

        if (!fileHandler.hasDeviceEnoughSpaceForMigration()) {
            return IOErrorCode.NOT_ENOUGH_DISK_SPACE.asFailure()
        }

        @Suppress("Deprecation")
        val documentStorageFile = File(context.filesDir, "documentstorage.json")
        // the sync storage file which is completely omitted, since partial uploads are ignored.
        val syncStorageFile = File(context.filesDir, "syncstorage.json")
        // the documentBackupStorageFile is usually in the public storage which doesn't matter, since for new app installs it cannot be retrieved anyway.
        val documentBackupStorageFile =
            File(context.filesDir, "documentstorage_bu.json")

        // if the document storage file does not exist, then we cannot do anything about it.
        if (!documentStorageFile.safeExists()) {
            preferencesHandler.shouldPerformDBMigration = false
            return Success(Unit)
        }

        val storage: JsonStorage
        when (val storageResult = parseJsonStorage(documentStorageFile)) {
            is Failure -> {
                Timber.e(
                    storageResult.exception,
                    "Unable to parse documentStorage.json - skipping migration entirely!"
                )
                preferencesHandler.shouldPerformDBMigration = false
                return Success(Unit)
            }
            is Success -> {
                storage = storageResult.data
            }
        }

        storage.documents.forEach { jsonDocument ->

            // even if it cannot be assured 100%, it is assumed that documents have a unique title.
            val document =
                documentDao.getDocumentsByTitle(jsonDocument.title).firstOrNull() ?: kotlin.run {
                    val newDocId = UUID.randomUUID()
                    val title = jsonDocument.title
                    val isActive = storage.title == jsonDocument.title
                    val meta = jsonDocument.jsonMetaData
                    val metaData = if (meta != null) {
                        MetaData(
                            author = meta.author,
                            authority = meta.authority,
                            hierarchy = meta.hierarchy,
                            genre = meta.genre,
                            language = meta.language,
                            isProjectReadme2020 = meta.readme2020,
                            allowImagePublication = meta.readme2020Public,
                            signature = meta.signature,
                            url = meta.uri,
                            writer = meta.writer,
                            description = meta.desc
                        )
                    } else {
                        null
                    }
                    Document(
                        newDocId,
                        title,
                        filePrefix = null,
                        isActive,
                        LockState.NONE,
                        metaData
                    )
                }

            documentDao.insertDocument(document)

            jsonDocument.pages.forEachIndexed pageContinue@{ index, jsonPage ->

                var skipDeletionOfSourceFile = false
                // if the page already exists, then skip this
                val existingPage =
                    pageDao.getPageByLegacyFilePath(document.id, jsonPage.file.path)
                        .firstOrNull()
                if (existingPage != null) {
                    Timber.i("Skipping page ${existingPage.id} since it already has been created!")
                    return@pageContinue
                }

                val newPageId = UUID.randomUUID()
                fileHandler.getFileByAbsolutePath(jsonPage.file.path)?.let { oldFile ->

                    // create an internal file placeholder
                    val newFile = fileHandler.createDocumentFile(
                        document.id,
                        newPageId,
                        // every copied file is a jpeg file
                        PageFileType.JPEG
                    )

                    // 1. copy file into internal storage.
                    when (val resource = fileHandler.copyFileResource(
                        oldFile,
                        newFile
                    )) {
                        is Failure -> {
                            Timber.e(
                                resource.exception,
                                "A file copy for the migration has failed!"
                            )

                            // all of these errors should basically not occur since we have already checked the space + after every image is copied
                            // the source is deleted to free up space

                            // if an IOException should occur, then there is probably no space left or the read/write has failed, therefore
                            // it will end the migration and show an error to the user
                            if (resource.exception is IOException) {
                                return IOErrorCode.FILE_COPY_ERROR.asFailure(resource.exception)
                            } else {
                                // ignore every other error type, move on with the next page - there is probably no way to deal with this.
                                Timber.i("File ${oldFile.absolutePath} is not going to be deleted, due to an unknown error that has happened!")
                                skipDeletionOfSourceFile = true
                            }
                        }
                        is Success -> {
                            // continue if the file has been successfully copied
                            Timber.i("File ${oldFile.absolutePath} successfully copied to ${newFile.absolutePath} for document ${document.id}")
                        }
                    }

                    // 2. read out the normed cropping points.
                    val normedCropPoints =
                        PageDetector.getNormedCropPoints(newFile.absolutePath)
                    val singlePageBoundary = if (normedCropPoints.points.size == 4) {
                        SinglePageBoundary(
                            normedCropPoints.points[0].asPoint(),
                            normedCropPoints.points[1].asPoint(),
                            normedCropPoints.points[2].asPoint(),
                            normedCropPoints.points[3].asPoint()
                        )
                    } else {
                        SinglePageBoundary.getDefault()
                    }

                    // 3. read out the exif orientation
                    val rotation = getRotation(newFile)

                    // 4. read out the state if the page has been already cropped.
                    val processingState =
                        if (PageDetector.isCropped(newFile.absolutePath)) PostProcessingState.DONE else PostProcessingState.DRAFT

                    val newPage = Page(
                        newPageId,
                        document.id,
                        newFile.getFileHash(),
                        index,
                        rotation,
                        PageFileType.JPEG,
                        processingState,
                        ExportState.NONE,
                        singlePageBoundary,
                        legacyFilePath = oldFile.absolutePath
                    )

                    pageDao.insertPage(newPage)

                    // delete the public file
                    if (!skipDeletionOfSourceFile) {
                        oldFile.safelyDelete()
                    }

                } ?: run {
                    Timber.e("Ignoring page file, since not file not found!")
                }
            }
        }

        // mark as migration been performed
        preferencesHandler.shouldPerformDBMigration = false

        // safely delete all internal files
        documentStorageFile.safelyDelete()
        syncStorageFile.safelyDelete()
        documentBackupStorageFile.safelyDelete()

        File(context.filesDir, "syncinfo.txt").safelyDelete()
        File(context.filesDir, "crop_log.txt").safelyDelete()

        return Success(Unit)
    }

    private fun parseJsonStorage(documentStorageFile: File): Resource<JsonStorage> {
        try {
            documentStorageFile.bufferedReader().use {
                val storage = gson.fromJson(it, JsonStorage::class.java)
                return Success(storage)
            }
        } catch (e: Exception) {
            Timber.e(e, "Parsing JsonStorage has failed!")
            return IOErrorCode.PARSING_FAILED.asFailure(e)
        }
    }
}