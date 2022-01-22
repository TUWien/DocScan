package at.ac.tuwien.caa.docscan.extensions

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.DocumentsContract
import androidx.core.database.getIntOrNull
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.logic.*
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.io.File

object DocumentContractNotifier {
    val observableDocumentContract = MutableLiveData<Event<Unit>>()
}

fun getDocumentFilesForDirectoryTree(
    context: Context,
    documentFolderUri: Uri,
    fileType: PageFileType
): List<DocumentFileWrapper> {
    val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
        documentFolderUri,
        DocumentsContract.getTreeDocumentId(documentFolderUri)
    )
    val documents = mutableListOf<DocumentFileWrapper>()
    val cursor = context.contentResolver.query(
        uriFolder,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        // selectionClauses and sortOrder are ignored for childs, see https://stackoverflow.com/questions/56263620/contentresolver-query-on-documentcontract-lists-all-files-disregarding-selection?rq=1
        null,
        null,
        null
    )
    if (cursor != null && cursor.moveToFirst()) {
        do {
            val documentId = cursor.getString(0)
            val lastModified = cursor.getLong(1)
            val displayName = cursor.getString(2)
            val sizeInBytes = cursor.getIntOrNull(3) ?: 0
            val mimeType = cursor.getString(4)
            val pdfUri =
                DocumentsContract.buildDocumentUriUsingTree(documentFolderUri, documentId)

            // ignore file if it doesn't match the mimetype
            if (mimeType != fileType.mimeType) {
                continue
            }

            DocumentFile.fromSingleUri(context, pdfUri)?.let { documentFile ->
                documents.add(
                    DocumentFileWrapper(
                        documentFile,
                        documentId,
                        lastModified,
                        displayName,
                        sizeInBytes
                    )
                )
            }
        } while (cursor.moveToNext())
    }

    cursor?.close()

    return documents
}

/**
 * A simple wrapper that wraps [DocumentFile] with its cached attributes. This is necessary, since
 * using the cached properties from the search result is much more efficient.
 */
@Parcelize
data class DocumentFileWrapper(
    val documentFile: @RawValue DocumentFile,
    val documentId: String,
    val lastModified: Long,
    val displayName: String,
    val sizeInBytes: Int
) : Parcelable

/**
 * Pre-Condition: write access for [documentFolderUri]
 * Saves [newFile] into [documentFolderUri]
 */
fun saveFile(
    context: Context,
    fileHandler: FileHandler,
    newFile: File,
    documentFolderUri: Uri,
    displayName: String,
    mimeType: String
): Resource<String> {
    val uriFolder = DocumentsContract.buildDocumentUriUsingTree(
        documentFolderUri,
        DocumentsContract.getTreeDocumentId(documentFolderUri)
    )

    val fileUri: Uri
    try {
        fileUri = DocumentsContract.createDocument(
            context.contentResolver,
            uriFolder,
            mimeType,
            displayName
        ) ?: return IOErrorCode.EXPORT_CREATE_URI_DOCUMENT_FAILED.asFailure()
    } catch (e: Exception) {
        return IOErrorCode.EXPORT_CREATE_URI_DOCUMENT_FAILED.asFailure(e)
    }

    // check the display name of the inserted file, this might not be our desired name if the same
    // name already exists in the folder (test.pdf -> test(1).pdf), therefore
    val outputFileName = DocumentFile.fromSingleUri(context, fileUri)?.name ?: displayName

    return try {
        fileHandler.copyFileToUri(newFile, fileUri)
        Success(outputFileName)
    } catch (e: Exception) {
        // delete the previously created file if the copy fails
        DocumentFile.fromSingleUri(context, fileUri)?.delete()
        IOErrorCode.FILE_COPY_ERROR.asFailure()
    }
}

fun deleteFile(file: DocumentFile) {
    file.delete()
}
