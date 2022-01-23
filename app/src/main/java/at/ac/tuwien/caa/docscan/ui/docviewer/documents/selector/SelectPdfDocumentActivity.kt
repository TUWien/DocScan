package at.ac.tuwien.caa.docscan.ui.docviewer.documents.selector

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityPdfSelectorBinding
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogButton
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.isPositive
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectPdfDocumentActivity : BaseNoNavigationActivity() {

    private val viewModel: SelectDocumentViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()
    private lateinit var binding: ActivityPdfSelectorBinding
    private lateinit var adapter: SelectDocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWithTitle(R.string.select_document_title)
        adapter = SelectDocumentAdapter(clickListener = { documentWithPages ->
            viewModel.export(documentWithPages, Bundle())
        })
        binding.documentsList.adapter = adapter
        binding.documentsList.layoutManager = LinearLayoutManager(this)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.observableDocuments.observe(this, {
            adapter.submitList(it)
        })
        viewModel.observableSelectionResource.observe(this, { model ->
            when (val resource = model.resource) {
                is Failure -> {
                    resource.exception.getDocScanDBError()?.let { dbError ->
                        if (dbError.code == DBErrorCode.DOCUMENT_NOT_CROPPED) {
                            showDialog(
                                ADialog.DialogAction.EXPORT_WARNING_IMAGE_CROP_MISSING,
                                model.arguments
                            )
                            return@observe
                        }
                    }
                    resource.exception.getDocScanIOError()?.let { dbError ->
                        if (dbError.ioErrorCode == IOErrorCode.EXPORT_GOOGLE_PLAYSTORE_NOT_INSTALLED_FOR_OCR) {
                            showDialog(
                                ADialog.DialogAction.EXPORT_WARNING_OCR_NOT_AVAILABLE,
                                model.arguments
                            )
                            return@observe
                        }
                    }
                    resource.exception.handleError(this)
                }
                is Success -> {
                    finish()
                }
            }
        })
        viewModel.observableConfirmExport.observe(this, {
            it.getContentIfNotHandled()?.let { bundle ->
                showDialog(ADialog.DialogAction.CONFIRM_OCR_SCAN, arguments = bundle)
            }
        })
        dialogViewModel.observableDialogAction.observe(this, {
            it.getContentIfNotHandled()?.let { dialogResult ->
                when (dialogResult.dialogAction) {
                    ADialog.DialogAction.CONFIRM_OCR_SCAN -> {
                        when (dialogResult.pressedAction) {
                            DialogButton.POSITIVE -> {
                                viewModel.export(
                                    dialogResult.arguments.appendIsConfirmed(true)
                                        .appendUseOCR(true)
                                )
                            }
                            DialogButton.NEGATIVE -> {
                                viewModel.export(
                                    dialogResult.arguments.appendIsConfirmed(true)
                                        .appendUseOCR(false)
                                )
                            }
                            DialogButton.NEUTRAL -> {
                                // ignore
                            }
                        }
                    }
                    ADialog.DialogAction.EXPORT_WARNING_OCR_NOT_AVAILABLE -> {
                        if (dialogResult.isPositive()) {
                            viewModel.export(
                                dialogResult.arguments.appendUseOCR(false)
                            )
                        }
                    }
                    ADialog.DialogAction.EXPORT_WARNING_IMAGE_CROP_MISSING -> {
                        if (dialogResult.isPositive()) {
                            viewModel.export(
                                dialogResult.arguments.appendSkipCropRestriction(true)
                            )
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
    }
}
