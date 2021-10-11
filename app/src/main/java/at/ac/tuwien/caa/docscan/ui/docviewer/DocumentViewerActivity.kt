package at.ac.tuwien.caa.docscan.ui.docviewer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global.getString
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ActionSheet
import at.ac.tuwien.caa.docscan.camera.DocumentActionSheet
import at.ac.tuwien.caa.docscan.camera.PdfActionSheet
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.*
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PdfCreator.PDF_FILE_NAME
import at.ac.tuwien.caa.docscan.databinding.ActivityDocumentViewerBinding
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.isCropped
import at.ac.tuwien.caa.docscan.db.model.isuploaded
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.rest.User
import at.ac.tuwien.caa.docscan.sync.SyncStorage
import at.ac.tuwien.caa.docscan.sync.SyncUtils
import at.ac.tuwien.caa.docscan.sync.UploadService.*
import at.ac.tuwien.caa.docscan.ui.AccountActivity
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer
import at.ac.tuwien.caa.docscan.ui.TranskribusLoginActivity.PARENT_ACTIVITY_NAME
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.inject
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


/**
 * Partly based on this tutorial:
 * https://pspdfkit.com/blog/2019/using-the-bottom-navigation-view-in-android/
 */
// TODO: Use the navigation graph with viewModel support for each fragment to simplify the communication
// TODO: This gets otherwise very complicated.
// TODO: Relocate stuff to their fragment, where it belongs to.
class DocumentViewerActivity : BaseNavigationActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener,
        ImagesAdapter.ImagesAdapterCallback,
        ActionSheet.SheetSelection,
        ActionSheet.DocumentSheetSelection,
        ActionSheet.PdfSheetSelection,
        ActionSheet.DialogStatus,
        SelectableToolbar.SelectableToolbarCallback, PdfFragment.PdfListener {

    private val fileHandler by inject<FileHandler>(FileHandler::class.java)
    private lateinit var binding: ActivityDocumentViewerBinding
    private val viewModel: DocumentViewerViewModel by viewModel()

    private val galleryResultCallback =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    it.data?.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            fileHandler.saveFile(clipData.getItemAt(i).uri)
                        }
                    } ?: run {
                        it.data?.data?.let { uriFile ->
                            fileHandler.saveFile(uriFile)
                        }
                    }
                    // refresh the UI after the new images have been imported.
                    openDocumentsView()
                }
            }

    override fun onScrollImageLoaded() {
        startPostponedEnterTransition()
    }

    override fun onImageLoaded() {
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            startPostponedEnterTransition()
//        }

    }

    companion object {
        val TAG = "DocumentViewerActivity"

        // TODO: Is not necessary, because it's backed up by flows
        const val DOCUMENT_RENAMING_REQUEST = 0

        // TODO: Is not necessary, due to flows.
        const val LAUNCH_VIEWER_REQUEST = 1

        const val DOCUMENT_PDF_SELECTION_REQUEST = 2
        const val PERSISTABLE_URI_PERMISSION = 3

        private const val EXTRA_DOCUMENT_PAGE = "EXTRA_DOCUMENT_PAGE"
        private const val EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW = "EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW"

        init {
//         We need this for Android 4:
            //         We need this for Android 4:
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Error while initializing OpenCV.")
            } else {
                System.loadLibrary("opencv_java3")
                System.loadLibrary("docscan-native")
            }
        }

        /**
         * @return an intent which will pre-select the passed document and navigate to the images
         * fragment and scroll to the specific element in the list.
         */
        fun newInstance(context: Context, documentPage: DocumentPage): Intent {
            return Intent(context, DocumentViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_PAGE, documentPage)
            }
        }

        /**
         * @return an intent which will open one of the bottom navigation based on the type.
         */
        fun newInstance(context: Context, documentViewerLaunchViewType: DocumentViewerLaunchViewType = DocumentViewerLaunchViewType.DOCUMENTS): Intent {
            return Intent(context, DocumentViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW, documentViewerLaunchViewType)
            }
        }
    }

    //    This is needed if the Activity is opened with an intent extra in order to show a certain
//    fragment: ImagesFragment or PdfFragment. In this case the fragments are created and the
//    selected item should change, without doing anything else:
//    private var updateSelectedNavigationItem = false
    private lateinit var messageReceiver: BroadcastReceiver
    private lateinit var selectableToolbar: SelectableToolbar
    private var newPdfs = mutableListOf<String?>()

    override fun onPdfSheetSelected(pdf: DocumentFile, sheetAction: ActionSheet.SheetAction) {

        when (sheetAction.mId) {
            R.id.action_pdf_share_item -> {
                sharePdf(pdf)
            }
            R.id.action_pdf_delete_item -> {
                showDeletePdfConfirmationDialog(pdf)
            }
        }
    }

    private fun deletePdf(pdf: DocumentFile) {

        val name = pdf.name
        pdf.delete()

        supportFragmentManager.findFragmentByTag(PdfFragment.TAG)?.apply {
            //                    Scan again for the files:
            if ((this as PdfFragment).isVisible)
                updatePdfs()
        }

        showDocumentsDeletedSnackbar(name)
    }


    private fun sharePdf(pdf: DocumentFile) {
        val uri = pdf.uri
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        //                    check here if the content resolver is null
        shareIntent.setDataAndType(uri, contentResolver.getType(uri))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/pdf"
        startActivity(
                Intent.createChooser(
                        shareIntent,
                        getString(R.string.page_slide_fragment_share_choose_app_text)
                )
        )
    }


    override fun onSelectionActivated(activated: Boolean) {

//        Update the toolbar:
        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
            if ((this as ImagesFragment).isVisible) {
                selectableToolbar.toolbar.menu.clear()

                if (activated)
                    selectableToolbar.toolbar.inflateMenu(R.menu.images_selected_menu)
                else
                    selectableToolbar.toolbar.inflateMenu(R.menu.images_menu)
            }
        }

