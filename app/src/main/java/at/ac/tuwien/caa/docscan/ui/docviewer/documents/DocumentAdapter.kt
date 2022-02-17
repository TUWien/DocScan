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
import at.ac.tuwien.caa.docscan.logic.NetworkStatus

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

            var desc = itemView.resources.getQuantityString(
                R.plurals.images,
                document.pages.size,
                document.pages.size
            )

            if (document.document.isActive) {
                viewBinding.documentTitleText.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_selection
                    )
                )
                viewBinding.documentDescriptionTextview.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_selection
                    )
                )
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
                document.isUploadScheduled() -> {
                    R.drawable.ic_baseline_schedule_24
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
            if (document.isUploadInProgress()) {
                viewBinding.documentProgressBar.isIndeterminate = false
                viewBinding.documentProgressBar.max = document.pages.size
                viewBinding.documentProgressBar.progress = document.numberOfFinishedUploads()
            } else {
                viewBinding.documentProgressBar.isIndeterminate = true
            }
            desc += when {
                document.isUploadInProgress() -> {
                    "\n${itemView.context.getText(R.string.sync_dir_pending_text)}"
                }
                document.isProcessing() -> {
                    "\n${itemView.context.getText(R.string.sync_dir_processing_text)}"
                }
                document.isUploadScheduled() -> {
                    val additionalTextHint = when (document.networkStatus) {
                        NetworkStatus.CONNECTED_UNMETERED -> itemView.context.getText(R.string.sync_dir_upload_scheduled_begin_shortly_text)
                        NetworkStatus.CONNECTED_METERED -> {
                            if (document.hasUserAllowedMeteredNetwork) {
                                itemView.context.getText(R.string.sync_dir_upload_scheduled_begin_shortly_text)
                            } else {
                                itemView.context.getText(R.string.sync_dir_upload_scheduled_unmetered_condition_text)
                            }
                        }
                        NetworkStatus.DISCONNECTED -> itemView.context.getText(R.string.sync_dir_upload_scheduled_unmetered_condition_text)
                    }
                    "\n${itemView.context.getText(R.string.sync_dir_upload_scheduled_text)} $additionalTextHint"
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
        return isEqual(oldItem, newItem) &&
                // if the upload is in progress, take the number of finished uploads into account due to the progress
                (if (oldItem.isUploadInProgress() == newItem.isUploadInProgress()) (oldItem.numberOfFinishedUploads() == newItem.numberOfFinishedUploads()) else false)
    }

    /**
     * By overriding this function, we can provide any arbitrary object to detect what has changed
     * in the two views, please read the doc of the super function when this is called to get a better
     * understanding on how it works.
     *
     * If null is returned, then the [androidx.recyclerview.widget.DefaultItemAnimator] which is used
     * by default on the recyclerview, won't apply the cross-fade animation, i.e. the whole item
     * won't be blinking. We want to add this behavior only if the progress of the upload changes, so
     * if the two items only change by the different number of finished uploads, an arbitrary object
     * is returned to not perform the change animation.
     */
    override fun getChangePayload(oldItem: DocumentWithPages, newItem: DocumentWithPages): Any? {
        return when {
            isEqual(
                oldItem,
                newItem
            ) && if (oldItem.isUploadInProgress() == newItem.isUploadInProgress()) (oldItem.numberOfFinishedUploads() != newItem.numberOfFinishedUploads()) else false -> Any()
            else -> null
        }
    }

    /**
     * Checks for equality for the scope of this adapter, this check does not include the upload in
     * progress state, since it's used to disable the entire animations if the progress changes.
     */
    private fun isEqual(oldItem: DocumentWithPages, newItem: DocumentWithPages): Boolean {
        return oldItem.document.title == newItem.document.title &&
                oldItem.pages.size == newItem.pages.size &&
                oldItem.isProcessing() == newItem.isProcessing() &&
                oldItem.isExporting() == newItem.isExporting() &&
                oldItem.isUploaded() == newItem.isUploaded() &&
                oldItem.document.metaData == newItem.document.metaData &&
                // for the scheduled state also compare the network status, since this will make a difference
                // in the appearance
                (if (oldItem.isUploadScheduled() == newItem.isUploadScheduled()) oldItem.networkStatus == newItem.networkStatus && oldItem.hasUserAllowedMeteredNetwork == newItem.hasUserAllowedMeteredNetwork else false)
    }
}
