package at.ac.tuwien.caa.docscan.ui.docviewer

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.GalleryItemBinding
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.GlideHelper
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.math.roundToInt

class ImagesAdapterNew(
    private val onClick: (PageSelection) -> Unit,
    private val onLongClick: (PageSelection) -> Unit,
    screenWidth: Int,
    private val columnCount: Int,
    private val paddingPx: Int,
    private val marginPx: Int
) :
    ListAdapter<PageSelection, ImagesAdapterNew.ImageViewHolder>(DiffPageCallback()) {

    private val itemWidth = screenWidth / columnCount
    private val fileHandler: FileHandler by inject(FileHandler::class.java)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(GalleryItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ImageViewHolder(val binding: GalleryItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

        fun bind(page: PageSelection, position: Int) {

            // set click listeners
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)

            val aspectRatio = aspectRatioForIndex(page.page)
            val topView = binding.pageContainer
            // set item height based on ratio
            topView.layoutParams.height = if (aspectRatio != .0) {
                (itemWidth / aspectRatio).roundToInt()
            } else {
                itemWidth
            }
            // set item width to the calculate width
            topView.layoutParams.width = itemWidth

            // set paddings
            when {
                (position % columnCount) == 0 -> {
                    topView.setPadding(0, paddingPx, paddingPx, paddingPx)
                }
                (position % columnCount) == (columnCount - 1) -> {
                    topView.setPadding(paddingPx, paddingPx, 0, paddingPx)
                }
                else -> {
                    topView.setPadding(paddingPx / 2, paddingPx, paddingPx / 2, paddingPx)
                }
            }

            // set image view
            binding.pageImageview.transitionName = page.page.id.toString()
            val isProcessing = page.page.postProcessingState == PostProcessingState.PROCESSING
            val isCropped = page.page.postProcessingState == PostProcessingState.DONE
            binding.pageProgressbar.visibility = if (isProcessing) View.VISIBLE else View.INVISIBLE
            // TODO: Why is this even true for this state?
            binding.pageCheckbox.isEnabled = isProcessing

            GlideHelper.loadPageIntoImageView(
                page.page,
                binding.pageImageview,
                if (isCropped) GlideHelper.GlideStyles.IMAGE_CROPPED else GlideHelper.GlideStyles.IMAGES_UNCROPPED
            )

            // set checkbox
            if (page.isSelectionActivated) {
                binding.pageCheckbox.isChecked = page.isSelected
            }
            binding.pageCheckbox.visibility =
                if (binding.pageCheckbox.isChecked) View.VISIBLE else View.GONE
            binding.pageContainer.setBackgroundColor(
                if (binding.pageCheckbox.isChecked) binding.root.resources.getColor(
                    R.color.colorSelectLight
                ) else binding.root.resources.getColor(R.color.white)
            )
            val params = binding.pageImageview.layoutParams as RelativeLayout.LayoutParams
            params.setMargins(
                if (binding.pageCheckbox.isChecked) marginPx else 0,
                if (binding.pageCheckbox.isChecked) marginPx else 0,
                0,
                0
            )
        }

        override fun onClick(view: View) {
            onClick(getItem(adapterPosition))
        }

        override fun onLongClick(view: View): Boolean {
            onLongClick(getItem(adapterPosition))
            return true
        }
    }

    // TODO: This seems to be very heavy for the UI thread, consider adding this into the DB
    private fun aspectRatioForIndex(page: Page): Double {
        val file = fileHandler.getFileByPage(page) ?: return .0
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            var width = options.outWidth
            var height = options.outHeight
            val rotation = page.rotation
            if (rotation.angle == 90 || rotation.angle == 270) {
                val tmp = width
                width = height
                height = tmp
            }
            return width / height.toDouble()
        } catch (e: Exception) {
            Timber.e("Could not determine aspect ratio", e)
        }
        return .0
    }
}

class DiffPageCallback : DiffUtil.ItemCallback<PageSelection>() {
    override fun areItemsTheSame(oldItem: PageSelection, newItem: PageSelection): Boolean {
        return oldItem.page.id == newItem.page.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: PageSelection,
        newItem: PageSelection
    ): Boolean {
        // TODO: Check if this is sufficient
        return oldItem.page.id == newItem.page.id && oldItem.page.number == newItem.page.number && oldItem.isSelected == newItem.isSelected
    }
}