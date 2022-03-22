package at.ac.tuwien.caa.docscan.ui.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import at.ac.tuwien.caa.docscan.BuildConfig
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityPageSlideBinding
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.asDocumentPageExtra
import at.ac.tuwien.caa.docscan.extensions.shareFile
import at.ac.tuwien.caa.docscan.logic.PageFileType
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.crop.CropViewActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.dialog.isPositive
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import at.ac.tuwien.caa.docscan.ui.segmentation.SegmentationActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

class PageSlideActivity : BaseNoNavigationActivity(), PageImageView.SingleClickListener {

    private lateinit var binding: ActivityPageSlideBinding
    private val viewModel: PageSlideViewModel by viewModel { parametersOf(intent.extras!!) }
    private val dialogViewModel: DialogViewModel by viewModel()
    private val adapter = PageSlideAdapter()

    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateTitle(
                binding.slideViewpager.currentItem, viewModel.observablePages.value?.first?.size
                    ?: 0
            )
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "EXTRA_DOCUMENT_ID"
        const val EXTRA_SELECTED_PAGE_ID = "EXTRA_SELECTED_PAGE_ID"

        @JvmStatic
        fun newInstance(context: Context, docId: UUID, selectedPageId: UUID?): Intent {
            return Intent(context, PageSlideActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DOCUMENT_ID, docId)
                putExtra(EXTRA_SELECTED_PAGE_ID, selectedPageId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageSlideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWithTitle(null)

        // Close the fragment if the user hits the back button in the toolbar:
        binding.mainToolbar.apply {
            setNavigationOnClickListener { onBackPressed() }
            setOnClickListener {
                viewModel.navigateToDocumentViewer(binding.slideViewpager.currentItem)
            }
        }

        binding.slideViewpager.adapter = adapter
        initButtons()
        observe()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.page_slide_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.all_images -> {
                viewModel.navigateToDocumentViewer(binding.slideViewpager.currentItem)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observe() {
        viewModel.observablePages.observe(this, {
            // if the document doesn't even contain a single image, then this view needs to be closed.
            if(it.first.isEmpty()) {
                finish()
                return@observe
            }
            binding.slideViewpager.unregisterOnPageChangeCallback(callback)
            adapter.setItems(it.first)
            // scroll only if a valid position has been requested
            if (it.second != -1) {
                binding.slideViewpager.currentItem = it.second
            }
            updateTitle(binding.slideViewpager.currentItem, it.first.size)
            binding.slideViewpager.registerOnPageChangeCallback(callback)
        })
        viewModel.observableInitCrop.observe(this, {
            it.getContentIfNotHandled()?.let { page ->
                navigateToCropActivity(page)
                // TODO: Checkout how to handle zoom out transitions.
                // TODO: Zoom out of scale for this specific image view

                //                    Zoom out before opening the CropViewActivity:
//                PageImageView imageView = mPagerAdapter.getCurrentFragment().getImageView();
//                SubsamplingScaleImageView.AnimationBuilder ab = imageView.animateScale(0);
////            I am not sure, why this is happening, but it happened once on MotoG3
//                if (ab == null) {
//                    startCropView();
//                    return;
//                }
//
//                imageView.animateScale(0).withOnAnimationEventListener(new SubsamplingScaleImageView.OnAnimationEventListener() {
//                    @Override
//                    public void onComplete() {
//                        startCropView();
//
//                    }
//
//                    @Override
//                    public void onInterruptedByUser() {
//                        startCropView();
//                    }
//
//                    @Override
//                    public void onInterruptedByNewAnim() {
//                        startCropView();
//                    }
//                }).start();
            }
        })
        viewModel.observableInitRetake.observe(this, {
            it.getContentIfNotHandled()?.let { pair ->
                startActivity(CameraActivity.newInstance(this, pair.first, pair.second))
            }
        })
        viewModel.observableSharePage.observe(this, {
            it.getContentIfNotHandled()?.let { uri ->
                shareFile(this, PageFileType.JPEG, uri)
            }
        })
        viewModel.observableInitSegmentation.observe(this, {
            it.getContentIfNotHandled()?.let { page ->
                startActivity(SegmentationActivity.newInstance(this, page))
            }
        })
        viewModel.observableInitDocumentViewer.observe(this, {
            it.getContentIfNotHandled()?.let { page ->
                startActivity(DocumentViewerActivity.newInstance(this, page.asDocumentPageExtra()))
            }
        })
        viewModel.observableError.observe(this, {
            it.getContentIfNotHandled()?.let { throwable ->
                throwable.handleError(this, logAsWarning = true)
            }
        })
        dialogViewModel.observableDialogAction.observe(this, {
            it.getContentIfNotHandled()?.let { dialogResult ->
                when (dialogResult.dialogAction) {
                    ADialog.DialogAction.CONFIRM_DELETE_PAGE -> {
                        if (dialogResult.isPositive()) {
                            viewModel.deletePageAtPosition(binding.slideViewpager.currentItem)
                        }
                    }
                    ADialog.DialogAction.CONFIRM_RETAKE_IMAGE -> {
                        if (dialogResult.isPositive()) {
                            viewModel.retakeImageAtPosition(binding.slideViewpager.currentItem)
                        }
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
    }

    private fun initButtons() {
        if (BuildConfig.DEBUG) {
            binding.pageViewButtons.debugSegmentation.visibility = View.VISIBLE
        }
        binding.pageViewButtons.pageViewButtonsLayoutDeleteButton.setOnClickListener {
            showDialog(ADialog.DialogAction.CONFIRM_DELETE_PAGE)
        }
        binding.pageViewButtons.pageViewButtonsLayoutCropButton.setOnClickListener {
            viewModel.cropPageAtPosition(binding.slideViewpager.currentItem)
        }
        binding.pageViewButtons.pageViewButtonsLayoutRotateButton.setOnClickListener {
            showDialog(ADialog.DialogAction.CONFIRM_RETAKE_IMAGE)
        }
        binding.pageViewButtons.pageViewButtonsLayoutShareButton.setOnClickListener {
            viewModel.shareImageAtPosition(binding.slideViewpager.currentItem)
        }
        binding.pageViewButtons.pageViewButtonsLayoutSegmentation.setOnClickListener {
            viewModel.debugSegmentation(binding.slideViewpager.currentItem)
        }
    }

    private fun updateTitle(current: Int, size: Int) {
        supportActionBar?.title = "${(current + 1)}" + "/" + size
    }

    private fun navigateToCropActivity(page: Page) {
        startActivity(CropViewActivity.newInstance(this, page))
    }

    private inner class PageSlideAdapter(private var pages: MutableList<Page> = mutableListOf()) :
        FragmentStateAdapter(this) {
        override fun getItemCount() = pages.size

        override fun getItemId(position: Int): Long {
            return pages[position].id.hashCode().toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return pages.any { it.id.hashCode().toLong() == itemId }
        }

        override fun createFragment(position: Int) =
            ImageViewerFragment.newInstance(pages[position].id)

        fun setItems(pages: List<Page>) {
            val callback = PagerDiffUtil(this.pages, pages)
            val diff = DiffUtil.calculateDiff(callback)
            this.pages.clear()
            this.pages.addAll(pages)
            diff.dispatchUpdatesTo(this)
        }
    }

    private inner class PagerDiffUtil(
        private val oldList: List<Page>,
        private val newList: List<Page>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // as long as the ids are ok, we do not provide any changes, because the changes to the file are taken care of the fragment.
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
    }

    override fun onSingleClick() {
        if (binding.mainToolbar.visibility == View.VISIBLE) {
            binding.mainToolbar.visibility = View.INVISIBLE
            binding.pageViewButtons.pageViewButtonsLayout.visibility = View.INVISIBLE
        } else {
            binding.mainToolbar.visibility = View.VISIBLE
            binding.pageViewButtons.pageViewButtonsLayout.visibility = View.VISIBLE
        }
    }
}
