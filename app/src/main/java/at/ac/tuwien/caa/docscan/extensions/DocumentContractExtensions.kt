package at.ac.tuwien.caa.docscan.extensions

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.logic.*
import java.io.File

fun getDocumentFilesForDirectoryTree(context: Context, documentFolderUri: Uri, fileType: PageFileType): List<DocumentFile> {
    val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
            documentFolderUri,
            DocumentsContract.getTreeDocumentId(documentFolderUri)
    )
    val documents = mutableListOf<DocumentFile>()
    val cursor = context.contentResolver.query(
            uriFolder,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
    )
    if (cursor != null && cursor.moveToFirst()) {
        do {
            val pdfUri =
                    DocumentsContract.buildDocumentUriUsingTree(documentFolderUri, cursor.getString(0))
            DocumentFile.fromSingleUri(context, pdfUri).let { documentFile ->
                if (documentFile?.type == fileType.mimeType) {
                    documents.add(documentFile)
                }
            }
        } while (cursor.moveToNext())
    }

    cursor?.close()

    return documents
}

/**
 * Pre-Condition: write access for [documentFolderUri]
 * Saves [newFile] into [documentFolderUri]
 */
fun saveFile(context: Context, fileHandler: FileHandler, newFile: File, documentFolderUri: Uri, displayName: String, mimeType: String): Resource<Unit> {
    val uriFolder = DocumentsContract.buildDocumentUriUsingTree(
            documentFolderUri,
            DocumentsContract.getTreeDocumentId(documentFolderUri)
    )

    val createdDocumentUri = DocumentsContract.createDocument(
            context.contentResolver,
            uriFolder,
            mimeType,
            displayName) ?: return IOErrorCode.CREATE_EXPORT_DOCUMENT_FAILED.asFailure()

    return try {
        fileHandler.copyFileToUri(newFile, createdDocumentUri)
        Success(Unit)
    } catch (e: Exception) {
        IOErrorCode.FILE_COPY_ERROR.asFailure()
    }
}

fun deleteFile(file: DocumentFile) {
    file.delete()
}