///        Update the floating action buttons:
        if (activated) {
            findViewById<FloatingActionButton>(R.id.viewer_camera_fab).hide()
//            hide all other fab's as well:
            showFAB(-1)
        } else {
//            show the camera fab:
            findViewById<FloatingActionButton>(R.id.viewer_camera_fab).show()
            when (binding.bottomNav.selectedItemId) {
                R.id.viewer_documents -> showFAB(R.id.viewer_add_fab)
                R.id.viewer_images -> showFAB(R.id.viewer_upload_fab)
            }
        }

    }

    private fun showDeletePdfConfirmationDialog(pdf: DocumentFile) {


//        val deleteText = resources.getString(R.string.sync_confirm_delete_prefix_text)

        val deleteText = getString(R.string.viewer_delete_pdf_text)
        val deleteTitle = "${getString(R.string.viewer_delete_pdf_title)}: ${pdf.name}?"

        val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(deleteTitle)
                .setMessage(deleteText)
                .setPositiveButton(R.string.sync_confirm_delete_button_text) { dialogInterface, i ->
                    deletePdf(pdf)
                }
                .setNegativeButton(R.string.sync_cancel_delete_button_text, null)
                .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    fun deleteImages(item: MenuItem) {
// TODO: Relocate this into the fragment
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            deleteImagesDialog(this as ImagesFragment, this.getSelectionCount())
//        }
    }

    private fun deleteImages(imgFragment: ImagesFragment) {
// TODO: Relocate this into the fragment

//        val selCount = imgFragment.getSelectionCount()
// TODO       imgFragment.deleteSelections()

//        Cancel the selection mode:
//        imgFragment.deselectAllItems()

//        showImagesDeletedSnackbar(selCount)
        //            Reset the toolbar to default mode:
        selectableToolbar.resetToolbar()
    }

    private fun deleteImagesDialog(imgFragment: ImagesFragment, selCount: Int) {

        val alertDialogBuilder = AlertDialog.Builder(this)

        val prefix = resources.getString(R.string.gallery_confirm_delete_title_prefix)
        val postfix =
                if (selCount == 1)
                    resources.getString(R.string.gallery_confirm_delete_images_title_single_postfix)
                else
                    resources.getString(R.string.gallery_confirm_delete_images_title_multiple_postfix)
        val title = "$prefix $selCount $postfix"

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.gallery_confirm_delete_text)
                .setTitle(title)
                .setPositiveButton(R.string.gallery_confirm_delete_confirm_button_text) { dialogInterface, i ->
                    deleteImages(
                            imgFragment
                    )
                }
                .setNegativeButton(R.string.gallery_confirm_delete_cancel_button_text, null)
                .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    fun rotateImages(item: MenuItem) {
// TODO: relocate into fragment
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            val files = (this as ImagesFragment).getSelectedFiles()
//            for (file in files)
//                rotateFile(file)
//        }
    }


    fun selectAll(item: MenuItem) {
        // TODO: Relocate into fragment
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            if ((this as ImagesFragment).isVisible) {
//                this.selectAll()
//            }
//        }
    }

    fun openDocumentOption(item: MenuItem) {

        // TODO: Show selected document with options
//        selectedDocument?.let { showDocumentOptions(it) }

    }


    override fun onDocumentSheetSelected(document: DocumentWithPages, sheetAction: ActionSheet.SheetAction) {

        when (sheetAction.mId) {
            R.id.action_document_continue_item -> {
//                DocumentStorage.getInstance(this).title = document.document.title
                Helper.startCameraActivity(this)
                finish()
            }
            R.id.action_document_pdf_item -> {
                if (KtHelper.isPdfFolderPermissionGiven(this)) {
                    if (document.isCropped())
                        showPdfOcrDialog(document)
                    else
                        showNotCropDialog(document, false)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        showDirectoryPermissionRequiredAlert()
                    }
                }
            }
            R.id.action_document_edit_item -> {
//                Start the rename activity and wait for the result:
                val intent = Intent(applicationContext, EditDocumentActivity::class.java)
                intent.putExtra(EditDocumentActivity.DOCUMENT_NAME_KEY, document.document.title)
                startActivityForResult(intent, DOCUMENT_RENAMING_REQUEST)
//                startActivity(intent)
            }
            R.id.action_document_delete_item -> showDeleteConfirmationDialog(document)
            R.id.action_document_crop_item -> {
                if (document.isCropped())
                    showNoCropDialog()
                else
                    showCropConfirmationDialog(document)
            }

            R.id.action_document_upload_item -> {

                DataLog.getInstance()
                        .writeUploadLog(this, TAG, "onDocumentSheetSelected: ${document.document.title}")

//                Do nothing if the document is already uploaded:
                //TODO: Handle this in the repository, warn the user that if something has been uploaded.
                // TODO: Should it change?
//                if (document.isUploaded)
//                    return

                if (document.isCropped())
                    uploadDocument(document)
                else
                    showNotCropDialog(document, true)
            }
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
// TODO: ALl of the updating stuff is not necessary, since this is backed up by the DB flows.
        when (requestCode) {
//
////            Permissions:
//            PERSISTABLE_URI_PERMISSION -> {
//                if (resultCode == Activity.RESULT_OK) {
//                    // The result data contains a URI for the document or directory that
//                    // the user selected.
//                    data?.data?.also { uri ->
//                        KtHelper.saveDocumentDir(this, uri)
//                        showDocumentDirSetDialog()
//                    }
//                }
//            }
////            Document rename:
//            DOCUMENT_RENAMING_REQUEST, LAUNCH_VIEWER_REQUEST -> {
//                supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//                    if ((this as ImagesFragment).isVisible) {
//                        selectableToolbar.resetToolbar()
//
//                        if (resultCode == Activity.RESULT_OK) {
////                    Did the user rename the current document?
//                            if (requestCode == DOCUMENT_RENAMING_REQUEST) {
//                                if (data!!.data != null) {
//                                    val docName = data.data!!.toString()
//                                    selectableToolbar.setTitle(docName)
//                                    updateDocumentName(docName)
//                                }
//                            }
////                    Did the user change one or multiple images in the PageSlideActivity?
//                            else if (requestCode == LAUNCH_VIEWER_REQUEST) {
//                                if (data != null && data.getBooleanExtra(
//                                                KEY_IMAGE_CHANGED,
//                                                false
//                                        )
//                                ) {
//                                    redrawItems()
//                                }
//
//                            }
//                        }
//                    }
//                }
//            }
            DOCUMENT_PDF_SELECTION_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data!!.data != null) {
                    val docName = data.data!!.toString()
// TODO: The selected doc via the chooser needs to be somehow retrieved in a different manner
//                    val document = DocumentStorage.getInstance(this).getDocument(docName)
//                    openPDFsView()
//                    selectBottomNavigationViewMenuItem(R.id.viewer_pdfs)
//                    showFAB(R.id.viewer_add_pdf_fab)
//                    toolbar.title = getText(R.string.document_navigation_pdfs)
//
//                    if (Helper.isDocumentCropped(document))
//                        showPdfOcrDialog(document)
//                    else
//                        showNotCropDialog(document, false)
                }
            }

        }


    }


    private fun uploadDocument(document: DocumentWithPages) {

        DataLog.getInstance().writeUploadLog(this, TAG, "uploadDocument: ${document.document.title}")

//        First check if user is online
        if (!Helper.isOnline(this)) {
            showOfflineDialog()
            return
        }
//        Check if the user is logged in:
        if (User.getInstance().isLoggedIn)
            startUpload(document)
        else
            showNotLoggedInDialog()

    }

    private fun showNotLoggedInDialog() {

        val dialogTitle = "${getString(R.string.viewer_not_logged_in_title)}"
        val dialogText = "${getString(R.string.sync_not_logged_in_text)}"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(dialogTitle)
                .setMessage(dialogText)
                .setPositiveButton(R.string.dialog_yes_text) { _, _ ->
                    run {
                        val intent = Intent(applicationContext, AccountActivity::class.java)
                        intent.putExtra(PARENT_ACTIVITY_NAME, this.javaClass.name)
                        startActivity(intent)
                    }
                }
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    private fun startUpload(document: DocumentWithPages) {

        if (document != null) {
            DataLog.getInstance().writeUploadLog(this, TAG, "startUpload document: $document")
            DataLog.getInstance()
                    .writeUploadLog(this, TAG, "startUpload document: ${document.document.title}")
        }

        var dirs = ArrayList<String>()
        dirs.add(document.document.title)
        startUpload(dirs)

        showUploadStartedSnackbar()

        // TODO: Not necessary, but re-check states
//        //        DocumentsFragment might be null, hence use apply:
//        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//            //            This is necessary to change the upload state icon of the document:
//            (this as DocumentsFragment).reloadDocuments()
//        }
    }

    /**
     * Shows a snackbar indicating that the upload started.
     */
    private fun showUploadStartedSnackbar() {

        val snackbarText = "${getString(R.string.sync_snackbar_upload_started)}"
        val s = Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText,
                Snackbar.LENGTH_LONG
        )
        s.show()

    }

    private fun showOfflineDialog() {

        val dialogTitle = "${getString(R.string.viewer_offline_title)}"
        val dialogText = "${getString(R.string.viewer_offline_text)}"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(dialogTitle)
                .setMessage(dialogText)
                .setPositiveButton(R.string.dialog_ok_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }


    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    // TODO: Handle this via the viewModel

    private fun startUpload(uploadDirs: java.util.ArrayList<String>) {

        DataLog.getInstance().writeUploadLog(this, TAG, "startUpload: $uploadDirs")

        SyncStorage.getInstance(this).addUploadDirs(this, uploadDirs)
        SyncUtils.startSyncJob(this, false)

    }

    override fun onBackPressed() {

        when (binding.bottomNav.selectedItemId) {

//            Open the CameraActivity:
            R.id.viewer_documents -> Helper.startCameraActivity(this)
//            Open the DocumentsFragment:
            R.id.viewer_pdfs -> binding.bottomNav.selectedItemId = R.id.viewer_documents
//            Special case for ImagesFragment:
            R.id.viewer_images -> {
                // TODO: Should not be necessary
//                supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//                    //                If there are currently some files selected, just deselect them:
//                    if ((this as ImagesFragment).isVisible && this.getSelectionCount() > 0) {
//                        this.deselectAllItems()
//                        this.redrawItems()
//                        selectableToolbar.resetToolbar()
//                    }
//                Otherwise open the DocumentFragment
//                    else
                binding.bottomNav.selectedItemId = R.id.viewer_documents
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
//                The user pressed the exit icon in the toolbar:
                if (selectableToolbar.isSelectMode) {
                    selectableToolbar.resetToolbar()
// TODO: Relocate this into the fragment
//                    supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//                        if ((this as ImagesFragment).isVisible) {
//                            this.deselectAllItems()
//                            //        We need to redraw the check boxes:
//                            this.redrawItems()
//                        }
//                    }
                }
//                The user pressed the navigation icon: Show the navigation drawer in this case:
                else
                    mNavigationDrawer.showNavigation()
            }

        }

        return true

    }

    private fun showNotCropDialog(
            document: DocumentWithPages,
            upload: Boolean,
            extendedInfo: Boolean = false
    ) {

        val proceed =
                if (upload) getString(R.string.viewer_not_cropped_upload)
                else getString(R.string.viewer_not_cropped_pdf)

        val text: String
        if (extendedInfo)
            text = getString(R.string.viewer_images_fragment_not_cropped_confirm_text)
        else
            text = getString(R.string.viewer_not_cropped_confirm_text)

//        val text = getString(R.string.viewer_not_cropped_confirm_text)
        val cropText = "$text $proceed?"
        val cropTitle = "${getString(R.string.viewer_not_cropped_confirm_title)}"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(cropTitle)
                .setMessage(cropText)
                .setPositiveButton(R.string.dialog_yes_text) { _, _ ->
                    run {
                        if (upload)
                            uploadDocument(document)
                        else
                            showOCRAlertDialog(document)
                    }
                }
                .setNegativeButton(R.string.dialog_cancel_text, null)
        alertDialogBuilder.create().show()

    }

//    /**
//     * Checks if the document contains images that have not been cropped.
//     */
//    private fun containsUncroppedImages(document: Document): Boolean {
//
//        val pageIt = document.pages.iterator()
//        while (pageIt.hasNext()) {
//            val file = pageIt.next().file
//            if (!PageDetector.isCurrentlyProcessed(file.absolutePath))
//                return true
//        }
//        return false
//
//    }

    private fun showNoCropDialog() {

        val cropText = getString(R.string.viewer_all_cropped_text)
        val cropTitle = "${getString(R.string.viewer_all_cropped_title)}"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(cropTitle)
                .setMessage(cropText)
                .setPositiveButton(R.string.dialog_ok_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    private fun showCropConfirmationDialog(document: DocumentWithPages) {

        val cropText = getString(R.string.viewer_crop_confirm_text)
        val cropTitle = "${getString(R.string.viewer_crop_confirm_title)}: ${document.document.title}?"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(cropTitle)
                .setMessage(cropText)
                .setPositiveButton(R.string.dialog_yes_text) { dialogInterface, i ->
                    cropDocument(document)
                }
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    private fun cropDocument(document: DocumentWithPages) {
        // TODO: Handle cropping in viewModel.

        //        Update the UI:
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            (this as ImagesFragment).showCropStart()
//        }

        val pageIt = document.pages.iterator()
//        while (pageIt.hasNext()) {
//            val file = pageIt.next().file
////            Just crop it if it is not already cropped:
//            if (!PageDetector.isCropped(file.absolutePath))
//                mapFile(file)
//        }

    }

//    Delete functionality

    private fun showDeleteConfirmationDialog(document: DocumentWithPages) {


//        val deleteText = resources.getString(R.string.sync_confirm_delete_prefix_text)

        val deleteText = getString(R.string.sync_confirm_delete_doc_prefix_text)
        val deleteTitle = "${getString(R.string.sync_confirm_delete_title)}: ${document.document.title}?"


        val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(deleteTitle)
                .setMessage(deleteText)
                .setPositiveButton(R.string.sync_confirm_delete_button_text) { dialogInterface, i ->
                    // TODO: Add viewModel to delete document here
//                deleteDocument(
//                    document
//                )
                }
                .setNegativeButton(R.string.sync_cancel_delete_button_text, null)
                .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    private fun deleteDocument(document: Document) {

//        if (document == selectedDocument)
//            selectedDocument = null

//        Update the UI:

//        Remove the document from the list:
        // TODO: Delete in DB, this will be automatically updated through the flows, add message

//        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//            if ((this as DocumentsFragment).isVisible) {
//                //                update the ui:
////                deleteDocument(document)
//                showDocumentsDeletedSnackbar(document.title)
//            }
//        }

//        TODO: Close the ImagesFragment, if the request was called within it:
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            if ((this as ImagesFragment).isVisible)
//                binding.bottomNav.selectedItemId = R.id.viewer_documents
//        }

//                update the documents:
        DocumentStorage.getInstance(this).documents.remove(document)
        document.deleteImages()
        DocumentStorage.saveJSON(this)
    }

    /**
     * Shows a snackbar indicating that images have been deleted.
     */
    private fun showImagesDeletedSnackbar(count: Int) {

        val postfix = if (count == 1) "${getString(R.string.sync_snackbar_file_deleted_postfix)}"
        else "${getString(R.string.sync_snackbar_files_deleted_postfix)}"
        val snackbarText =
                "${getString(R.string.sync_snackbar_files_deleted_prefix)}: $count $postfix"
        val s = Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText,
                Snackbar.LENGTH_LONG
        )
        s.show()

    }


    /**
     * Shows a snackbar indicating that documents have been deleted.
     */
    private fun showDocumentsDeletedSnackbar(title: String?) {

        val snackbarText = "${getString(R.string.sync_snackbar_files_deleted_prefix)}: $title"
        val s = Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText,
                Snackbar.LENGTH_LONG
        )
        s.show()

    }

    private fun showPdfCreatedSnackbar(title: String) {

        val snackbarText = "${getString(R.string.viewer_pdf_created_snackbar_text)}: $title"
        val s = Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText,
                Snackbar.LENGTH_LONG
        )
        s.show()

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun grantPdfDirAccess() {

        val intent = KtHelper.getOpenDocumentDirIntent(this)
        startActivityForResult(intent, PERSISTABLE_URI_PERMISSION)

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showDirectoryPermissionRequiredAlert() {

        val title = getString(R.string.viewer_document_dir_permission_title)
        val text = getString(R.string.viewer_document_dir_permission_text)

//        val text = getString(R.string.pdf_fragment_persisted_permission_text)
//        val title = getString(R.string.pdf_fragment_persisted_permission_title)

        val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(R.string.dialog_ok_text) { _, _ ->
                    grantPdfDirAccess()
                }
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    //    OCR functionality
    fun showPdfOcrDialog(document: DocumentWithPages) {

        //        Check if the play services are installed first:
        if (!Helper.checkPlayServices(this))
            showNoPlayServicesDialog(document)
        else
            showOCRAlertDialog(document)

    }

    private fun showNoPlayServicesDialog(document: DocumentWithPages) {

        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.gallery_confirm_no_ocr_available_title)
                .setPositiveButton(R.string.dialog_yes_text) { dialog, which ->
                    createPdf(document, false)
//                    deselectListViewItems()
                }
                .setNegativeButton(R.string.dialog_no_text) { dialogInterface, i -> }
                .setCancelable(true)
                .setMessage(R.string.gallery_confirm_no_ocr_available_text)
        alertDialogBuilder.create().show()

    }

    private fun showDocumentDirSetDialog() {

        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.viewer_document_dir_set_title)
                .setPositiveButton(R.string.dialog_ok_text) { _, _ -> }
                .setCancelable(true)
                .setMessage(R.string.viewer_document_dir_set_text)
        alertDialogBuilder.create().show()

    }


    private fun createPdf(document: DocumentWithPages, withOCR: Boolean) {

        // TODO: Relocate creation of documents with new domain into the viewModel
//        if (withOCR)
//            createPdfWithOCR(document)
//        else
//            createPdf(document)

    }

    private fun showOCRAlertDialog(document: DocumentWithPages) {

        val alertDialogBuilder = AlertDialog.Builder(this)
        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.gallery_confirm_ocr_title)
                .setPositiveButton(R.string.dialog_yes_text) { dialog, which ->
                    createPdf(document, true)
//                    deselectListViewItems()
                }
                .setNegativeButton(R.string.dialog_no_text) { dialogInterface, i ->
                    createPdf(document, false)
//                    deselectListViewItems()
                }
                .setCancelable(true)
                .setMessage(R.string.gallery_confirm_ocr_text)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text)
        ) { dialog, which -> alertDialog.cancel() }
        alertDialog.show()

    }


    override fun onSheetSelected(sheetAction: ActionSheet.SheetAction) {

    }

    override fun onShown() {
    }

    override fun onDismiss() {
    }

    fun startCamera(view: View) {

        Helper.startCameraActivity(this)
        finish()

    }

    fun startGallery(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryResultCallback.launch(intent)
    }

    fun newDocument(view: View) {

        startActivity(Intent(applicationContext, CreateDocumentActivity::class.java))

    }

    fun uploadFABPressed(view: View) {

//        selectedDocument?.title?.let { showDocumentOptions(selectedDocument!!) }

        // TODO: Handle this via the viewModel, check the states there:
//        selectedDocument?.let {
//            when {
//                it.isUploaded -> showDocumentUploadedDialog()
//                Helper.isDocumentCropped(it) -> uploadDocument(it)
//                else -> showNotCropDialog(it, true, true)
//            }
//
//        }

    }

    private fun showDocumentUploadedDialog() {

        val dialogText = getString(R.string.viewer_document_uploaded_text)
        val dialogTitle = getString(R.string.viewer_document_uploaded_title)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(dialogTitle)
                .setMessage(dialogText)
                .setPositiveButton(R.string.dialog_ok_text, null)
        alertDialogBuilder.create().show()


    }

    fun newPDF(view: View) {

        val intent = Intent(applicationContext, SelectPdfDocumentActivity::class.java)
        startActivityForResult(intent, DOCUMENT_PDF_SELECTION_REQUEST)

    }

    // TODO: Check how to show document options
