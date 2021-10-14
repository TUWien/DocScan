package at.ac.tuwien.caa.docscan.repository.upload

import android.content.Intent
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.logic.KtHelper
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import at.ac.tuwien.caa.docscan.ui.docviewer.SelectPdfDocumentActivity
import com.google.android.material.snackbar.Snackbar

// TODO: All utility functions for the PDF export

//
//private fun showOCRAlertDialog(document: DocumentWithPages) {
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//    // set dialog message
//    alertDialogBuilder
//        .setTitle(R.string.gallery_confirm_ocr_title)
//        .setPositiveButton(R.string.dialog_yes_text) { dialog, which ->
//            createPdf(document, true)
////                    deselectListViewItems()
//        }
//        .setNegativeButton(R.string.dialog_no_text) { dialogInterface, i ->
//            createPdf(document, false)
////                    deselectListViewItems()
//        }
//        .setCancelable(true)
//        .setMessage(R.string.gallery_confirm_ocr_text)
//
//    val alertDialog = alertDialogBuilder.create()
//    alertDialog.setButton(
//        AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text)
//    ) { dialog, which -> alertDialog.cancel() }
//    alertDialog.show()
//
//}
//
//private fun showNotCropDialog(
//    document: DocumentWithPages,
//    upload: Boolean,
//    extendedInfo: Boolean = false
//) {
//
//    val proceed =
//        if (upload) getString(R.string.viewer_not_cropped_upload)
//        else getString(R.string.viewer_not_cropped_pdf)
//
//    val text: String
//    if (extendedInfo)
//        text = getString(R.string.viewer_images_fragment_not_cropped_confirm_text)
//    else
//        text = getString(R.string.viewer_not_cropped_confirm_text)
//
////        val text = getString(R.string.viewer_not_cropped_confirm_text)
//    val cropText = "$text $proceed?"
//    val cropTitle = "${getString(R.string.viewer_not_cropped_confirm_title)}"
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//    alertDialogBuilder
//        .setTitle(cropTitle)
//        .setMessage(cropText)
//        .setPositiveButton(R.string.dialog_yes_text) { _, _ ->
//            run {
//                if (upload)
//                    uploadDocument(document)
//                else
//                    showOCRAlertDialog(document)
//            }
//        }
//        .setNegativeButton(R.string.dialog_cancel_text, null)
//    alertDialogBuilder.create().show()
//
//}
//
//private fun showDocumentDirSetDialog() {
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//
//    // set dialog message
//    alertDialogBuilder
//        .setTitle(R.string.viewer_document_dir_set_title)
//        .setPositiveButton(R.string.dialog_ok_text) { _, _ -> }
//        .setCancelable(true)
//        .setMessage(R.string.viewer_document_dir_set_text)
//    alertDialogBuilder.create().show()
//
//}
//
//
//private fun createPdf(document: DocumentWithPages, withOCR: Boolean) {
//
//    // TODO: Relocate creation of documents with new domain into the viewModel
////        if (withOCR)
////            createPdfWithOCR(document)
////        else
////            createPdf(document)
//
//}
//
//private fun showNoPlayServicesDialog(document: DocumentWithPages) {
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//
//    // set dialog message
//    alertDialogBuilder
//        .setTitle(R.string.gallery_confirm_no_ocr_available_title)
//        .setPositiveButton(R.string.dialog_yes_text) { dialog, which ->
//            ImageProcessor.createPdf(document, false)
////                    deselectListViewItems()
//        }
//        .setNegativeButton(R.string.dialog_no_text) { dialogInterface, i -> }
//        .setCancelable(true)
//        .setMessage(R.string.gallery_confirm_no_ocr_available_text)
//    alertDialogBuilder.create().show()
//
//}
//
//@RequiresApi(Build.VERSION_CODES.N)
//private fun grantPdfDirAccess() {
//
//    val intent = KtHelper.getOpenDocumentDirIntent(this)
//    startActivityForResult(intent, DocumentViewerActivity.PERSISTABLE_URI_PERMISSION)
//
//}
//
//@RequiresApi(Build.VERSION_CODES.N)
//private fun showDirectoryPermissionRequiredAlert() {
//
//    val title = getString(R.string.viewer_document_dir_permission_title)
//    val text = getString(R.string.viewer_document_dir_permission_text)
//
////        val text = getString(R.string.pdf_fragment_persisted_permission_text)
////        val title = getString(R.string.pdf_fragment_persisted_permission_title)
//
//    val alertDialogBuilder = AlertDialog.Builder(this)
//        .setTitle(title)
//        .setMessage(text)
//        .setPositiveButton(R.string.dialog_ok_text) { _, _ ->
//            grantPdfDirAccess()
//        }
//        .setNegativeButton(R.string.dialog_cancel_text, null)
//        .setCancelable(true)
//    alertDialogBuilder.create().show()
//
//}
//
////    OCR functionality
//fun showPdfOcrDialog(document: DocumentWithPages) {
//
//    //        Check if the play services are installed first:
//    if (!Helper.checkPlayServices(this))
//        showNoPlayServicesDialog(document)
//    else
//        showOCRAlertDialog(document)
//
//}
//
//
//private fun showPdfCreatedSnackbar(title: String) {
//
//    val snackbarText = "${getString(R.string.viewer_pdf_created_snackbar_text)}: $title"
//    val s = Snackbar.make(
//        findViewById(R.id.sync_coordinatorlayout),
//        snackbarText,
//        Snackbar.LENGTH_LONG
//    )
//    s.show()
//
//}