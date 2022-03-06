package at.ac.tuwien.caa.docscan.ui.docviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.SheetAction
import at.ac.tuwien.caa.docscan.databinding.ActivityDocumentViewerBinding
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.isUploadInProgress
import at.ac.tuwien.caa.docscan.db.model.isUploadScheduled
import at.ac.tuwien.caa.docscan.db.model.isUploaded
import at.ac.tuwien.caa.docscan.extensions.SnackbarOptions
import at.ac.tuwien.caa.docscan.extensions.getImageImportIntent
import at.ac.tuwien.caa.docscan.extensions.shareFile
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.ui.account.TranskribusLoginActivity
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.dialog.*
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity
import at.ac.tuwien.caa.docscan.ui.document.EditDocumentActivity
import at.ac.tuwien.caa.docscan.ui.docviewer.documents.DocumentsFragmentDirections
import at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector.SelectPdfDocumentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.OpenCVLoader
import timber.log.Timber
import java.net.HttpURLConnection

/**
 * The main document viewer which consists of multiple fragments using the navigation component.
 */
class DocumentViewerActivity : BaseNavigationActivity(), View.OnClickListener {

    companion object {

        private const val EXTRA_DOCUMENT_PAGE = "EXTRA_DOCUMENT_PAGE"
        private const val EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW = "EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW"

        init {
//         We need this for Android 4:
            //         We need this for Android 4:
            if (!OpenCVLoader.initDebug()) {
                Timber.d("Error while initializing OpenCV.")
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
        fun newInstance(
            context: Context,
            documentViewerLaunchViewType: DocumentViewerLaunchViewType = DocumentViewerLaunchViewType.DOCUMENTS
        ): Intent {
            return Intent(context, DocumentViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW, documentViewerLaunchViewType)
            }
        }
    }

    override val selfNavDrawerItem = NavigationDrawer.NavigationItem.DOCUMENTS

    private lateinit var binding: ActivityDocumentViewerBinding
    private val viewModel: DocumentViewerViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()
    private val modalSheetViewModel: ModalActionSheetViewModel by viewModel()

    private val galleryResultCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.clipData?.let { clipData ->
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                    viewModel.addNewImages(uris)
                } ?: run {
                    it.data?.data?.let { uriFile ->
                        viewModel.addNewImages(listOf(uriFile))
                    }
                }
            }
        }

    private val topLevelDestinations = setOf(
        R.id.viewer_documents,
        R.id.viewer_images,
        R.id.viewer_pdfs
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar.mainToolbar)

        binding.viewerAddFab.setOnClickListener(this)
        binding.viewerAddPdfFab.setOnClickListener(this)
        binding.viewerUploadFab.setOnClickListener(this)
        binding.viewerCameraFab.setOnClickListener(this)
        binding.viewerGalleryFab.setOnClickListener(this)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations, binding.drawerLayout)
        binding.mainToolbar.mainToolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemReselectedListener {
            // ignore the reselection, otherwise the fragment would be recreated again.
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.viewer_documents -> {
                    viewModel.changeScreen(DocumentViewerScreen.DOCUMENTS)
                }
                R.id.viewer_images -> {
                    viewModel.changeScreen(DocumentViewerScreen.IMAGES)
                }
                R.id.viewer_pdfs -> {
                    viewModel.changeScreen(DocumentViewerScreen.PDFS)
                }
                else -> {
                    // ignore
                }
            }
        }

        val documentPage = intent.getParcelableExtra<DocumentPage>(EXTRA_DOCUMENT_PAGE)
        val type =
            intent.getSerializableExtra(EXTRA_DOCUMENT_VIEWER_LAUNCH_VIEW) as? DocumentViewerLaunchViewType

        // a navigation case when user wants to directly open the images fragment
        if (documentPage != null) {
            navController.navigate(
                DocumentsFragmentDirections.actionViewerDocumentsToViewerImages(documentPage)
            )
        } else {
            when (type) {
                DocumentViewerLaunchViewType.PDFS -> {
                    navController.navigate(DocumentsFragmentDirections.actionViewerDocumentsToViewerPdfs())
                }
                else -> {
                    // ignore
                }
            }
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.selectedScreen.observe(this) { screen ->
            // do not update the FABs since this will be already updated in this case.
            if ((viewModel.observableNumOfSelectedElements.value ?: 0) > 0) {
                return@observe
            }
            handleFABVisibilityForScreen(screen)
        }
        viewModel.observableInitDocumentOptions.observe(this, ConsumableEvent {
            showDocumentOptions(it)
        })
        viewModel.observableNewExportCount.observe(this) {
            val badge = binding.bottomNav.getOrCreateBadge(R.id.viewer_pdfs)
            badge.isVisible = it > 0
            badge.number = it
        }
        viewModel.observableNumOfSelectedElements.observe(this) {
            if (it > 0) {
                // the first argument doesn't matter in this case
                handleFABVisibilityForScreen(DocumentViewerScreen.DOCUMENTS, false)
            } else {
                handleFABVisibilityForScreen(viewModel.selectedScreen.value!!)
            }
        }
        viewModel.observableInitCamera.observe(this, ConsumableEvent {
            startActivity(CameraActivity.newInstance(this, false))
            finish()
        })
        viewModel.observableResourceAction.observe(this, ConsumableEvent { model ->
            when (val resource = model.resource) {
                is Failure -> {
                    when (model.action) {
                        DocumentAction.DELETE, DocumentAction.SHARE -> {
                            resource.exception.handleError(this)
                        }
                        DocumentAction.EXPORT -> {
                            resource.exception.getDocScanDBError()?.let { dbError ->
                                if (dbError.code == DBErrorCode.DOCUMENT_NOT_CROPPED) {
                                    showDialog(
                                        ADialog.DialogAction.EXPORT_WARNING_IMAGE_CROP_MISSING,
                                        model.arguments
                                    )
                                    return@ConsumableEvent
                                }
                            }
                            resource.exception.getDocScanIOError()?.let { dbError ->
                                if (dbError.ioErrorCode == IOErrorCode.EXPORT_GOOGLE_PLAYSTORE_NOT_INSTALLED_FOR_OCR) {
                                    showDialog(
                                        ADialog.DialogAction.EXPORT_WARNING_OCR_NOT_AVAILABLE,
                                        model.arguments
                                    )
                                    return@ConsumableEvent
                                } else if (dbError.ioErrorCode == IOErrorCode.EXPORT_FILE_MISSING_PERMISSION) {
                                    binding.bottomNav.selectedItemId = R.id.viewer_pdfs
                                    return@ConsumableEvent
                                }
                            }
                            resource.exception.handleError(this)
                        }
                        DocumentAction.CROP -> {
                            resource.exception.handleError(this)
                        }
                        DocumentAction.UPLOAD -> {
                            resource.exception.getDocScanDBError()?.let { dbError ->
                                when (dbError.code) {
                                    DBErrorCode.DOCUMENT_ALREADY_UPLOADED -> {
                                        showDialog(
                                            ADialog.DialogAction.UPLOAD_WARNING_DOC_ALREADY_UPLOADED,
                                            model.arguments
                                        )
                                        return@ConsumableEvent
                                    }
                                    DBErrorCode.DOCUMENT_NOT_CROPPED -> {
                                        showDialog(
                                            ADialog.DialogAction.UPLOAD_WARNING_IMAGE_CROP_MISSING,
                                            model.arguments
                                        )
                                        return@ConsumableEvent
                                    }
                                    else -> {
                                        resource.exception.handleError(this)
                                        return@ConsumableEvent
                                    }
                                }
                            }
                            resource.exception.getDocScanTranskribusRESTError()?.let { restError ->
                                when (restError) {
                                    is DocScanError.TranskribusRestError.HttpError -> {
                                        if (restError.httpStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                            showDialog(ADialog.DialogAction.UPLOAD_FAILED_UNAUTHORIZED)
                                            return@ConsumableEvent
                                        }
                                    }
                                    is DocScanError.TranskribusRestError.IOError -> {
                                        // ignore
                                    }
                                }
                            }
                            resource.exception.handleError(this)
                        }
                        DocumentAction.CANCEL_UPLOAD -> {
                            resource.exception.handleError(this)
                        }
                    }
                }
                is Success<*> -> {
                    if (model.action == DocumentAction.SHARE) {
                        model.resource.applyOnSuccess { any ->
                            @Suppress("UNCHECKED_CAST")
                            (any as? List<Uri>?)?.let { uris ->
                                shareFile(this, PageFileType.JPEG, uris)
                            }
                        }
                        return@ConsumableEvent
                    }
                    if (!model.action.showSuccessMessage) {
                        return@ConsumableEvent
                    }
                    val name = when (model.action) {
                        DocumentAction.DELETE, DocumentAction.CROP, DocumentAction.SHARE, DocumentAction.CANCEL_UPLOAD -> ""
                        DocumentAction.EXPORT -> getString(R.string.operation_export)
                        DocumentAction.UPLOAD -> getString(R.string.operation_upload)
                    }
                    singleSnackbar(
                        binding.syncCoordinatorlayout,
                        SnackbarOptions(
                            String.format(
                                getString(R.string.viewer_document_operation_success),
                                name
                            ),
                            Snackbar.LENGTH_LONG
                        )
                    )
                }
            }
        })

        viewModel.observableResourceConfirmation.observe(this, ConsumableEvent { model ->
            when (model.action) {
                DocumentAction.DELETE -> {
                    showDialog(
                        ADialog.DialogAction.CONFIRM_DELETE_DOCUMENT.with(
                            customTitle = "${getString(R.string.sync_confirm_delete_title)} " + model.documentWithPages.document.title,
                            arguments = model.arguments
                        )
                    )
                }
                DocumentAction.EXPORT -> {
                    showDialog(
                        ADialog.DialogAction.CONFIRM_OCR_SCAN.with(
                            customTitle = "${getString(R.string.gallery_confirm_ocr_title)} " + model.documentWithPages.document.title,
                            arguments = model.arguments
                        )
                    )
                }
                DocumentAction.CROP -> {
                    showDialog(
                        ADialog.DialogAction.CONFIRM_DOCUMENT_CROP_OPERATION.with(
                            customTitle = "${getString(R.string.viewer_crop_confirm_title)} " + model.documentWithPages.document.title,
                            arguments = model.arguments
                        )
                    )
                }
                DocumentAction.UPLOAD -> {
                    showDialog(
                        ADialog.DialogAction.CONFIRM_UPLOAD.with(
                            arguments = model.arguments
                        )
                    )
                }
                DocumentAction.CANCEL_UPLOAD -> {
                    showDialog(
                        ADialog.DialogAction.CONFIRM_CANCEL_UPLOAD.with(
                            arguments = model.arguments
                        )
                    )
                }
                DocumentAction.SHARE -> {
                    // Share doesn't need a confirmation
                }
            }
        })

        dialogViewModel.observableDialogAction.observe(this, ConsumableEvent { result ->
            when (result.dialogAction) {
                ADialog.DialogAction.CONFIRM_DOCUMENT_CROP_OPERATION -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.CROP,
                            result.arguments.appendIsConfirmed(true)
                        )
                    }
                }
                ADialog.DialogAction.CONFIRM_DELETE_DOCUMENT -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.DELETE,
                            result.arguments.appendIsConfirmed(true)
                        )
                    }
                }
                ADialog.DialogAction.CONFIRM_OCR_SCAN -> {
                    when (result.pressedAction) {
                        DialogButton.POSITIVE -> {
                            viewModel.applyActionFor(
                                DocumentAction.EXPORT,
                                result.arguments.appendIsConfirmed(true).appendUseOCR(true)
                            )
                        }
                        DialogButton.NEGATIVE -> {
                            viewModel.applyActionFor(
                                DocumentAction.EXPORT,
                                result.arguments.appendIsConfirmed(true).appendUseOCR(false)
                            )
                        }
                        DialogButton.NEUTRAL -> {
                            // ignore
                        }
                    }
                }
                ADialog.DialogAction.CONFIRM_UPLOAD -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.UPLOAD,
                            result.arguments.appendIsConfirmed(true)
                        )
                    }
                }
                ADialog.DialogAction.CONFIRM_CANCEL_UPLOAD -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.CANCEL_UPLOAD,
                            result.arguments.appendIsConfirmed(true)
                        )
                    }
                }
                ADialog.DialogAction.UPLOAD_WARNING_DOC_ALREADY_UPLOADED -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.UPLOAD,
                            result.arguments.appendSkipAlreadyUploadedRestriction(true)
                        )
                    }
                }
                ADialog.DialogAction.EXPORT_WARNING_IMAGE_CROP_MISSING -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.EXPORT,
                            result.arguments.appendSkipCropRestriction(true)
                        )
                    }
                }
                ADialog.DialogAction.UPLOAD_WARNING_IMAGE_CROP_MISSING -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.UPLOAD,
                            result.arguments.appendSkipCropRestriction(true)
                        )
                    }
                }
                ADialog.DialogAction.EXPORT_WARNING_OCR_NOT_AVAILABLE -> {
                    if (result.isPositive()) {
                        viewModel.applyActionFor(
                            DocumentAction.EXPORT,
                            result.arguments.appendUseOCR(false)
                        )
                    }
                }
                ADialog.DialogAction.UPLOAD_FAILED_UNAUTHORIZED -> {
                    if (result.isPositive()) {
                        startActivity(TranskribusLoginActivity.newInstance(this))
                    }
                }
                else -> {
                    // ignore
                }
            }
        })
        modalSheetViewModel.observableSheetAction.observe(this, ConsumableEvent { result ->
            when (result.pressedSheetAction.id) {
                SheetActionId.CONTINUE_IMAGING.id -> {
                    result.arguments.extractDocWithPages()?.let { doc ->
                        viewModel.startImagingWith(doc.document.id)
                    } ?: kotlin.run {
                        // Check if this is ok to call
                        viewModel.startImagingWith(null)
                    }
                }
                SheetActionId.EXPORT.id -> {
                    viewModel.applyActionFor(DocumentAction.EXPORT, result.arguments)
                }
                SheetActionId.EDIT.id -> {
                    result.arguments.extractDocWithPages()?.let { doc ->
                        startActivity(
                            EditDocumentActivity.newInstance(
                                this,
                                document = doc.document
                            )
                        )
                    }
                }
                SheetActionId.DELETE.id -> {
                    viewModel.applyActionFor(DocumentAction.DELETE, result.arguments)
                }
                SheetActionId.CROP.id -> {
                    viewModel.applyActionFor(DocumentAction.CROP, result.arguments)
                }
                SheetActionId.UPLOAD.id -> {
                    viewModel.applyActionFor(DocumentAction.UPLOAD, result.arguments)
                }
                SheetActionId.CANCEL_UPLOAD.id -> {
                    viewModel.applyActionFor(DocumentAction.CANCEL_UPLOAD, result.arguments)
                }
                SheetActionId.SHARE.id -> {
                    viewModel.applyActionFor(DocumentAction.SHARE, result.arguments)
                }
            }
        })
    }

    override fun onBackPressed() {
        // close the drawer first if it's opened
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.close()
            return
        }
        when (binding.bottomNav.selectedItemId) {
//            Open the CameraActivity:
            R.id.viewer_documents -> {
                super.onBackPressed()
            }
//            Open the DocumentsFragment:
            R.id.viewer_pdfs -> binding.bottomNav.selectedItemId = R.id.viewer_documents
//            Special case for ImagesFragment:
            R.id.viewer_images -> {
                binding.bottomNav.selectedItemId = R.id.viewer_documents
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleFABVisibilityForScreen(screen: DocumentViewerScreen, force: Boolean? = null) {
        setFABVisibility(
            binding.viewerGalleryFab,
            force ?: BuildConfig.DEBUG && screen == DocumentViewerScreen.IMAGES
        )
        setFABVisibility(
            binding.viewerAddFab,
            force ?: screen == DocumentViewerScreen.DOCUMENTS
        )
        setFABVisibility(
            binding.viewerAddPdfFab,
            force ?: screen == DocumentViewerScreen.PDFS
        )
        setFABVisibility(
            binding.viewerUploadFab,
            force ?: screen == DocumentViewerScreen.IMAGES
        )
        setFABVisibility(
            binding.viewerCameraFab,
            force ?: true
        )
    }

    private fun setFABVisibility(fab: FloatingActionButton, isVisible: Boolean) {
        if (isVisible) {
            fab.show()
        } else {
            fab.hide()
        }
    }

    override fun onClick(p0: View) {
        when (p0.id) {
            R.id.viewer_add_fab -> {
                startActivity(CreateDocumentActivity.newInstance(this, null))
            }
            R.id.viewer_add_pdf_fab -> {
                startActivity(
                    Intent(
                        applicationContext,
                        SelectPdfDocumentActivity::class.java
                    )
                )
            }
            R.id.viewer_upload_fab -> {
                viewModel.uploadSelectedDocument()
            }
            R.id.viewer_camera_fab -> {
                viewModel.startImagingWith()
            }
            R.id.viewer_gallery_fab -> {
                galleryResultCallback.launch(getImageImportIntent())
            }
        }
    }

    private fun showDocumentOptions(document: DocumentWithPages) {

        val sheetActions = ArrayList<SheetAction>()
        sheetActions.add(
            SheetAction(
                SheetActionId.CONTINUE_IMAGING.id,
                getString(R.string.action_document_continue_document),
                R.drawable.ic_add_a_photo_black_24dp
            )
        )
        sheetActions.add(
            SheetAction(
                SheetActionId.EDIT.id,
                getString(R.string.action_document_edit_document),
                R.drawable.ic_edit_black_24dp
            )
        )
//        This options are just available if the document contains at least one image:
        if (document.pages.isNotEmpty()) {
            sheetActions.add(
                SheetAction(
                    SheetActionId.CROP.id,
                    getString(R.string.action_document_crop_title),
                    R.drawable.ic_transform_black_24dp
                )
            )
            sheetActions.add(
                SheetAction(
                    SheetActionId.EXPORT.id,
                    getString(R.string.action_document_pdf_title),
                    R.drawable.ic_baseline_picture_as_pdf_24px
                )
            )
            val sheetActionUpload = when {
                document.isUploaded() -> {
                    SheetAction(
                        SheetActionId.UPLOAD.id,
                        getString(R.string.action_document_upload_document),
                        R.drawable.ic_cloud_upload_black_24dp
                    )
                }
                document.isUploadInProgress() || document.isUploadScheduled() -> {
                    SheetAction(
                        SheetActionId.CANCEL_UPLOAD.id,
                        getString(R.string.action_document_cancel_upload_document),
                        R.drawable.ic_cloud_off_black_24dp
                    )
                }
                else -> {
                    SheetAction(
                        SheetActionId.UPLOAD.id,
                        getString(R.string.action_document_upload_document),
                        R.drawable.ic_baseline_cloud_upload_24
                    )
                }
            }
            sheetActions.add(sheetActionUpload)
        }
        sheetActions.add(
            SheetAction(
                SheetActionId.DELETE.id,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp
            )
        )

        if (document.pages.isNotEmpty()) {
            sheetActions.add(
                SheetAction(
                    SheetActionId.SHARE.id,
                    getString(R.string.action_share),
                    R.drawable.ic_share_black_24dp
                )
            )
        }

        SheetModel(sheetActions, Bundle().appendDocWithPages(document)).show(supportFragmentManager)
    }
}
