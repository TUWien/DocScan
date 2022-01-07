package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.SheetAction
import at.ac.tuwien.caa.docscan.databinding.FragmentPdfsBinding
import at.ac.tuwien.caa.docscan.extensions.bindVisible
import at.ac.tuwien.caa.docscan.extensions.getExportFolderPermissionIntent
import at.ac.tuwien.caa.docscan.extensions.shareFile
import at.ac.tuwien.caa.docscan.extensions.showFile
import at.ac.tuwien.caa.docscan.logic.appendExportFile
import at.ac.tuwien.caa.docscan.logic.extractExportFile
import at.ac.tuwien.caa.docscan.ui.base.BaseFragment
import at.ac.tuwien.caa.docscan.ui.dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PdfFragment : BaseFragment() {

    private val viewModel: PdfViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()
    private val modalSheetViewModel: ModalActionSheetViewModel by viewModel()

    private lateinit var binding: FragmentPdfsBinding
    private lateinit var adapter: PdfAdapter

    private val folderPermissionResultCallback =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    it.data?.data?.let { uri ->
                        viewModel.persistFolderUri(uri)
                    }
                }
            }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentPdfsBinding.inflate(layoutInflater)
        adapter = PdfAdapter(select = {
            when (it) {
                is ExportList.ExportHeader -> {
                    // ignore
                }
                is ExportList.File -> {
                    showFile(requireActivity(), it.pageFileType, it.documentFile)
                }
            }
        }, options = {
            when (it) {
                is ExportList.ExportHeader -> {
                    // ignore
                }
                is ExportList.File -> {
                    showOptions(it)
                }
            }
        })
        binding.pdfList.adapter = adapter
        binding.pdfList.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }

    private fun observe() {
        viewModel.observableExportModel.observe(viewLifecycleOwner, {
            when (it) {
                is ExportModel.MissingPermissions -> {
                    binding.pdfList.bindVisible(false)
                    binding.pdfEmptyLayout.bindVisible(false)
                    binding.pdfNoPermission.bindVisible(true)
                    adapter.submitList(listOf())
                    showDialog(ADialog.DialogAction.EXPORT_FOLDER_PERMISSION)
                }
                is ExportModel.Success -> {
                    binding.pdfList.bindVisible(it.exportEntries.isNotEmpty())
                    binding.pdfEmptyLayout.bindVisible(it.exportEntries.isEmpty())
                    binding.pdfNoPermission.bindVisible(false)
                    adapter.submitList(it.exportEntries)
                }
            }
        })
        dialogViewModel.observableDialogAction.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { dialogResult ->
                when (dialogResult.dialogAction) {
                    ADialog.DialogAction.EXPORT_FOLDER_PERMISSION -> {
                        if (dialogResult.isPositive()) {
                            folderPermissionResultCallback.launch(getExportFolderPermissionIntent(requireContext()))
                        }
                    }
                    ADialog.DialogAction.DELETE_PDF -> {
                        if (dialogResult.isPositive()) {
                            dialogResult.arguments.extractExportFile()?.let { file ->
                                viewModel.deleteFile(file)
                            }
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
        modalSheetViewModel.observableSheetAction.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { result ->
                when (result.pressedSheetAction.id) {
                    R.id.action_pdf_delete_item -> {
                        // TODO: Add title of document file into the dialog title
                        showDialog(DialogModel(ADialog.DialogAction.DELETE_PDF, arguments = Bundle().appendExportFile(result.arguments.extractExportFile())))
                    }
                    R.id.action_pdf_share_item -> {
                        result.arguments.extractExportFile()?.let { file ->
                            shareFile(requireActivity(), file.pageFileType, file.documentFile)
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun showOptions(export: ExportList.File) {

        val sheetActions = ArrayList<SheetAction>()

        sheetActions.add(
                SheetAction(
                        R.id.action_pdf_share_item,
                        getString(R.string.action_pdf_share),
                        R.drawable.ic_share_black_24dp
                )
        )

        sheetActions.add(
                SheetAction(
                        R.id.action_pdf_delete_item,
                        getString(R.string.action_document_delete_document),
                        R.drawable.ic_delete_black_24dp
                )
        )

        SheetModel(sheetActions, Bundle().appendExportFile(export)).show(childFragmentManager)
    }
}
