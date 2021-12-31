package at.ac.tuwien.caa.docscan.ui.docviewer.images

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.FragmentImagesBinding
import at.ac.tuwien.caa.docscan.logic.computeScreenWidth
import at.ac.tuwien.caa.docscan.ui.base.BaseFragment
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerViewModel
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ImagesFragment : BaseFragment() {

    companion object {
        // this is dynamically determined before the adapter is created
        private var DYNAMIC_SCREEN_WIDTH = 0
        private var COLUMN_COUNT = 2
    }

    private val args: ImagesFragmentArgs by navArgs()
    private val viewModel: ImagesViewModel by viewModel()

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
                mode.menuInflater.inflate(R.menu.images_selected_menu, menu)
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
                        viewModel.deleteAllSelectedPages()
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
        // TODO: add here more columns for landscape mode:
        binding.imagesList.layoutManager = GridLayoutManager(context, COLUMN_COUNT)
        binding.imagesList.adapter = imagesAdapter
        viewModel.loadDocumentPagesById(args.documentPage?.docId, args.documentPage?.pageId)
        observe()
    }

    private fun observe() {
        viewModel.observablePages.observe(viewLifecycleOwner, {
            imagesAdapter.submitList(it.pages)
            if (it.pages.isEmpty()) {
                binding.imagesList.visibility = View.INVISIBLE
                binding.imagesEmptyLayout.visibility = View.VISIBLE
                binding.emptyMessage.text = if (it.document == null) {
                    "No active document\nPlease select a document first"
                } else {
                    "No images for document\nPlease take an image first"
                }
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
        })
        viewModel.observableDocWithPages.observe(viewLifecycleOwner, {
            setTitle(it?.document?.title ?: getString(R.string.document_navigation_images))
            // this is important to call to inform the sharedViewModel about the document
            // that is displayed here.
            sharedViewModel.informAboutImageViewer(it)
        })
        viewModel.observableInitGallery.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { page ->
                startActivity(PageSlideActivity.newInstance(requireActivity(), page.docId, page.id))
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

// TODO: delete images dialog
//    private fun deleteImagesDialog(imgFragment: ImagesFragment, selCount: Int) {
//
//        val alertDialogBuilder = AlertDialog.Builder(this)
//
//        val prefix = resources.getString(R.string.gallery_confirm_delete_title_prefix)
//        val postfix =
//            if (selCount == 1)
//                resources.getString(R.string.gallery_confirm_delete_images_title_single_postfix)
//            else
//                resources.getString(R.string.gallery_confirm_delete_images_title_multiple_postfix)
//        val title = "$prefix $selCount $postfix"
//
//        // set dialog message
//        alertDialogBuilder
//            .setMessage(R.string.gallery_confirm_delete_text)
//            .setTitle(title)
//            .setPositiveButton(R.string.gallery_confirm_delete_confirm_button_text) { dialogInterface, i ->
//                deleteImages(
//                    imgFragment
//                )
//            }
//            .setNegativeButton(R.string.gallery_confirm_delete_cancel_button_text, null)
//            .setCancelable(true)
//
//        // create alert dialog
//        val alertDialog = alertDialogBuilder.create()
//
//        // show it
//        alertDialog.show()
//
//    }


// TODO: handle transitions
//    private fun openImagesView(document: DocumentWithPages) {
//
//        val imagesFragment = setupImagesFragment(document)
//
//        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
//        //                The animation depends on the position of the selected item:
//        if (binding.bottomNav.selectedItemId == R.id.viewer_documents)
//            ft.setCustomAnimations(
//                R.anim.translate_left_to_right_in,
//                R.anim.translate_left_to_right_out
//            )
//        else
//            ft.setCustomAnimations(
//                R.anim.translate_right_to_left_in,
//                R.anim.translate_right_to_left_out
//            )
//
//////                Create the shared element transition:
////        supportFragmentManager.findFragmentByTag(DocumentsFragment.TAG)?.apply {
////            if ((this as DocumentsFragment).isVisible) {
//////          Check if the document has pages, otherwise use no shared element transition:
////                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && selectedDocument?.pages?.isNotEmpty()!!) {
////                    setExitTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.fade))
////                    imagesFragment.postponeEnterTransition()
////                    imagesFragment.sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
////                    var imageView = getImageView(document)
////                    ft.addSharedElement(imageView!!, imageView!!.transitionName)
////                    ft.setReorderingAllowed(true)
////                }
////            }
////        }
//
////        ft.replace(
////            R.id.viewer_fragment_layout, imagesFragment,
////            ImagesFragment.TAG
////        ).commit()

}
