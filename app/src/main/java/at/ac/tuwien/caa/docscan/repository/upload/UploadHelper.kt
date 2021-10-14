package at.ac.tuwien.caa.docscan.repository.upload

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.DataLog
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.rest.User
import at.ac.tuwien.caa.docscan.sync.SyncStorage
import at.ac.tuwien.caa.docscan.sync.SyncUtils
import at.ac.tuwien.caa.docscan.sync.UploadService
import at.ac.tuwien.caa.docscan.ui.AccountActivity
import at.ac.tuwien.caa.docscan.ui.TranskribusLoginActivity
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import com.google.android.material.snackbar.Snackbar

// TODO: Contains all utility classes for the upload of documents
//private fun uploadDocument(context: Context, document: DocumentWithPages) {
//
//    DataLog.getInstance()
//        .writeUploadLog(context, DocumentViewerActivity.TAG, "uploadDocument: ${document.document.title}")
//
////        First check if user is online
//    if (!Helper.isOnline(context)) {
////        showOfflineDialog()
//        return
//    }
//// TODO:
//////        Check if the user is logged in:
////    if (User.getInstance().isLoggedIn)
////        startUpload(document)
////    else
////        showNotLoggedInDialog()
//
//}
//
//
//
//
//
//private fun showNotLoggedInDialog() {
//
//    val dialogTitle = "${getString(R.string.viewer_not_logged_in_title)}"
//    val dialogText = "${getString(R.string.sync_not_logged_in_text)}"
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//    alertDialogBuilder
//        .setTitle(dialogTitle)
//        .setMessage(dialogText)
//        .setPositiveButton(R.string.dialog_yes_text) { _, _ ->
//            run {
//                val intent = Intent(applicationContext, AccountActivity::class.java)
//                intent.putExtra(TranskribusLoginActivity.PARENT_ACTIVITY_NAME, this.javaClass.name)
//                startActivity(intent)
//            }
//        }
//        .setNegativeButton(R.string.dialog_cancel_text, null)
//        .setCancelable(true)
//    alertDialogBuilder.create().show()
//
//}
//
//private fun startUpload(document: DocumentWithPages) {
//
//    if (document != null) {
//        DataLog.getInstance().writeUploadLog(this, DocumentViewerActivity.TAG, "startUpload document: $document")
//        DataLog.getInstance()
//            .writeUploadLog(this, DocumentViewerActivity.TAG, "startUpload document: ${document.document.title}")
//    }
//
//    var dirs = ArrayList<String>()
//    dirs.add(document.document.title)
//    startUpload(dirs)
//
//    showUploadStartedSnackbar()
//}
//
///**
// * Shows a snackbar indicating that the upload started.
// */
//private fun showUploadStartedSnackbar() {
//
//    val snackbarText = "${getString(R.string.sync_snackbar_upload_started)}"
//    val s = Snackbar.make(
//        findViewById(R.id.sync_coordinatorlayout),
//        snackbarText,
//        Snackbar.LENGTH_LONG
//    )
//    s.show()
//
//}
//
//private fun showOfflineDialog() {
//
//    val dialogTitle = "${getString(R.string.viewer_offline_title)}"
//    val dialogText = "${getString(R.string.viewer_offline_text)}"
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//    alertDialogBuilder
//        .setTitle(dialogTitle)
//        .setMessage(dialogText)
//        .setPositiveButton(R.string.dialog_ok_text, null)
//        .setCancelable(true)
//    alertDialogBuilder.create().show()
//
//}
//
//
///**
// * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
// * upload folder.
// */
//// TODO: Handle this via the viewModel
//
//private fun startUpload(uploadDirs: java.util.ArrayList<String>) {
//
//    DataLog.getInstance().writeUploadLog(this, DocumentViewerActivity.TAG, "startUpload: $uploadDirs")
//
//    SyncStorage.getInstance(this).addUploadDirs(this, uploadDirs)
//    SyncUtils.startSyncJob(this, false)
//
//}
//
//INTENT_UPLOAD_ACTION -> {
//    // TODO: Check why the selectedItemId has been sometimes switched
////                        if (supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG) != null) {
////                            supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
////                                (this as DocumentsFragment).reloadDocuments()
////                            }
////                        } else {
////                            binding.bottomNav.selectedItemId = R.id.viewer_documents
////                        }
//
//    when (intent.getStringExtra(UploadService.UPLOAD_INTEND_TYPE)) {
//        UploadService.UPLOAD_ERROR_ID -> showUploadErrorDialog()
//        UploadService.UPLOAD_OFFLINE_ERROR_ID -> showOfflineSnackbar()
//        UploadService.UPLOAD_FINISHED_ID -> showUploadFinishedSnackbar()
//        UploadService.UPLOAD_FILE_DELETED_ERROR_ID -> showFileDeletedErrorDialog()
//    }
//}
//
///**
// * Shows a snackbar indicating that the device is offline.
// */
//private fun showOfflineSnackbar() {
//
//    val snackbarText = resources.getString(R.string.sync_snackbar_offline_text)
//
//    Snackbar.make(
//        findViewById(R.id.sync_coordinatorlayout),
//        snackbarText, Snackbar.LENGTH_LONG
//    ).show()
//
//}
//
//
///**
// * Shows a snackbar indicating that the upload process starts. We need this because we have
// * little control of the time when the upload starts really.
// */
//private fun showUploadFinishedSnackbar() {
//
//    val snackbarText = resources.getString(R.string.sync_snackbar_finished_upload_text)
//
////        closeSnackbar()
//    Snackbar.make(
//        findViewById(R.id.sync_coordinatorlayout),
//        snackbarText, Snackbar.LENGTH_LONG
//    ).show()
//
//
//}
//
//private fun showUploadErrorDialog() {
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//
//    // set dialog message
//    alertDialogBuilder
//        .setTitle(R.string.sync_error_upload_title)
//        .setPositiveButton("OK", null)
//        .setMessage(R.string.sync_error_upload_text)
//
//    // create alert dialog
//    val alertDialog = alertDialogBuilder.create()
//
//    // show it
//    alertDialog.show()
//
//}
//
//private fun showFileDeletedErrorDialog() {
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//
//    // set dialog message
//    alertDialogBuilder
//        .setTitle(R.string.sync_file_deleted_title)
//        .setPositiveButton("OK", null)
//        .setMessage(R.string.sync_file_deleted_text)
//
//    // create alert dialog
//    val alertDialog = alertDialogBuilder.create()
//
//    // show it
//    alertDialog.show()
//
//}
//
//
//private fun showDocumentUploadedDialog() {
//
//    val dialogText = getString(R.string.viewer_document_uploaded_text)
//    val dialogTitle = getString(R.string.viewer_document_uploaded_title)
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//    alertDialogBuilder
//        .setTitle(dialogTitle)
//        .setMessage(dialogText)
//        .setPositiveButton(R.string.dialog_ok_text, null)
//    alertDialogBuilder.create().show()
//
//
//}
//
//fun uploadFABPressed(view: View) {
//
////        selectedDocument?.title?.let { showDocumentOptions(selectedDocument!!) }
//
//    // TODO: Handle this via the viewModel, check the states there:
////        selectedDocument?.let {
////            when {
////                it.isUploaded -> showDocumentUploadedDialog()
////                Helper.isDocumentCropped(it) -> uploadDocument(it)
////                else -> showNotCropDialog(it, true, true)
////            }
////
////        }
//
//}
