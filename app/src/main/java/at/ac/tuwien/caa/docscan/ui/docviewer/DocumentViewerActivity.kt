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
import at.ac.tuwien.caa.docscan.db.model.isUploaded
import at.ac.tuwien.caa.docscan.extensions.SnackbarOptions
import at.ac.tuwien.caa.docscan.logic.*
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

/**
 * The main document viewer which consists of multiple fragments using the navigation component.
 */
class DocumentViewerActivity : BaseNavigationActivity(), View.OnClickListener {

    companion object {

        // TODO: can be relocated to the PDF only
        const val DOCUMENT_PDF_SELECTION_REQUEST = 2
        const val PERSISTABLE_URI_PERMISSION = 3

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
        observe()
    }

    private fun observe() {
        viewModel.selectedScreen.observe(this, { screen ->
            // do not update the FABs since this will be already updated in this case.
            if ((viewModel.observableNumOfSelectedElements.value ?: 0) > 0) {
                return@observe
            }
            handleFABVisibilityForScreen(screen)
        })
        viewModel.observableInitDocumentOptions.observe(this, {
            it.getContentIfNotHandled()?.let { doc ->
                showDocumentOptions(doc)
            }
        })
        viewModel.observableNumOfSelectedElements.observe(this, {
            if (it > 0) {
                // the first argument doesn't matter in this case
                handleFABVisibilityForScreen(DocumentViewerScreen.DOCUMENTS, false)
            } else {
                handleFABVisibilityForScreen(viewModel.selectedScreen.value!!)
            }
        })
        viewModel.observableInitCamera.observe(this, {
            it?.getContentIfNotHandled()?.let {
                startActivity(CameraActivity.newInstance(this))
                finish()
            }
        })
        viewModel.observableResourceAction.observe(this, {
            it.getContentIfNotHandled()?.let { pair ->
                when (val resource = pair.second) {
                    is Failure -> {
                        when (pair.first) {
                            DocumentAction.DELETE -> {
                                resource.exception.handleError(this)
                            }
                            DocumentAction.EXPORT -> {
                                resource.exception.handleError(this)
                            }
                            DocumentAction.CROP -> {
                                resource.exception.handleError(this)
                            }
                            DocumentAction.UPLOAD -> {
                                resource.exception.getDocScanDBError()?.let { dbError ->
                                    if (dbError.code == DBErrorCode.DOCUMENT_ALREADY_UPLOADED) {
                                        // TODO: Append the document extra here, otherwise the dialog will be ignored!
                                        showDialog(ADialog.DialogAction.DOCUMENT_ALREADY_UPLOADED)
                                        return@observe
                                    }
                                }
                            }
                        }
                    }
                    is Success -> {
                        // TODO: Add translated string for successful action!
                        singleSnackbar(
                            binding.syncCoordinatorlayout,
                            SnackbarOptions(
                                pair.first.name + " has been successfully initiated!",
                                Snackbar.LENGTH_LONG
                            )
                        )
                    }
                }
            }
        })

        viewModel.observableResourceConfirmation.observe(this, {
            it.getContentIfNotHandled()?.let { pair ->
                when (pair.first) {
                    DocumentAction.DELETE -> {
                        showDialog(
                            ADialog.DialogAction.CONFIRM_DELETE_DOCUMENT.with(
                                customTitle = "${getString(R.string.sync_confirm_delete_title)} " + pair.second.document.title,
                                arguments = Bundle().appendDocWithPages(pair.second)
                            )
                        )
                    }
                    DocumentAction.EXPORT -> {
                        showDialog(
                            ADialog.DialogAction.CONFIRM_OCR_SCAN.with(
                                customTitle = "${getString(R.string.gallery_confirm_ocr_title)} " + pair.second.document.title,
                                arguments = Bundle().appendDocWithPages(pair.second)
                            )
                        )
                    }
                    DocumentAction.CROP -> {
                        showDialog(
                            ADialog.DialogAction.CONFIRM_DOCUMENT_CROP_OPERATION.with(
                                customTitle = "${getString(R.string.viewer_crop_confirm_title)} " + pair.second.document.title,
                                arguments = Bundle().appendDocWithPages(pair.second)
                            )
                        )
                    }
                    DocumentAction.UPLOAD -> {
                        showDialog(
                            ADialog.DialogAction.CONFIRM_UPLOAD.with(
                                arguments = Bundle().appendDocWithPages(pair.second)
                            )
                        )
                    }
                }
            }
        })

        dialogViewModel.observableDialogAction.observe(this, {
            it.getContentIfNotHandled()?.let { result ->
                if (result.isPositive()) {
                    when (result.dialogAction) {
                        ADialog.DialogAction.CONFIRM_DOCUMENT_CROP_OPERATION -> {
                            result.arguments.extractDocWithPages()?.let { doc ->
                                viewModel.applyActionFor(true, DocumentAction.CROP, doc)
                            }
                        }
                        ADialog.DialogAction.CONFIRM_DELETE_DOCUMENT -> {
                            result.arguments.extractDocWithPages()?.let { doc ->
                                viewModel.applyActionFor(true, DocumentAction.DELETE, doc)
                            }
                        }
                        ADialog.DialogAction.CONFIRM_OCR_SCAN -> {
                            result.arguments.extractDocWithPages()?.let { doc ->
                                viewModel.applyActionFor(true, DocumentAction.EXPORT, doc)
                            }
                        }
                        ADialog.DialogAction.CONFIRM_UPLOAD -> {
                            result.arguments.extractDocWithPages()?.let { doc ->
                                viewModel.applyActionFor(true, DocumentAction.UPLOAD, doc)
                            }
                        }
                        ADialog.DialogAction.DOCUMENT_ALREADY_UPLOADED -> {
                            result.arguments.extractDocWithPages()?.let { doc ->
                                viewModel.applyActionFor(
                                    true,
                                    DocumentAction.UPLOAD,
                                    doc,
                                    forceAction = true
                                )
                            }
                        }
                        else -> {
                            // ignore
                        }
                    }
                }
            }
        })
        modalSheetViewModel.observableSheetAction.observe(this, {
            it.getContentIfNotHandled()?.let { result ->
                when (result.pressedSheetAction.id) {
                    R.id.action_document_continue_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            viewModel.startImagingWith(doc.document.id)
                        } ?: kotlin.run {
                            // Check if this is ok to call
                            viewModel.startImagingWith(null)
                        }
                    }
                    R.id.action_document_pdf_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            viewModel.applyActionFor(
                                action = DocumentAction.EXPORT,
                                forceAction = false,
                                documentWithPages = doc
                            )
                        }
                        // TODO: EXPORT_LOGIC - Add (PDF) export logic to the viewModel.
//                        if (KtHelper.isPdfFolderPermissionGiven(this)) {
////                    if (document.isCropped())
////                        showPdfOcrDialog(document)
////                    else
////                        showNotCropDialog(document, false)
//                        } else {
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
////                        showDirectoryPermissionRequiredAlert()
//                            }
//                        }
                    }
                    R.id.action_document_edit_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            startActivity(
                                EditDocumentActivity.newInstance(
                                    this,
                                    document = doc.document
                                )
                            )
                        }
                    }
                    R.id.action_document_delete_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            viewModel.applyActionFor(
                                action = DocumentAction.DELETE,
                                documentWithPages = doc
                            )
                        }
                    }
                    R.id.action_document_crop_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            viewModel.applyActionFor(
                                action = DocumentAction.CROP,
                                documentWithPages = doc
                            )
                        }
                    }
                    R.id.action_document_upload_item -> {
                        result.arguments.extractDocWithPages()?.let { doc ->
                            viewModel.applyActionFor(
                                isConfirmed = false,
                                action = DocumentAction.UPLOAD,
                                documentWithPages = doc
                            )
                        }
                    }
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
                startActivityForResult(
                    Intent(
                        applicationContext,
                        SelectPdfDocumentActivity::class.java
                    ), DOCUMENT_PDF_SELECTION_REQUEST
                )
            }
            R.id.viewer_upload_fab -> {
                viewModel.uploadSelectedDocument()
            }
            R.id.viewer_camera_fab -> {
                viewModel.startImagingWith()
            }
            R.id.viewer_gallery_fab -> {
                // TODO: CODE_STYLE - Encapsulate this in a helper class.
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                galleryResultCallback.launch(intent)
            }
        }
    }

    private fun showDocumentOptions(document: DocumentWithPages) {

        val sheetActions = ArrayList<SheetAction>()
        sheetActions.add(
            SheetAction(
                R.id.action_document_continue_item,
                getString(R.string.action_document_continue_document),
                R.drawable.ic_add_a_photo_black_24dp
            )
        )
        sheetActions.add(
            SheetAction(
                R.id.action_document_edit_item,
                getString(R.string.action_document_edit_document),
                R.drawable.ic_edit_black_24dp
            )
        )
//        This options are just available if the document contains at least one image:
        if (document.pages.isNotEmpty()) {
            sheetActions.add(
                SheetAction(
                    R.id.action_document_crop_item,
                    getString(R.string.action_document_crop_title),
                    R.drawable.ic_transform_black_24dp
                )
            )
            sheetActions.add(
                SheetAction(
                    R.id.action_document_pdf_item,
                    getString(R.string.action_document_pdf_title),
                    R.drawable.ic_baseline_picture_as_pdf_24px
                )
            )
            if (!document.isUploaded())
                sheetActions.add(
                    SheetAction(
                        R.id.action_document_upload_item,
                        getString(R.string.action_document_upload_document),
                        R.drawable.ic_cloud_upload_black_24dp
                    )
                )
            else
                sheetActions.add(
                    SheetAction(
                        R.id.action_document_upload_item,
                        getString(R.string.action_document_upload_document),
                        R.drawable.ic_cloud_upload_gray_24dp
                    )
                )
        }
        sheetActions.add(
            SheetAction(
                R.id.action_document_delete_item,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp
            )
        )

        SheetModel(sheetActions, Bundle().appendDocWithPages(document)).show(supportFragmentManager)
    }
}
