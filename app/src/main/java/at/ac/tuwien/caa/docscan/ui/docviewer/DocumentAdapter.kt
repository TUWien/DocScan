package at.ac.tuwien.caa.docscan.ui.docviewer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.DocumentRowLayoutBinding
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.isProcessing
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

            if (document.pages.isNotEmpty()) {
                val page = document.pages[0]
                GlideHelper.loadPageIntoImageView(
                    page,
                    viewBinding.documentThumbnailImageview,
                    GlideHelper.GlideStyles.DOCUMENT_PREVIEW
                )
                viewBinding.documentThumbnailImageview.transitionName = page.id.toString()
            } else {
                viewBinding.documentThumbnailImageview.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)
                viewBinding.documentThumbnailImageview.setBackgroundColor(
                    itemView.resources.getColor(
                        R.color.second_light_gray
                    )
                )
            }

//            1 image or  many images?
            val docDesc =
                if (document.pages.size == 1) itemView.context.getText(R.string.sync_doc_image)
                else itemView.context.getText(R.string.sync_doc_images)

            var desc = "${document.pages.size} $docDesc"

//            Display the active document in a special color - and print it on the screen:
            if (document.document.isActive) {
                viewBinding.documentTitleText.setTextColor(itemView.resources.getColor(R.color.text_selection))
                desc =
                    "$desc ${itemView.context.getText(R.string.sync_doc_active_text)}"

                viewBinding.documentDescriptionTextview.setTextColor(itemView.resources.getColor(R.color.text_selection))
            }

            viewBinding.documentUploadStateIcon.setColorFilter(
                ContextCompat.getColor(
                    viewBinding.root.context,
                    if (document.document.isActive) R.color.document_icon_active else R.color.document_icon_inactive
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            // TODO: add uploading states too, awaitingUpload=ic_cloud_upload_gray_24dp and isUploaded=ic_cloud_done_gray_24dp
            // TODO: Add the help message                 desc += "\n${itemView.context.getText(R.string.sync_dir_pending_text)}"
            val imageResource: Int = if (document.isProcessing()) {
                viewBinding.documentProgressBar.visibility = View.VISIBLE
                desc += "\n${itemView.context.getText(R.string.sync_dir_processing_text)}"
                R.drawable.ic_do_not_disturb_black_24dp
            } else {
                viewBinding.documentProgressBar.visibility = View.INVISIBLE
                R.drawable.ic_cloud_queue_gray_24dp
            }

            viewBinding.documentUploadStateIcon.setImageResource(imageResource)
            viewBinding.documentDescriptionTextview.text = "$desc"
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
        // TODO: Check if this is sufficient
        return oldItem.document.title == newItem.document.title && oldItem.pages.size == newItem.pages.size && oldItem.isProcessing() == newItem.isProcessing() && oldItem.document.metaData == newItem.document.metaData
    }
}
