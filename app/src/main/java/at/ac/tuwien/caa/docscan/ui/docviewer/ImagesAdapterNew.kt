package at.ac.tuwien.caa.docscan.ui.docviewer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.databinding.GalleryItemBinding
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page

class ImagesAdapterNew(
    private val onClick: (PageSelection) -> Unit,
    private val onLongClick: (PageSelection) -> Unit
) :
    ListAdapter<PageSelection, ImagesAdapterNew.ImageViewHolder>(DiffPageCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(GalleryItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(val binding: GalleryItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

        fun bind(page: PageSelection) {
            // TODO: Apply domain to style
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
        // TODO: Check if this is sufficient
        return oldItem.page.id == newItem.page.id && oldItem.page.number == newItem.page.number && oldItem.isSelected == newItem.isSelected
    }
}