package at.ac.tuwien.caa.docscan.repository.migration

import android.content.Context
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.MetaData
import at.ac.tuwien.caa.docscan.db.model.Page
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
     */
    suspend fun migrateJsonDataToDatabase(context: Context) {
        // TODO: 1. get access to the internal json file and parse it
        // TODO: Use the filehandler instead
        if (!preferencesHandler.shouldPerformDBMigration) {
            return
        }
        val documentStorageFile = File(context.filesDir, DocumentStorage.DOCUMENT_STORE_FILE_NAME)
        if (documentStorageFile.exists()) {
            try {
                val reader = documentStorageFile.bufferedReader()
                val storage = gson.fromJson(reader, JsonStorage::class.java)
                reader.safeClose()


                // TODO: Not enough storage issue: The user might have a large number of photos, i.e. copying the entire files to their new destination
                // TODO: And deleting it just afterwards might not be the best option, this maybe needs to be done partially.

                // TODO: If the files have been previously on an external device (SD card) then this might be necessary check
                // TODO: otherwise, the migration could fail because of missing storage on the internal device storage.

                // TODO: drop the entire documents and tables since if someone has closed the app during the migration, this might cause problems.

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
                                    fileHandler.createDocumentFile(newDocId, newPageId, FileType.JPEG)
                                )
                                newPages.add(Page(newPageId, newDocId, index))
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
                                metaData
                            ),
                            newPages
                        )
                    )
                }

                // TODO: Store into database




                // mark as migration been performed
                preferencesHandler.shouldPerformDBMigration = false
                // If copying the files was successful, drop the public files
                storage.documents.flatMap { document ->
                    document.pages
                }.forEach { page ->
                    fileHandler.getFileByAbsolutePath(page.file.path)?.safelyDelete()
                }

                // TODO: delete documents file and mark it in the preferences
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