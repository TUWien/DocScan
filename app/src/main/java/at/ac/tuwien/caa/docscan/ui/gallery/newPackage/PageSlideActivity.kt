package at.ac.tuwien.caa.docscan.ui.gallery.newPackage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityPageSlideBinding
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

class PageSlideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPageSlideBinding
    private val viewModel: PageSlideViewModel by viewModel { parametersOf(intent.extras!!) }
    private val adapter = PageSlideAdapter()

    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateTitle(
                binding.slideViewpager.currentItem,
                viewModel.observablePages.value?.first?.size ?: 0
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

        //       Take care that the mToolbar is not overlaid by the status bar:
        binding.imageViewerToolbar.setPadding(0, getStatusBarHeight(), 0, 0)
        // TODO: Check if the navigation listeners are necessary

        binding.slideViewpager.adapter = adapter
        initButtons()
        observe()
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
            if (it.second != -1) {
                binding.slideViewpager.currentItem = it.second
            }
            updateTitle(binding.slideViewpager.currentItem, it.first.size)
            binding.slideViewpager.registerOnPageChangeCallback(callback)
        })
        viewModel.observableInitCrop.observe(this, {
            it.getContentIfNotHandled()?.let { page ->
                // TODO: Start cropping activity
                // TODO: Zoom out of scale for this specific image view
            }
        })
        viewModel.observableInitRetake.observe(this, {
            it.getContentIfNotHandled()?.let { pair ->
                startActivity(CameraActivity.newInstance(this, pair.first, pair.second))
            }
        })
    }

    // TODO: Init buttons!
    private fun initButtons() {
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
    }

//    private void initButtons() {
//
//        mButtonsLayout = findViewById(R.id.page_view_buttons_layout);
//        //       Take care that the mButtonsLayout is not overlaid by the navigation bar:
//        //       mButtonsLayout.setPadding(0, 0, 0, getNavigationBarHeight());
//
//        //        initGalleryButton();
//        initCropButton();
//        initDeleteButton();
//        initRotateButton();
//        initShareButton();
//        initSegmentationButton();
//    }

    private fun updateTitle(current: Int, size: Int) {
        supportActionBar?.title = "${(current + 1)}" + "/" + size
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
}
