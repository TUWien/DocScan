package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ActionSheet
import at.ac.tuwien.caa.docscan.camera.DocumentActionSheet
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.*
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.sync.SyncStorage
import at.ac.tuwien.caa.docscan.sync.SyncUtils
import at.ac.tuwien.caa.docscan.sync.UploadService.*
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity
import at.ac.tuwien.caa.docscan.ui.syncui.DocumentAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Partly based on this tutorial:
 * https://pspdfkit.com/blog/2019/using-the-bottom-navigation-view-in-android/
 */

class ViewerActivity : BaseNavigationActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener,
        DocumentAdapter.DocumentAdapterCallback,
        DocumentsFragment.DocumentListener,
        ImagesAdapter.ImagesAdapterCallback,
        ActionSheet.SheetSelection,
        ActionSheet.DocumentSheetSelection,
        ActionSheet.DialogStatus,
        SelectableToolbar.SelectableToolbarCallback
{

    override fun onSelectionActivated(activated: Boolean) {

//        Update the toolbar:
        if (activated) {
            supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
                if ((this as ImagesFragment).isVisible && activated)
                    toolbar.inflateMenu(R.menu.images_menu)
            }
        }
        else
            toolbar.menu.clear()

//        Update the floating action buttons:

//        Hide the fabs:
        if (activated) {
            findViewById<FloatingActionButton>(R.id.viewer_camera_fab).hide()
//            hide all other fab's as well:
            showFAB(-1)
        }
        else {
//            show the camera fab anyway:
            findViewById<FloatingActionButton>(R.id.viewer_camera_fab).show()
            when (bottomNavigationView.selectedItemId) {
                R.id.viewer_documents -> showFAB(R.id.viewer_add_fab)
                R.id.viewer_images -> showFAB(R.id.viewer_edit_fab)
            }
        }

    }

    fun deleteImages(item: MenuItem) {

        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
            deleteImagesDialog(this as ImagesFragment, this.getSelectionCount())
        }
    }

    private fun deleteImages(imgFragment: ImagesFragment) {

        val selCount = imgFragment.getSelectionCount()
        imgFragment.deleteSelections()

        showImagesDeletedSnackbar(selCount)
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
                .setPositiveButton(R.string.gallery_confirm_delete_confirm_button_text) {
                    dialogInterface, i -> deleteImages(imgFragment) }
                .setNegativeButton(R.string.gallery_confirm_delete_cancel_button_text, null)
                .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    fun rotateImages(item: MenuItem) {

        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
            val files = (this as ImagesFragment).getSelectedFiles()
            for (file in files)
                rotateFile(file)
        }
    }

    companion object {
        val TAG = "ViewerActivity"
    }

    private lateinit var messageReceiver: BroadcastReceiver
    private lateinit var selectableToolbar: SelectableToolbar

    override fun onDocumentSheetSelected(document: Document, sheetAction: ActionSheet.SheetAction) {

        when (sheetAction.mId) {
            R.id.action_document_continue_item -> {
                DocumentStorage.getInstance(this).title = document.getTitle()
                Helper.startCameraActivity(this)
                DocumentStorage.saveJSON(this)
                finish()
            }
            R.id.action_document_pdf_item -> {
                if (Helper.isDocumentCropped(document))
                    showPdfOcrDialog(document)
                else
                    showNotCropDialog(document, false)
            }
            R.id.action_document_edit_item -> {
                val intent = Intent(applicationContext, EditDocumentActivity::class.java)
                intent.putExtra(EditDocumentActivity.DOCUMENT_NAME_KEY, document.title)
                startActivity(intent)
            }
            R.id.action_document_delete_item -> showDeleteConfirmationDialog(document)
            R.id.action_document_crop_item -> showCropConfirmationDialog(document)
            R.id.action_document_upload_item -> {
                if (Helper.isDocumentCropped(document))
                    uploadDocument(document)
                else
                    showNotCropDialog(document, true)
            }
        }

    }

    private fun uploadDocument(document: Document) {

        var dirs = ArrayList<String>()
        dirs.add(document.title)
        startUpload(dirs)

//        DocumentsFragment might be null, hence use apply:
        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
//            This is necessary to change the upload state icon of the document:
            (this as DocumentsFragment).resetAdapter()
        }

    }



    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    private fun startUpload(uploadDirs: java.util.ArrayList<String>) {

        SyncStorage.getInstance(this).addUploadDirs(uploadDirs)
        SyncUtils.startSyncJob(this, false)

    }

    override fun onBackPressed() {

        when (bottomNavigationView.selectedItemId) {

//            Open the CameraActivity:
            R.id.viewer_documents -> super.onBackPressed()
//            Open the DocumentsFragment:
            R.id.viewer_pdfs -> bottomNavigationView.selectedItemId = R.id.viewer_documents
//            Special case for ImagesFragment:
            R.id.viewer_images -> {
                if (bottomNavigationView.selectedItemId == R.id.viewer_documents)
                    super.onBackPressed()
                else {
//            ImagesFragment might be null, hence use apply:
                    supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
                        //                If there are currently some files selected, just deselect them:
                        if ((this as ImagesFragment).isVisible && this.getSelectionCount() > 0) {
                            this.deselectAllItems()
                            this.redrawItems()
                            selectableToolbar.resetToolbar()
                        }
//                Otherwise open the DocumentFragment
                        else
                            bottomNavigationView.selectedItemId = R.id.viewer_documents
                    }
                }
            }
        }


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
//                The user pressed the exit icon in the toolbar:
                if (selectableToolbar.isSelectMode) {
                    selectableToolbar.resetToolbar()

                    supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
                        if ((this as ImagesFragment).isVisible) {
                            this.deselectAllItems()
                            //        We need to redraw the check boxes:
                            this.redrawItems()
                        }
                    }
                }
