package at.ac.tuwien.caa.docscan.repository.migration

import android.content.Context
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.boundary.asPoint
import at.ac.tuwien.caa.docscan.db.model.state.ExportState
import at.ac.tuwien.caa.docscan.db.model.state.LockState
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.repository.migration.domain.JsonStorage
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.File
import java.util.*

/**
 * @author matejbartalsky
 */
class MigrationRepository(
        private val docRepo: DocumentRepository,
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
     * TODO: ERROR_HANDLING (1) - Files need to be copied & deleted one by one, otherwise storage issues might occur, if the space is already very limited.
     * TODO: ERROR_HANDLING (2) - If the files have been previously on an external device (SD card), then even if (2) is considered, storage issues might occur.
     * TODO: ERROR_HANDLING (3) - To mitigate (1) and (2), add a more sophisticated migration strategy, e.g. by asking the user if the data should even be migrated.
     * TODO: MIGRATION_LOGIC - If the app is killed during migration and if (1) is considered, then we may loose some data, the name of the files in the public folder are probably necessary for this purpose.
     */
    suspend fun migrateJsonDataToDatabase(context: Context) {
        if (!preferencesHandler.shouldPerformDBMigration) {
            return
        }
        @Suppress("Deprecation")
        val documentStorageFile = File(context.filesDir, DocumentStorage.DOCUMENT_STORE_FILE_NAME)
        if (documentStorageFile.exists()) {
            try {
                val reader = documentStorageFile.bufferedReader()
                val storage = gson.fromJson(reader, JsonStorage::class.java)
                reader.safeClose()

                val newDocsWithPages = mutableListOf<DocumentWithPages>()
                storage.documents.forEach { jsonDocument ->
                    val newDocId = UUID.randomUUID()
                    val title = jsonDocument.title
                    val isActive = storage.title == jsonDocument.title
                    val meta = jsonDocument.jsonMetaData
                    val metaData = if (meta != null) {
                        MetaData(
                                author = meta.author,
                                authority = meta.authority,
                                genre = meta.genre,
                                language = meta.language,
                                isProjectReadme2020 = meta.readme2020,
                                allowImagePublication = meta.readme2020Public,
                                signature = meta.signature,
                                url = meta.uri,
                                writer = meta.writer
                        )
                    } else {
                        null
                    }

                    val newPages = mutableListOf<Page>()
                    jsonDocument.pages.forEachIndexed { index, jsonPage ->
                        val newPageId = UUID.randomUUID()
                        fileHandler.getFileByAbsolutePath(jsonPage.file.path)?.let {
                            try {
                                fileHandler.copyFile(
                                        it,
                                        // we assume that all file types are jpeg files
                                        fileHandler.createDocumentFile(
                                                newDocId,
                                                newPageId,
                                                PageFileType.JPEG
                                        )
                                )
                                val result = PageDetector.getNormedCropPoints(it.absolutePath)
                                // read out the old
                                val singlePageBoundary = if (result.points.size == 4) {
                                    SinglePageBoundary(
                                            result.points[0].asPoint(),
                                            result.points[1].asPoint(),
                                            result.points[2].asPoint(),
                                            result.points[3].asPoint()
                                    )
                                } else {
                                    SinglePageBoundary.getDefault()
                                }
                                // read out old orientation
                                val rotation = Helper.getNewSafeExifOrientation(it)

                                // if cropping has been already performed, then this will be marked as done
                                val processingState =
                                        if (PageDetector.isCropped(it.absolutePath)) PostProcessingState.DONE else PostProcessingState.DRAFT

                                newPages.add(
                                        Page(
                                                newPageId,
                                                newDocId,
                                                it.getFileHash(),
                                                index,
                                                rotation,
                                                PageFileType.JPEG,
                                                processingState,
                                                ExportState.NONE,
                                                singlePageBoundary
                                        )
                                )
                            } catch (exception: Exception) {
                                // TODO: Log copying has failed!
                                // TODO: If the copying fails due to not enough storage, this should be prevented before.
                            }
                        }
                    }
                    newDocsWithPages.add(
                            DocumentWithPages(
                                    Document(
                                            newDocId,
                                            title,
                                            isActive,
                                            LockState.NONE,
                                            metaData
                                    ),
                                    newPages
                            )
                    )
                }

                // mark as migration been performed
                preferencesHandler.shouldPerformDBMigration = false
                // If copying the files was successful, drop the public files
                storage.documents.flatMap { document ->
                    document.pages
                }.forEach { page ->
                    fileHandler.getFileByAbsolutePath(page.file.path)?.safelyDelete()
                }

                documentStorageFile.safelyDelete()

            } catch (exception: Exception) {
                // T
            }
        }
    }


    /**
     * Safely closes a [BufferedReader]
     */
    private fun BufferedReader.safeClose() {
        try {
            close()
        } catch (exception: Exception) {
            // ignore
        }
    }
}