//    override fun onDocumentOptions(document: DocumentWithPages) {
//        showDocumentOptions(document)
//    }

    override fun getSelfNavDrawerItem(): NavigationDrawer.NavigationItemEnum {
        return NavigationDrawer.NavigationItemEnum.DOCUMENTS
    }

    private fun showPdfOptions(file: DocumentFile) {

        val sheetActions = ArrayList<ActionSheet.SheetAction>()

        sheetActions.add(
                ActionSheet.SheetAction(
                        R.id.action_pdf_share_item,
                        getString(R.string.action_pdf_share),
                        R.drawable.ic_share_black_24dp
                )
        )

        sheetActions.add(
                ActionSheet.SheetAction(
                        R.id.action_pdf_delete_item,
                        getString(R.string.action_document_delete_document),
                        R.drawable.ic_delete_black_24dp
                )
        )

        val actionSheet = PdfActionSheet(file, sheetActions, this, this)
        supportFragmentManager.beginTransaction().add(actionSheet, "TAG").commit()

    }

    private fun showDocumentOptions(document: DocumentWithPages) {

        val sheetActions = ArrayList<ActionSheet.SheetAction>()
        sheetActions.add(
                ActionSheet.SheetAction(
                        R.id.action_document_continue_item,
                        getString(R.string.action_document_continue_document),
                        R.drawable.ic_add_a_photo_black_24dp
                )
        )
        sheetActions.add(
                ActionSheet.SheetAction(
                        R.id.action_document_edit_item,
                        getString(R.string.action_document_edit_document),
                        R.drawable.ic_edit_black_24dp
                )
        )
//        This options are just available if the document contains at least one image:
        if (!document.pages.isEmpty()) {
            sheetActions.add(
                    ActionSheet.SheetAction(
                            R.id.action_document_crop_item,
                            getString(R.string.action_document_crop_title),
                            R.drawable.ic_transform_black_24dp
                    )
            )
            sheetActions.add(
                    ActionSheet.SheetAction(
                            R.id.action_document_pdf_item,
                            getString(R.string.action_document_pdf_title),
                            R.drawable.ic_baseline_picture_as_pdf_24px
                    )
            )
            if (!document.isuploaded())
                sheetActions.add(
                        ActionSheet.SheetAction(
                                R.id.action_document_upload_item,
                                getString(R.string.action_document_upload_document),
                                R.drawable.ic_cloud_upload_black_24dp
                        )
                )
            else
                sheetActions.add(
                        ActionSheet.SheetAction(
                                R.id.action_document_upload_item,
                                getString(R.string.action_document_upload_document),
                                R.drawable.ic_cloud_upload_gray_24dp
                        )
                )
        }
        sheetActions.add(
                ActionSheet.SheetAction(
                        R.id.action_document_delete_item,
                        getString(R.string.action_document_delete_document),
                        R.drawable.ic_delete_black_24dp
                )
        )

        val actionSheet = DocumentActionSheet(document, sheetActions, this, this)
        supportFragmentManager.beginTransaction().add(actionSheet, "TAG").commit()

    }

    /**
     * Displays the number of selected items if it is larger than zero in the toolbar. Otherwise
     * shows the default toolbar title.
     */
    override fun onSelectionChange(selectionCount: Int) {

        selectableToolbar.update(selectionCount)

    }