//                The user pressed the navigation icon: Show the navigation drawer in this case:
                else
                    mNavigationDrawer.showNavigation()
            }

        }

        return true

    }

    private fun showNotCropDialog(document: Document, upload: Boolean) {

        val proceed =
                if (upload) getString(R.string.viewer_not_cropped_upload)
                else getString(R.string.viewer_not_cropped_pdf)
        val cropText = "${getString(R.string.viewer_not_cropped_confirm_text)} $proceed?"
        val cropTitle = "${getString(R.string.viewer_not_cropped_confirm_title)} ${document.title}"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(cropTitle)
                .setMessage(cropText)
                .setPositiveButton(R.string.dialog_yes_text) {
                    _, _ ->
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


    private fun showCropConfirmationDialog(document: Document) {

        val cropText = getString(R.string.viewer_crop_confirm_text)
        val cropTitle = "${getString(R.string.viewer_crop_confirm_title)}: ${document.title}?"

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setTitle(cropTitle)
                .setMessage(cropText)
                .setPositiveButton(R.string.dialog_yes_text) { dialogInterface, i -> cropDocument(document) }
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    private fun cropDocument(document: Document) {

        //        Update the UI:
        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
            (this as ImagesFragment).showCropStart()
        }

        val pageIt = document.pages.iterator()
        while (pageIt.hasNext())
            mapFile(pageIt.next().file)

    }

//    Delete functionality

    private fun showDeleteConfirmationDialog(document: Document) {


//        val deleteText = resources.getString(R.string.sync_confirm_delete_prefix_text)

        val deleteText = getString(R.string.sync_confirm_delete_doc_prefix_text)
        val deleteTitle = "${getString(R.string.sync_confirm_delete_title)}: ${document.title}?"


        val alertDialogBuilder = AlertDialog.Builder(this)
                .setTitle(deleteTitle)
                .setMessage(deleteText)
                .setPositiveButton(R.string.sync_confirm_delete_button_text) { dialogInterface, i -> deleteDocument(document) }
                .setNegativeButton(R.string.sync_cancel_delete_button_text, null)
                .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    private fun deleteDocument(document: Document) {

//        Update the UI:
        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
            if ((this as DocumentsFragment).isVisible) {
                //                update the ui:
                this.deleteDocument(document)
                showDocumentsDeletedSnackbar(document.title)
            }
        }
//                update the documents:
        DocumentStorage.getInstance(this).documents.remove(document)
        DocumentStorage.saveJSON(this)

    }

    /**
     * Shows a snackbar indicating that images have been deleted.
     */
    private fun showImagesDeletedSnackbar(count: Int) {

        val postfix = if (count == 1) "${getString(R.string.sync_snackbar_file_deleted_postfix)}"
            else "${getString(R.string.sync_snackbar_files_deleted_postfix)}"
        val snackbarText = "${getString(R.string.sync_snackbar_files_deleted_prefix)}: $count $postfix"
        val s = Snackbar.make(findViewById(R.id.sync_coordinatorlayout), snackbarText, Snackbar.LENGTH_LONG)
        s.show()

    }


    /**
     * Shows a snackbar indicating that documents have been deleted.
     */
    private fun showDocumentsDeletedSnackbar(title: String) {

        val snackbarText = "${getString(R.string.sync_snackbar_files_deleted_prefix)}: $title"
        val s = Snackbar.make(findViewById(R.id.sync_coordinatorlayout), snackbarText, Snackbar.LENGTH_LONG)
//        s.anchorView = findViewById(R.id.viewer_navigation)
        s.show()

    }


//    OCR functionality
    fun showPdfOcrDialog(document: Document) {

        if (!document.isCropped) {

        }

        //        Check if the play services are installed first:
        if (!Helper.checkPlayServices(this))
            showNoPlayServicesDialog(document)
        else
            showOCRAlertDialog(document)

    }

    private fun showNoPlayServicesDialog(document: Document) {

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

    private fun createPdf(document: Document, withOCR: Boolean) {

        if (withOCR)
            createPdfWithOCR(document)
        else
            createPdf(document)

    }

    private fun showOCRAlertDialog(document: Document) {

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
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text)
        ) { dialog, which -> alertDialog.cancel() }
        alertDialog.show()

    }


    override fun onSheetSelected(sheetAction: ActionSheet.SheetAction) {

    }

    override fun onShown() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDismiss() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startCamera(view: View) {

        Helper.startCameraActivity(this)
        finish()

    }

    fun newDocument(view: View) {

        startActivity(Intent(applicationContext, CreateDocumentActivity::class.java))

    }

    fun openDocumentOptions(view: View) {

        selectedDocument?.title?.let { showDocumentOptions(selectedDocument!!) }

    }

    override fun onDocumentOptions(document: Document) {

        showDocumentOptions(document)

    }

    override fun getSelfNavDrawerItem(): NavigationDrawer.NavigationItemEnum {
        return NavigationDrawer.NavigationItemEnum.DOCUMENTS
    }

    private fun showDocumentOptions(document: Document) {

        val sheetActions = ArrayList<ActionSheet.SheetAction>()
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_continue_item,
                getString(R.string.action_document_continue_document),
                R.drawable.ic_add_a_photo_black_24dp))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_edit_item,
                getString(R.string.action_document_edit_document),
                R.drawable.ic_title_black_24dp))
