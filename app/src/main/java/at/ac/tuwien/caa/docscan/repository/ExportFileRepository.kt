package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.dao.ExportFileDao
import at.ac.tuwien.caa.docscan.db.model.ExportFile
import at.ac.tuwien.caa.docscan.ui.docviewer.pdf.ExportList

class ExportFileRepository(
    private val exportFileDao: ExportFileDao
) {
    suspend fun checkFileNames(files: List<ExportList.File>): List<ExportList.File> {
        // 1. remove all files in the DB if they cannot be found anymore.
        val notFoundFiles = exportFileDao.getExportFiles()
            .filter { exportFile -> files.firstOrNull { file -> file.name == exportFile.fileName } == null }
        exportFileDao.delete(notFoundFiles)

        // 2. loop through each requested file and check if it's name is in the DB.
        files.forEach { file ->
            val exportFile = exportFileDao.getExportFileByFileName(file.name).firstOrNull()
            file.isNew = exportFile != null
        }
        return files
    }

    suspend fun addFile(fileName: String) {
        val file = exportFileDao.getExportFileByFileName(fileName).firstOrNull()
        if (file == null) {
            exportFileDao.insertExportFile(ExportFile(fileName = fileName))
        }
    }

    suspend fun removeFile(fileName: String) {
        exportFileDao.deleteExportFileByFileName(fileName)
    }

    fun getExportFileCount() = exportFileDao.getExportFileCount()
}