//    private var selectedDocument: DocumentWithPages? = null
    //TODO: check how to open document pager with images Fragment.

//    override fun onDocumentOpened(document: DocumentWithPages) {
////        selectedDocument = document
////        updateSelectedNavigationItem = false
////        Opens the ImagesFragment:
//        binding.bottomNav.selectedItemId = R.id.viewer_images
//
//    }

    /**
     * Shows the new floating action button and hides any previous fab. If newFABID is -1 all fab's
     * are hidden except for the camera fab.
     */
    private fun showFAB(newFABID: Int) {

        var newFAB: FloatingActionButton? = null
        if (newFABID != -1)
            newFAB = findViewById(newFABID)

        if (newFABID == R.id.viewer_add_fab || newFABID == -1) {
            hideFab(R.id.viewer_add_pdf_fab)
            hideFab(R.id.viewer_upload_fab)
        }
        if (newFABID == R.id.viewer_add_pdf_fab || newFABID == -1) {
            hideFab(R.id.viewer_add_fab)
            hideFab(R.id.viewer_upload_fab)
        }

        if (newFABID == R.id.viewer_upload_fab || newFABID == -1) {
            hideFab(R.id.viewer_add_fab)
            hideFab(R.id.viewer_add_pdf_fab)
        }
//        Show the button if it is assigned:
        newFAB?.show()

    }

    private fun hideFab(id: Int) {
        val fab: FloatingActionButton = findViewById(id)
        if (fab.visibility == View.VISIBLE)
            fab.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNav.setOnNavigationItemSelectedListener(this)

        initToolbar()

        val documentPage = intent.getParcelableExtra<DocumentPage>(EXTRA_DOCUMENT_PAGE)
        val type = intent.getSerializableExtra(EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW) as? DocumentViewerLaunchViewType

        // TODO: The entire fragment handling is a bit confusing.

        if (documentPage != null) {
            // TODO: Open the images fragment from here
//            openImagesFragmentFromIntent(document, fileName)

        } else {
            when (type) {
                DocumentViewerLaunchViewType.PDFS -> {
                    // TODO: Check if the animations are correct
                    openPDFsView()
                }
                DocumentViewerLaunchViewType.DOCUMENTS, null -> {
                    openDocumentsView()
                }
            }
        }

        // TODO: Check if this is necessary, as the previous if statement should be exhaustive.
        // Just open the document view if no other fragment is already shown:
        if (supportFragmentManager.findFragmentByTag(ImagesFragment.TAG) == null &&
                supportFragmentManager.findFragmentByTag(PdfFragment.TAG) == null
        ) {
            openDocumentsView()
        }

        observe()
    }

    private fun observe() {
        viewModel.observableNumOfSelectedElements.observe(this, {
            if (it > 0) {
                if (selectableToolbar.isSelectMode) {
                    selectableToolbar.update(it)
                } else {
                    selectableToolbar.selectToolbar(it)
                }
            } else {
                if (selectableToolbar.isSelectMode) {
                    selectableToolbar.resetToolbar()
                }
            }
        })
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

//        Do nothing in case the item is already selected:
        if (binding.bottomNav.selectedItemId == menuItem.itemId)
            return false


        return openNavigationView(menuItem.itemId)

    }

    private fun openNavigationView(@IdRes itemId: Int): Boolean {

        //        Clear the toolbar menu:
        selectableToolbar.resetToolbar()
        toolbar.menu.clear()

        when (itemId) {
            R.id.viewer_documents -> openDocumentsView()
            R.id.viewer_images -> openImagesView()
            R.id.viewer_pdfs -> openPDFsView()
//            This should not happen:
            else -> return false
        }

        //        Expand the AppBarLayout without any animation
        findViewById<AppBarLayout>(R.id.gallery_appbar).setExpanded(true, false)

        return true

    }

    private fun openDocumentsView() {

        val documentsFragment = DocumentsFragment.newInstance()

        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(
                R.anim.translate_right_to_left_in,
                R.anim.translate_right_to_left_out
        )
        ft.replace(
                R.id.viewer_fragment_layout, documentsFragment,
                DocumentsFragment.TAG
        ).commit()

        // TODO: Checkout scroll to current active document
//        documentsFragment.scrollToActiveDocument()

        showFAB(R.id.viewer_add_fab)

        toolbar.title = getText(R.string.document_navigation_documents)

    }

    private fun openPDFsView() {

//        Hide any badge if existing:
        binding.bottomNav.removeBadge(R.id.viewer_pdfs)
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(
                R.anim.translate_left_to_right_in,
                R.anim.translate_left_to_right_out
        )
        ft.replace(R.id.viewer_fragment_layout, PdfFragment.newInstance(), PdfFragment.TAG).commit()
        showFAB(R.id.viewer_add_pdf_fab)

        toolbar.title = getText(R.string.document_navigation_pdfs)
    }

    private fun openImagesView() {

//TODO        The user just clicked images tab and did not open any document:
//        if (selectedDocument == null)
//            selectedDocument = DocumentStorage.getInstance(this).activeDocument
//
//        selectedDocument?.let { openImagesView(it) }

    }


    private fun openImagesView(document: DocumentWithPages) {

        val imagesFragment = setupImagesFragment(document)

        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        //                The animation depends on the position of the selected item:
        if (binding.bottomNav.selectedItemId == R.id.viewer_documents)
            ft.setCustomAnimations(
                    R.anim.translate_left_to_right_in,
                    R.anim.translate_left_to_right_out
            )
        else
            ft.setCustomAnimations(
                    R.anim.translate_right_to_left_in,
                    R.anim.translate_right_to_left_out
            )

////                Create the shared element transition:
//        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//            if ((this as DocumentsFragment).isVisible) {
////          Check if the document has pages, otherwise use no shared element transition:
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && selectedDocument?.pages?.isNotEmpty()!!) {
//                    setExitTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.fade))
//                    imagesFragment.postponeEnterTransition()
//                    imagesFragment.sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
//                    var imageView = getImageView(document)
//                    ft.addSharedElement(imageView!!, imageView!!.transitionName)
//                    ft.setReorderingAllowed(true)
//                }
//            }
//        }

        ft.replace(
                R.id.viewer_fragment_layout, imagesFragment,
                ImagesFragment.TAG
        ).commit()

    }

    private fun openImagesFragmentFromIntent(document: DocumentWithPages, fileName: String? = null) {

//        selectedDocument = document

        val imagesFragment = setupImagesFragment(document)

//        Create the enter transition:
        if (fileName != null) {
            postponeEnterTransition()
//  TODO:          imagesFragment.scrollToFile(fileName)
        }
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(
                R.id.viewer_fragment_layout, imagesFragment,
                ImagesFragment.TAG
        ).commit()

//        selectNavigationItem(R.id.viewer_images)
        selectBottomNavigationViewMenuItem(R.id.viewer_images)

//        setupImagesFragmentToolbar(document)

    }

    private fun setupImagesFragment(document: DocumentWithPages): ImagesFragment {

//        val arguments = Bundle().apply {
//            putString(DOCUMENT_NAME_KEY, document.document.title)
//        }
        // TODO: This is not correct, just for debugging purposes!
        val imagesFragment = ImagesFragment.newInstance(UUID.randomUUID(), UUID.randomUUID())

//        Setup the toolbar
        setupImagesFragmentToolbar(document)
//        Hide any additional floating action button
        showFAB(R.id.viewer_upload_fab)

        return imagesFragment

    }

    // TODO: Check empty state
    private fun setupImagesFragmentToolbar(document: DocumentWithPages) {

        //        Update the toolbar title:
        if (document != null)
            toolbar.setTitle(document.document.title)
        else
            toolbar.setTitle(getString(R.string.document_navigation_images))
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//
//        val inflater = menuInflater
//        inflater.inflate(R.menu.images_menu, menu)
//
//        return true
//
//    }

    private fun initToolbar() {

        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        val appBarLayout: AppBarLayout = findViewById(R.id.gallery_appbar)

        selectableToolbar = SelectableToolbar(this, toolbar, appBarLayout)
        //        Enable back navigation in action bar:
        setSupportActionBar(toolbar)
        selectableToolbar.setTitle(getText(R.string.document_navigation_documents))

    }


    /**
     * Selects the specified item in the bottom navigation view without triggering a callback.
     */
    private fun selectBottomNavigationViewMenuItem(@IdRes menuItemId: Int) {
        binding.bottomNav.setOnNavigationItemSelectedListener(null)
        binding.bottomNav.selectedItemId = menuItemId
        binding.bottomNav.setOnNavigationItemSelectedListener(this)
    }

    override fun onResume() {

        super.onResume()
        // Register to receive messages:
        messageReceiver = getReceiver()
        val filter = IntentFilter()
        filter.addAction(INTENT_UPLOAD_ACTION)
        filter.addAction(INTENT_IMAGE_PROCESS_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter)


//        Assure that the current fragment and the selected item are corresponding.
//        We are using this, because the selection after resuming is not always corresponding to the
//        fragment. It seems the behavior is different for different devices.
        val selItemId = getItemIdOfActiveFragment()
        if (selItemId != binding.bottomNav.selectedItemId)
            selectBottomNavigationViewMenuItem(selItemId)
//            openNavigationView(selItemId)

//        Assure that the FAB is correct:
        when (selItemId) {
            R.id.viewer_pdfs -> {
                showFAB(R.id.viewer_add_pdf_fab)
                toolbar.title = getText(R.string.document_navigation_pdfs)
            }
            R.id.viewer_images -> {
                showFAB(R.id.viewer_upload_fab)
                //TODO: Check the toolbars title
//                selectedDocument?.let { toolbar.title = it.document.title }
            }
            R.id.viewer_documents -> {
                showFAB(R.id.viewer_add_fab)
                toolbar.title = getText(R.string.document_navigation_documents)
            }
        }
//        Assure that the title is correct:

//            selectBottomNavigationViewMenuItem(selItemId)

    }

    /**
     * Returns the item id of the current and active fragment.
     */
    private fun getItemIdOfActiveFragment(): Int {

        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.isVisible
        return when {
            supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG) != null -> R.id.viewer_documents
            supportFragmentManager.findFragmentByTag(ImagesFragment.TAG) != null -> R.id.viewer_images
            supportFragmentManager.findFragmentByTag(PdfFragment.TAG) != null -> R.id.viewer_pdfs
            else -> -1
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)

    }

