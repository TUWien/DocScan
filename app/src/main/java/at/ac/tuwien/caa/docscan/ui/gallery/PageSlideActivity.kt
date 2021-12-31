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
import at.ac.tuwien.caa.docscan.logic.PageFileType
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.crop.CropViewActivity
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import at.ac.tuwien.caa.docscan.ui.segmentation.SegmentationActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

// TODO: Checkout how to handle zoom out transitions.
// TODO: CONSTRAINT - only delete/retake if document is not locked.
class PageSlideActivity : BaseActivity(), PageImageView.SingleClickListener {

    private lateinit var binding: ActivityPageSlideBinding
    private val viewModel: PageSlideViewModel by viewModel { parametersOf(intent.extras!!) }
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

        setSupportActionBar(binding.imageViewerToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Take care that the mToolbar is not overlaid by the status bar:
        binding.imageViewerToolbar.setPadding(0, getStatusBarHeight(), 0, 0)

        // Close the fragment if the user hits the back button in the toolbar:
        binding.imageViewerToolbar.apply {
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

    // A method to find height of the status bar
    //    Based on: https://gist.github.com/hamakn/8939eb68a920a6d7a498
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun observe() {
        viewModel.observablePages.observe(this, {
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
            it.getContentIfNotHandled()?.let { pair ->
                navigateToCropActivity(pair.second)
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
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(uri, contentResolver.getType(uri))
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = PageFileType.JPEG.mimeType
                }
                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.page_slide_fragment_share_choose_app_text)
                    )
                )
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
    }

    private fun initButtons() {
        if (BuildConfig.DEBUG) {
            binding.pageViewButtons.debugSegmentation.visibility = View.VISIBLE
        }
        binding.pageViewButtons.pageViewButtonsLayoutDeleteButton.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(R.string.page_slide_fragment_confirm_delete_text)
            builder.setPositiveButton(
                R.string.page_slide_fragment_confirm_delete_text
            ) { _, _ ->
                viewModel.deletePageAtPosition(binding.slideViewpager.currentItem)
            }
            builder.show()
        }
        binding.pageViewButtons.pageViewButtonsLayoutCropButton.setOnClickListener {
            viewModel.cropPageAtPosition(binding.slideViewpager.currentItem)
        }
        binding.pageViewButtons.pageViewButtonsLayoutRotateButton.setOnClickListener {
            viewModel.retakeImageAtPosition(binding.slideViewpager.currentItem)
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
        if (binding.imageViewerToolbar.visibility == View.VISIBLE) {
            binding.imageViewerToolbar.visibility = View.INVISIBLE
            binding.pageViewButtons.pageViewButtonsLayout.visibility = View.INVISIBLE
        } else {
            binding.imageViewerToolbar.visibility = View.VISIBLE
            binding.pageViewButtons.pageViewButtonsLayout.visibility = View.VISIBLE
        }
    }
}
