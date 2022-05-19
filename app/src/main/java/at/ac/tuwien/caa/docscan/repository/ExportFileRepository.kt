package at.ac.tuwien.caa.docscan.repository

import android.net.Uri
import at.ac.tuwien.caa.docscan.db.dao.ExportFileDao
import at.ac.tuwien.caa.docscan.db.model.ExportFile
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.ExportList
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.ExportState
import java.util.*

class ExportFileRepository(
    private val exportFileDao: ExportFileDao
) {
    suspend fun checkFileNames(files: List<ExportList.File>): List<ExportList.File> {
        // 1. remove all files in the DB if they cannot be found anymore.
        val notFoundFiles = exportFileDao.getExportFiles()
            .filter { exportFile -> files.firstOrNull { file -> file.name == exportFile.fileName } == null }
        exportFileDao.delete(notFoundFiles)

        // 2. loop through each requested file and check if its name is in the DB.
        files.forEach { file ->
            val exportFile = exportFileDao.getExportFileByFileName(file.name).firstOrNull()
            val exportState: ExportState = when {
                exportFile == null -> {
                    ExportState.ALREADY_OPENED
                }
                exportFile.isProcessing -> {
                    ExportState.EXPORTING
                }
                else -> {
                    ExportState.NEW
                }
            }
            file.state = exportState
        }
        return files
    }

    suspend fun insertOrUpdateFile(
        docId: UUID,
        fileName: String,
        fileUri: Uri?,
        isProcessing: Boolean
    ) {
        exportFileDao.insertExportFile(
            ExportFile(
                fileName = fileName,
                docId = docId,
                fileUri = fileUri,
                isProcessing = isProcessing
            )
        )
    }

    suspend fun removeAll() {
        exportFileDao.deleteAll()
    }

    suspend fun removeFile(fileName: String) {
        exportFileDao.deleteExportFileByFileName(fileName)
    }

    fun getExportFileCount() = exportFileDao.getExportFileCount()
}