//    override fun onStop() {
//        super.onStop()
////        DocumentStorage.saveJSON(this)
//        //        if (changeRatio > 0.01) {
//        val t1 = SimpleDateFormat("yyyyMMdd_HHmmssSS").format(Date())
//        Crashlytics.setString(Helper.START_SAVE_JSON_CALLER, "DocumentViewerActivity::1258 $t1")
//        DocumentStorage.saveJSON(this)
//        val t2 = SimpleDateFormat("yyyyMMdd_HHmmssSS").format(Date())
//        Crashlytics.setString(Helper.END_SAVE_JSON_CALLER, "DocumentViewerActivity:1260 $t2")
//    }

    // TODO: Sooner or later, this will be obsolete if everything is handled through the DB
    private fun getReceiver(): BroadcastReceiver {

        return object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {

                when (intent?.action) {
                    INTENT_IMAGE_PROCESS_ACTION -> {
                        val defValue = -1
                        val fileName = intent.getStringExtra(INTENT_FILE_NAME)

                        when (intent.getIntExtra(INTENT_IMAGE_PROCESS_TYPE, defValue)) {
                            INTENT_PDF_PROCESS_FINISHED -> {
//                                The intent is directly consumed if the PdfFragment is open:
                                var intentConsumed = false
                                supportFragmentManager.findFragmentByTag(PdfFragment.TAG)?.apply {
                                    if ((this as PdfFragment).isVisible) {
                                        this.updateFile(fileName)
                                        intentConsumed = true
                                    }
                                }
//                                If the PdfFragment is not opened remember the new PDFs:
                                if (!intentConsumed)
                                    newPdfs.add(fileName)

                                showPdfCreatedSnackbar(File(fileName).name)

                                binding.bottomNav.getOrCreateBadge(R.id.viewer_pdfs)
                            }
                            INTENT_IMAGE_PROCESS_FINISHED -> {
                                updateGallery(fileName)
                                updateDocumentList(fileName)
                            }
//                            INTENT_IMAGE_PROCESS_STARTED, INTENT_IMAGE_PROCESS_FINISHED -> {
//                                updateGallery(fileName)
//                                updateDocumentList(fileName)
//                            }
                        }
                    }

                    INTENT_UPLOAD_ACTION -> {
                        // TODO: Check why the selectedItemId has been sometimes switched
//                        if (supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG) != null) {
//                            supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//                                (this as DocumentsFragment).reloadDocuments()
//                            }
//                        } else {
//                            binding.bottomNav.selectedItemId = R.id.viewer_documents
//                        }

                        when (intent.getStringExtra(UPLOAD_INTEND_TYPE)) {
                            UPLOAD_ERROR_ID -> showUploadErrorDialog()
                            UPLOAD_OFFLINE_ERROR_ID -> showOfflineSnackbar()
                            UPLOAD_FINISHED_ID -> showUploadFinishedSnackbar()
                            UPLOAD_FILE_DELETED_ERROR_ID -> showFileDeletedErrorDialog()
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a snackbar indicating that the device is offline.
     */
    private fun showOfflineSnackbar() {

        val snackbarText = resources.getString(R.string.sync_snackbar_offline_text)

        Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG
        ).show()

    }

    private fun showFileDeletedErrorDialog() {

        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_file_deleted_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_file_deleted_text)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }


    /**
     * Shows a snackbar indicating that the upload process starts. We need this because we have
     * little control of the time when the upload starts really.
     */
    private fun showUploadFinishedSnackbar() {

        val snackbarText = resources.getString(R.string.sync_snackbar_finished_upload_text)

//        closeSnackbar()
        Snackbar.make(
                findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG
        ).show()


    }


    private fun showUploadErrorDialog() {

        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.sync_error_upload_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.sync_error_upload_text)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    private fun updateDocumentList(fileName: String?) {

        if (fileName == null)
            return

//        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//            if ((this as DocumentsFragment).isVisible) {
//                // TODO: This won't be necessary if handled with flows
////                this.checkDocumentProcessStatus(File(fileName))
//            }
//        }

    }

    private fun updateGallery(fileName: String?) {

        if (fileName == null)
            return
// TODO: Relocate to fragment
//        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
//            if ((this as ImagesFragment).isVisible) {
//                this.updateGallery(fileName)
//            }
//        }

    }

    override fun onPdfOptions(file: DocumentFile) {
        showPdfOptions(file)
    }


}
