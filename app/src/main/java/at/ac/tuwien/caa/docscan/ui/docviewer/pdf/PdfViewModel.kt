package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.extensions.asURI
import at.ac.tuwien.caa.docscan.extensions.getDocumentFilesForDirectoryTree
import at.ac.tuwien.caa.docscan.extensions.getExportFolderPermissionIntent
import at.ac.tuwien.caa.docscan.logic.PageFileType
import at.ac.tuwien.caa.docscan.logic.PermissionHandler
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

class PdfViewModel(val app: DocScanApp, val preferencesHandler: PreferencesHandler) : ViewModel() {

    val observableExportModel: MutableLiveData<ExportModel> = MutableLiveData()

    fun load(uri: Uri? = null) {
        val folderUri = uri ?: preferencesHandler.exportDirectoryUri?.asURI()
        if (folderUri != null && PermissionHandler.isPermissionGiven(app, folderUri.toString())) {
            val list = mutableListOf<ExportList>()
//            val header = ExportList.ExportHeader(folderUri.path ?: "Unknown path!")
            val files = getDocumentFilesForDirectoryTree(app, folderUri, PageFileType.PDF).map { file ->
                ExportList.File(file, pageFileType = PageFileType.PDF, file.name ?: "Unknown name!")
            }
//            list.add(header)
            list.addAll(files)
            observableExportModel.postValue(ExportModel.Success(list))
        } else {
            observableExportModel.postValue(ExportModel.MissingPermissions())
        }
    }

    /**
     * Persists [uri] and releases old folder uri for exports.
     */
    fun persistFolderUri(uri: Uri) {
        app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        // release the old permissions only if they have changed.
        preferencesHandler.exportDirectoryUri?.asURI()?.let {
            if (it != uri) {
                app.contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
        // save the new permissions
        preferencesHandler.exportDirectoryUri = uri.toString()
        load(uri)
    }

    fun deleteFile(file: ExportList.File) {
        at.ac.tuwien.caa.docscan.extensions.deleteFile(file.documentFile)
        load()
    }

    fun launchFolderSelection(folderPermissionResultCallback: ActivityResultLauncher<Intent>) {
        folderPermissionResultCallback.launch(getExportFolderPermissionIntent(app, preferencesHandler.exportDirectoryUri?.asURI()))
    }
}

sealed class ExportModel {
    class MissingPermissions : ExportModel()
    class Success(val exportEntries: List<ExportList>) : ExportModel()
}


sealed class ExportList {
    data class ExportHeader(val directory: String) : ExportList()

    @Parcelize
    data class File(val documentFile: @RawValue DocumentFile, val pageFileType: PageFileType, val name: String) : ExportList(), Parcelable
}
