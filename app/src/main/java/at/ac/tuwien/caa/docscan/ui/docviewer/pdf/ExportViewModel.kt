package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.extensions.DocumentFileWrapper
import at.ac.tuwien.caa.docscan.extensions.asURI
import at.ac.tuwien.caa.docscan.extensions.getDocumentFilesForDirectoryTree
import at.ac.tuwien.caa.docscan.extensions.getExportFolderPermissionIntent
import at.ac.tuwien.caa.docscan.logic.Event
import at.ac.tuwien.caa.docscan.logic.PageFileType
import at.ac.tuwien.caa.docscan.logic.PermissionHandler
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.repository.ExportFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class ExportViewModel(
    private val app: DocScanApp,
    private val preferencesHandler: PreferencesHandler,
    private val exportFileRepository: ExportFileRepository
) : ViewModel() {

    val observableExportModel: MutableLiveData<ExportModel> = MutableLiveData()
    val observableOpenFile: MutableLiveData<Event<ExportList.File>> = MutableLiveData()

    fun load(uri: Uri? = null, scrollToTop: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val folderUri = uri ?: preferencesHandler.exportDirectoryUri?.asURI()
            if (folderUri != null && PermissionHandler.isPermissionGiven(
                    app,
                    folderUri.toString()
                )
            ) {
                val systemMilliSeconds = System.currentTimeMillis()
                Timber.d("DOCUMENTS")
                val list = mutableListOf<ExportList>()
//            val header = ExportList.ExportHeader(folderUri.path ?: "Unknown path!")
                val files = exportFileRepository.checkFileNames(
                    getDocumentFilesForDirectoryTree(
                        app,
                        folderUri,
                        listOf(PageFileType.PDF, PageFileType.ZIP)
                    ).map { file ->
                        ExportList.File(
                            file,
                            pageFileType = file.fileType,
                            file.displayName
                        )
                    })
                Timber.d("DOCUMENTS - ${System.currentTimeMillis() - systemMilliSeconds}")
//            list.add(header)
                list.addAll(files.sortedByDescending { file -> file.file.lastModified })
                Timber.d("DOCUMENTS - ${System.currentTimeMillis() - systemMilliSeconds}")

                observableExportModel.postValue(ExportModel.Success(list, scrollToTop))
            } else {
                observableExportModel.postValue(ExportModel.MissingPermissions())
            }
        }
    }

    /**
     * Persists [uri] and releases old folder uri for exports.
     */
    fun persistFolderUri(uri: Uri) {
        app.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        // release the old permissions only if they have changed.
        preferencesHandler.exportDirectoryUri?.asURI()?.let {
            if (it != uri) {
                viewModelScope.launch(Dispatchers.IO) {
                    exportFileRepository.removeAll()
                }
                try {
                    app.contentResolver.releasePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (exception: Exception) {
                    // this can be an expected exception if the uri does not point to a valid source anymore.
                    Timber.w("Releasing persistable uri has failed", exception)
                }
            }
        }
        // save the new permissions
        preferencesHandler.exportDirectoryUri = uri.toString()
        load(uri)
    }

    fun deleteFile(file: ExportList.File) {
        at.ac.tuwien.caa.docscan.extensions.deleteFile(file.file.documentFile)
        load()
    }

    fun openFile(file: ExportList.File) {
        viewModelScope.launch(Dispatchers.IO) {
            exportFileRepository.removeFile(file.name)
            observableOpenFile.postValue(Event(file))
        }
    }

    fun launchFolderSelection(folderPermissionResultCallback: ActivityResultLauncher<Intent>) {
        folderPermissionResultCallback.launch(
            getExportFolderPermissionIntent(
                app,
                preferencesHandler.exportDirectoryUri?.asURI()
            )
        )
    }
}

sealed class ExportModel {
    class MissingPermissions : ExportModel()
    class Success(val exportEntries: List<ExportList>, val scrollToTop: Boolean) : ExportModel()
}


sealed class ExportList {
    data class ExportHeader(val directory: String) : ExportList()

    @Parcelize
    data class File(
        val file: DocumentFileWrapper,
        val pageFileType: PageFileType,
        val name: String,
        var state: ExportState = ExportState.ALREADY_OPENED
    ) : ExportList(), Parcelable
}

enum class ExportState {
    EXPORTING,
    NEW,
    ALREADY_OPENED
}