//        This options are just available if the document contains at least one image:
        if (!document.pages.isEmpty()) {
            sheetActions.add(ActionSheet.SheetAction(R.id.action_document_crop_item,
                    getString(R.string.action_document_crop_title),
                    R.drawable.ic_crop_black_24dp))
            sheetActions.add(ActionSheet.SheetAction(R.id.action_document_pdf_item,
                    getString(R.string.action_document_pdf_title),
                    R.drawable.ic_baseline_picture_as_pdf_24px))
            sheetActions.add(ActionSheet.SheetAction(R.id.action_document_upload_item,
                    getString(R.string.action_document_upload_document),
                    R.drawable.ic_cloud_upload_black_24dp))
        }
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_delete_item,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp))

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

    private var selectedDocument: Document? = null

    override fun onDocumenOpened(document: Document) {
        selectedDocument = document
//        Opens the ImagesFragment:
        bottomNavigationView.selectedItemId = R.id.viewer_images
    }

    override fun onSelectionChange() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var bottomNavigationView: BottomNavigationView

    /**
     * Shows the new floating action button and hides any previous fab. If newFABID is -1 all fab's
     * are hidden.
     */
    private fun showFAB(newFABID: Int) {

        var newFAB: FloatingActionButton? = null
        if (newFABID != -1)
            newFAB = findViewById(newFABID)

        if (newFABID == R.id.viewer_edit_fab || newFABID == -1) {
            val addFab: FloatingActionButton = findViewById(R.id.viewer_add_fab)
            if (addFab.visibility == View.VISIBLE)
                addFab.hide()
        }
        if (newFABID == R.id.viewer_add_fab || newFABID == -1) {
            val editFab: FloatingActionButton = findViewById(R.id.viewer_edit_fab)
            if (editFab.visibility == View.VISIBLE)
                editFab.hide()
        }
//        Show the button if it is assigned:
        newFAB?.show()

    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

//        Do nothing in case the item is already selected:
        if (menuItem.itemId == bottomNavigationView.selectedItemId)
            return false

        when(menuItem.itemId) {
            R.id.viewer_documents -> {
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.setCustomAnimations(R.anim.translate_right_to_left_in,
                        R.anim.translate_right_to_left_out)

//                ft.setCustomAnimations(R.anim.translate_left_to_right, R.anim.translate_right_to_left)
                ft.replace(R.id.viewer_fragment_layout, DocumentsFragment(this),
                        DocumentsFragment.TAG).commit()

                showFAB(R.id.viewer_add_fab)

                selectableToolbar.setTitle(getText(R.string.document_navigation_documents))
//                supportActionBar?.setTitle(R.string.document_navigation_documents)


            }
            R.id.viewer_images -> {

//                The user just clicked images tab and did not open any document:
                if (selectedDocument == null)
                    selectedDocument = DocumentStorage.getInstance(this).activeDocument

                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()

//                The animation depends on the position of the selected item:
                if (bottomNavigationView.selectedItemId == R.id.viewer_documents)
                    ft.setCustomAnimations(R.anim.translate_left_to_right_in,
                            R.anim.translate_left_to_right_out)
                else
                    ft.setCustomAnimations(R.anim.translate_right_to_left_in,
                            R.anim.translate_right_to_left_out)

                ft.replace(R.id.viewer_fragment_layout, ImagesFragment(selectedDocument),
                        ImagesFragment.TAG).commit()

                showFAB(R.id.viewer_edit_fab)
                selectableToolbar.setTitle(selectedDocument?.title)

            }
            R.id.viewer_pdfs -> {

//                Hide any badge if existing:
                bottomNavigationView.removeBadge(R.id.viewer_pdfs)

                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.setCustomAnimations(R.anim.translate_left_to_right_in,
                        R.anim.translate_left_to_right_out)
                ft.replace(R.id.viewer_fragment_layout, PdfFragment()).commit()

                showFAB(-1)

                selectableToolbar.setTitle(getText(R.string.document_navigation_pdfs))
//                supportActionBar?.setTitle(R.string.document_navigation_pdfs)


            }
            else -> return false
        }

        //        Expand the AppBarLayout without any animation
        findViewById<AppBarLayout>(R.id.gallery_appbar).setExpanded(true, false)

        return true

    }

    private fun initToolbar() {

        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        val appBarLayout: AppBarLayout = findViewById(R.id.gallery_appbar)

        selectableToolbar = SelectableToolbar(this, toolbar, appBarLayout)
        //        Enable back navigation in action bar:
        setSupportActionBar(toolbar)
        selectableToolbar.setTitle(getText(R.string.document_navigation_documents))

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        bottomNavigationView = findViewById(R.id.viewer_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val documentsFragment = DocumentsFragment(this)
        documentsFragment.scrollToActiveDocument()

//        Open the DocumentsFragment first / as default view:
        supportFragmentManager.beginTransaction().replace(R.id.viewer_fragment_layout,
                documentsFragment, DocumentsFragment.TAG).commit()

        // Register to receive messages:
        messageReceiver = getReceiver()
        val filter = IntentFilter()
        filter.addAction(INTENT_UPLOAD_ACTION)
        filter.addAction(INTENT_IMAGE_PROCESS_ACTION)
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter)

        initToolbar()

////        use this if you start the app with ViewerActivity:
//        initContext(this)
////        initToolbar()

    }

    override fun onPause() {
        super.onPause()
        DocumentStorage.saveJSON(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }

    private fun getReceiver(): BroadcastReceiver {

        return object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {

                Log.d(TAG, "onReceive: " + intent)

                when (intent?.action) {
                    INTENT_IMAGE_PROCESS_ACTION -> {
                        val defValue = -1
                        when (intent.getIntExtra(INTENT_IMAGE_PROCESS_TYPE, defValue)) {
//                            TODO: this works only in material theme!
                            INTENT_PDF_PROCESS_FINISHED -> bottomNavigationView.getOrCreateBadge(R.id.viewer_pdfs)
                            INTENT_IMAGE_PROCESS_FINISHED -> {
                                val fileName = intent.getStringExtra(INTENT_FILE_NAME)
                                updateGallery(fileName)
                            }
                        }
                    }
                    INTENT_UPLOAD_ACTION -> {

                        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
                            (this as DocumentsFragment).resetAdapter()
                        }

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

        Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG).show()

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
        Snackbar.make(findViewById(R.id.sync_coordinatorlayout),
                snackbarText, Snackbar.LENGTH_LONG).show()


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

    private fun updateGallery(fileName: String) {

        Log.d(TAG, "updateGallery: " + fileName)

        //        Update the UI:
        supportFragmentManager.findFragmentByTag(ImagesFragment.TAG)?.apply {
            if ((this as ImagesFragment).isVisible) {
//                update the ui:
                this.updateGallery(fileName)
            }
        }

    }

}