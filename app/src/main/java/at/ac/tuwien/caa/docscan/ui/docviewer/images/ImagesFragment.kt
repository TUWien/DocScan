package at.ac.tuwien.caa.docscan.ui.docviewer.images

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.FragmentImagesBinding
import at.ac.tuwien.caa.docscan.logic.ConsumableEvent
import at.ac.tuwien.caa.docscan.logic.computeScreenWidth
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.ui.base.BaseFragment
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.isPositive
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ImagesFragment : BaseFragment() {

    companion object {
        // this is dynamically determined before the adapter is created
        private var DYNAMIC_SCREEN_WIDTH = 0

        // TODO: add here more columns when landscape mode is used
        private var COLUMN_COUNT = 2
    }

    private val args: ImagesFragmentArgs by navArgs()
    private val viewModel: ImagesViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()

    private val sharedViewModel: DocumentViewerViewModel by sharedViewModel()

    private lateinit var binding: FragmentImagesBinding
    private lateinit var imagesAdapter: ImagesAdapter
    private var actionMode: ActionMode? = null

    private val actionModeCallback: ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(
                mode: ActionMode,
                menu: Menu
            ): Boolean {
                mode.menuInflater.inflate(R.menu.images_action_menu, menu)
                return true
            }

            override fun onPrepareActionMode(
                mode: ActionMode,
                menu: Menu
            ): Boolean {
                return false
            }

            override fun onActionItemClicked(
                mode: ActionMode,
                item: MenuItem
            ): Boolean {
                return when (item.itemId) {
                    R.id.rotate -> {
                        viewModel.rotateAllSelectedPages()
                        true
                    }
                    R.id.select_all -> {
                        viewModel.setSelectedForAll(true)
                        true
                    }
                    R.id.delete -> {
                        viewModel.deleteAllSelectedPages(force = false)
                        true
                    }
                    else -> {
                        false
                    }
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                viewModel.setSelectedForAll(false)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DYNAMIC_SCREEN_WIDTH = computeScreenWidth(requireActivity())
        imagesAdapter = ImagesAdapter(
            {
                viewModel.clickOnItem(it)
            },
            {
                viewModel.longClickOnItem(it)
            },
            DYNAMIC_SCREEN_WIDTH,
            COLUMN_COUNT,
            resources.getDimensionPixelSize(R.dimen.images_padding),
            resources.getDimensionPixelSize(R.dimen.images_selected_margin)
        )
        binding.imagesList.layoutManager = GridLayoutManager(context, COLUMN_COUNT)
        binding.imagesList.adapter = imagesAdapter
        viewModel.loadDocumentPagesById(args.documentPage?.docId, args.documentPage?.pageId)
        observe()
    }

    private fun observe() {
        viewModel.observablePages.observe(viewLifecycleOwner) {
            imagesAdapter.submitList(it.pages)
            if (it.pages.isEmpty()) {
                binding.imagesList.visibility = View.INVISIBLE
                binding.imagesEmptyLayout.visibility = View.VISIBLE
                binding.emptyMessage.text = getString(
                    if (it.document == null) {
                        R.string.images_no_active_document
                    } else {
                        R.string.images_no_images
                    }
                )
            } else {
                binding.imagesList.visibility = View.VISIBLE
                binding.imagesEmptyLayout.visibility = View.INVISIBLE
                if (it.scrollTo != -1) {
                    val scrollToTmp = it.scrollTo
                    it.scrollTo = -1
                    binding.imagesList.layoutManager?.scrollToPosition(scrollToTmp)
                }
            }
            val result = it.pages.isSelectionActivated()
            actionBarSelection(result.second)
            sharedViewModel.setSelectedElements(result.second)
        }
        viewModel.observableDocWithPages.observe(viewLifecycleOwner) {
            setTitle(it?.document?.title ?: getString(R.string.document_navigation_images))
            // this is important to call to inform the sharedViewModel about the document
            // that is displayed here.
            sharedViewModel.informAboutImageViewer(it)
        }
        viewModel.observableInitGallery.observe(viewLifecycleOwner, ConsumableEvent { page ->
            startActivity(PageSlideActivity.newInstance(requireActivity(), page.docId, page.id))
        })
        viewModel.observableError.observe(viewLifecycleOwner, ConsumableEvent { throwable ->
            throwable.handleError(requireBaseActivity(), logAsWarning = true)
        })
        viewModel.observableConfirmDelete.observe(viewLifecycleOwner, ConsumableEvent { pageCount ->
            val title = resources.getQuantityString(R.plurals.confirm_delete_pages, pageCount)
            val message = resources.getQuantityString(
                R.plurals.confirm_delete_pages_with_count,
                pageCount,
                pageCount
            )
            val dialogModel = DialogModel(
                ADialog.DialogAction.CONFIRM_DELETE_PAGES,
                customTitle = title,
                customMessage = message
            )
            showDialog(dialogModel)
        })
        dialogViewModel.observableDialogAction.observe(
            viewLifecycleOwner,
            ConsumableEvent { result ->
                when (result.dialogAction) {
                    ADialog.DialogAction.CONFIRM_DELETE_PAGES -> {
                        if (result.isPositive()) {
                            viewModel.deleteAllSelectedPages(force = true)
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            })
    }

    override fun onPause() {
        super.onPause()
        if (isRemoving) {
            actionMode?.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.images_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.image_options -> {
                viewModel.observableDocWithPages.value?.let {
                    sharedViewModel.initDocumentOptions(it)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun actionBarSelection(selectedItems: Int) {
        if (selectedItems == 0) {
            actionMode?.finish()
        } else {
            if (actionMode == null) {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            }
            actionMode?.title =
                selectedItems.toString() + " " + getString(R.string.gallery_selected)
        }
    }
}
