package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.FragmentImagesBinding
import at.ac.tuwien.caa.docscan.logic.computeScreenWidth
import at.ac.tuwien.caa.docscan.ui.gallery.newPackage.PageSlideActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class ImagesFragment : BaseFragment() {

    companion object {
        val TAG = "ImagesFragment"
        private var DYNAMIC_SCREEN_WIDTH = 0
        private var COLUMN_COUNT = 2
    }

    private val args: ImagesFragmentArgs by navArgs()
    private val viewModel: ImagesViewModel by viewModel()

    private val sharedViewModel: DocumentViewerViewModel by sharedViewModel()

    private lateinit var binding: FragmentImagesBinding
    private lateinit var imagesAdapter: ImagesAdapterNew

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
        imagesAdapter = ImagesAdapterNew(
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
        viewModel.loadDocumentPagesById(args.documentPage?.docId)
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
                    it.scrollTo = -1
                    binding.imagesList.scrollToPosition(it.scrollTo)
                }
            }

        })
        viewModel.observableDoc.observe(viewLifecycleOwner, {
            setTitle(it.title)
        })
        viewModel.observableInitGallery.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { page ->
                startActivity(PageSlideActivity.newInstance(requireActivity(), page.docId, page.id))
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.images_menu, menu)
    }
}
