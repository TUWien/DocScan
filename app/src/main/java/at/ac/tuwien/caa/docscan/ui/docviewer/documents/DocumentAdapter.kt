package at.ac.tuwien.caa.docscan.ui.docviewer.documents

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.DocumentRowLayoutBinding
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.extensions.bindInvisible
import at.ac.tuwien.caa.docscan.logic.GlideHelper

class DocumentAdapter(
    private val clickListener: (DocumentWithPages) -> Unit,
    private val optionsListener: (DocumentWithPages) -> Unit
) : ListAdapter<DocumentWithPages, DocumentAdapter.DocumentViewHolder>(DiffDocumentCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder =
        DocumentViewHolder(
            DocumentRowLayoutBinding.inflate(LayoutInflater.from(parent.context))
        )

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(private val viewBinding: DocumentRowLayoutBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(document: DocumentWithPages) {
            viewBinding.documentTitleText.text = document.document.title
            itemView.setOnClickListener { clickListener(document) }

            val isEmpty = document.pages.isEmpty()
            if (!isEmpty) {
                val page = document.pages[0]
                GlideHelper.loadPageIntoImageView(
                    page,
                    viewBinding.documentThumbnailImageview,
                    GlideHelper.GlideStyles.DOCUMENT_PREVIEW
                )
                viewBinding.documentThumbnailImageview.transitionName = page.id.toString()
            }

            val docDesc =
                if (document.pages.size == 1) itemView.context.getText(R.string.sync_doc_image)
                else itemView.context.getText(R.string.sync_doc_images)

            var desc = "${document.pages.size} $docDesc"

            if (document.document.isActive) {
                viewBinding.documentTitleText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_selection))
                viewBinding.documentDescriptionTextview.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_selection))
                desc = "$desc ${itemView.context.getText(R.string.sync_doc_active_text)}"
            }

            viewBinding.documentUploadStateIcon.setColorFilter(
                ContextCompat.getColor(
                    viewBinding.root.context,
                    if (isEmpty) R.color.second_light_gray else if (document.document.isActive) R.color.document_icon_active else R.color.document_icon_inactive
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            val imageResource = when {
                isEmpty -> {
                    R.drawable.ic_do_not_disturb_black_24dp
                }
                document.isUploadInProgress() -> {
                    R.drawable.ic_cloud_upload_blue_24dp
                }
                document.isUploaded() -> {
                    R.drawable.ic_cloud_done_blue_24dp
                }
                else -> {
                    R.drawable.ic_cloud_queue_blue_24dp
                }
            }
            val isProgressBarShown = document.isUploadInProgress() || document.isProcessing()
            viewBinding.documentProgressBar.bindInvisible(isProgressBarShown)
            desc += when {
                document.isUploadInProgress() -> {
                    "\n${itemView.context.getText(R.string.sync_dir_pending_text)}"
                }
                document.isProcessing() -> {
                    "\n${itemView.context.getText(R.string.sync_dir_processing_text)}"
                }
                else -> {
                    ""
                }
            }

            viewBinding.documentUploadStateIcon.setImageResource(imageResource)
            viewBinding.documentDescriptionTextview.text = desc
            viewBinding.documentMoreButton.setOnClickListener { optionsListener(document) }
        }
    }
}

class DiffDocumentCallback : DiffUtil.ItemCallback<DocumentWithPages>() {
    override fun areItemsTheSame(oldItem: DocumentWithPages, newItem: DocumentWithPages): Boolean {
        return oldItem.document.id == newItem.document.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: DocumentWithPages,
        newItem: DocumentWithPages
    ): Boolean {
        return oldItem.document.title == newItem.document.title &&
                oldItem.pages.size == newItem.pages.size &&
                oldItem.isProcessing() == newItem.isProcessing() &&
                oldItem.isExporting() == newItem.isExporting() &&
                oldItem.isUploadInProgress() == newItem.isUploadInProgress() &&
                oldItem.document.metaData == newItem.document.metaData
    }
}
