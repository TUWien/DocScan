package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.SheetAction
import at.ac.tuwien.caa.docscan.databinding.FragmentPdfsBinding
import at.ac.tuwien.caa.docscan.extensions.*
import at.ac.tuwien.caa.docscan.logic.LinearLayoutManagerWithSmoothScroller
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
        setHasOptionsMenu(true)
        binding = FragmentPdfsBinding.inflate(layoutInflater)
        adapter = PdfAdapter(select = {
            when (it) {
                is ExportList.ExportHeader -> {
                    // ignore
                }
                is ExportList.File -> {
                    viewModel.openFile(it)
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
        binding.pdfList.layoutManager = LinearLayoutManagerWithSmoothScroller(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.exports_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.switch_folder -> {
                viewModel.launchFolderSelection(folderPermissionResultCallback)
            }
        }
        return super.onOptionsItemSelected(item)
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
                    if (it.scrollToTop) {
                        binding.pdfList.postDelayed(
                            { binding.pdfList.scrollToPositionIfNotVisible(0) },
                            300
                        )
                    }
                }
            }
        })
        viewModel.observableOpenFile.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {
                showFile(requireActivity(), it.pageFileType, it.file.documentFile)
            }
        })
        dialogViewModel.observableDialogAction.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { dialogResult ->
                when (dialogResult.dialogAction) {
                    ADialog.DialogAction.EXPORT_FOLDER_PERMISSION -> {
                        if (dialogResult.isPositive()) {
                            viewModel.launchFolderSelection(folderPermissionResultCallback)
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
                    SheetActionId.DELETE.id -> {
                        val exportFile = result.arguments.extractExportFile()
                        showDialog(
                            DialogModel(
                                ADialog.DialogAction.DELETE_PDF,
                                customTitle = getString(R.string.viewer_delete_pdf_title) + " ${exportFile?.name}?",
                                arguments = Bundle().appendExportFile(result.arguments.extractExportFile())
                            )
                        )
                    }
                    SheetActionId.SHARE.id -> {
                        result.arguments.extractExportFile()?.let { file ->
                            shareFile(requireActivity(), file.pageFileType, file.file.documentFile.uri)
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
        DocumentContractNotifier.observableDocumentContract.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {
                viewModel.load(scrollToTop = true)
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
                SheetActionId.SHARE.id,
                getString(R.string.action_pdf_share),
                R.drawable.ic_share_black_24dp
            )
        )

        sheetActions.add(
            SheetAction(
                SheetActionId.DELETE.id,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp
            )
        )

        SheetModel(sheetActions, Bundle().appendExportFile(export)).show(childFragmentManager)
    }
}
