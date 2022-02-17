package at.ac.tuwien.caa.docscan.ui.docviewer.images

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.GalleryItemBinding
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.GlideHelper
import at.ac.tuwien.caa.docscan.logic.calculateImageResolution
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.roundToInt

class ImagesAdapter(
    private val onClick: (PageSelection) -> Unit,
    private val onLongClick: (PageSelection) -> Unit,
    screenWidth: Int,
    private val columnCount: Int,
    private val paddingPx: Int,
    private val marginPx: Int
) : ListAdapter<PageSelection, ImagesAdapter.ImageViewHolder>(DiffPageCallback()) {

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
            val isProcessing = page.page.postProcessingState == PostProcessingState.PROCESSING
            // set click listeners
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)

            // TODO: Consider calling this in the viewModel on an IO thread
            val aspectRatio = fileHandler.getFileByPage(page.page)?.let {
                calculateImageResolution(it, page.page.rotation).aspectRatio
            } ?: .0

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
            val isCropped = page.page.postProcessingState == PostProcessingState.DONE
            binding.pageProgressbar.visibility = if (isProcessing) View.VISIBLE else View.INVISIBLE
            binding.pageCheckbox.isEnabled = page.isSelectionActivated
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
                if (page.isSelectionActivated) View.VISIBLE else View.GONE
            binding.pageContainer.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (page.isSelectionActivated) R.color.colorSelectLight else R.color.white
                )
            )
            val params = binding.pageImageview.layoutParams as RelativeLayout.LayoutParams
            params.setMargins(
                if (page.isSelectionActivated) marginPx else 0,
                if (page.isSelectionActivated) marginPx else 0,
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
        return isPrimaryEqual(oldItem, newItem) && isSecondaryEqual(oldItem, newItem)
    }

    /**
     * See [at.ac.tuwien.caa.docscan.ui.docviewer.documents.DocumentAdapter] for the same function
     * on why this is necessary.
     */
    override fun getChangePayload(oldItem: PageSelection, newItem: PageSelection): Any? {
        return when {
            isPrimaryEqual(
                oldItem,
                newItem
            ) && !isSecondaryEqual(oldItem, newItem) -> Any()
            else -> null
        }
    }

    /**
     * The primary equal check for which the default animation of the recyclerview is always applied
     * if it's not equal.
     */
    private fun isPrimaryEqual(
        oldItem: PageSelection,
        newItem: PageSelection
    ): Boolean {
        return oldItem.page.fileHash == newItem.page.fileHash
    }

    /**
     * In contrast to [isPrimaryEqual] these changes won't trigger a default animation.
     */
    private fun isSecondaryEqual(
        oldItem: PageSelection,
        newItem: PageSelection
    ): Boolean {
        return oldItem.isSelected == newItem.isSelected &&
                oldItem.isSelectionActivated == newItem.isSelectionActivated &&
                oldItem.page.singlePageBoundary == newItem.page.singlePageBoundary &&
                oldItem.page.postProcessingState == newItem.page.postProcessingState
    }
}
