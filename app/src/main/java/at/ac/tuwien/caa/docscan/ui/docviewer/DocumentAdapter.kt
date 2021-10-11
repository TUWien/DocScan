package at.ac.tuwien.caa.docscan.ui.docviewer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.DocumentRowLayoutBinding
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Helper
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.io.IOException

class DocumentAdapter(
    private val clickListener: (DocumentWithPages) -> Unit,
    private val optionsListener: (DocumentWithPages) -> Unit
) : ListAdapter<DocumentWithPages, DocumentAdapter.DocumentViewHolder>(DiffDocumentCallback()) {

    private val fileHandler by inject<FileHandler>(FileHandler::class.java)

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
                fileHandler.getFileByPage(document.pages[0])?.let {
                    loadThumbnail(viewBinding.documentThumbnailImageview, it, itemView.context)
                    viewBinding.documentThumbnailImageview.transitionName = it.absolutePath
                }
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


//            Show the upload state:
//            if (document.isCurrentlyProcessed)
//                iconView.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)
//            else if (document.isUploaded) {
            // TODO: These fields needs to be correctly implemented.
//            if (document.isUploaded) {
//                if (document == activeDocument)
//                    iconView.setImageResource(R.drawable.ic_cloud_done_blue_24dp)
//                else
//                    iconView.setImageResource(R.drawable.ic_cloud_done_gray_24dp)
//            } else if (document.isAwaitingUpload) {
//                if (document == activeDocument)
//                    iconView.setImageResource(R.drawable.ic_cloud_upload_blue_24dp)
//                else
//                    iconView.setImageResource(R.drawable.ic_cloud_upload_gray_24dp)
//
//            } else {
//                if (document == activeDocument)
//                    iconView.setImageResource(R.drawable.ic_cloud_queue_blue_24dp)
//                else
//                    iconView.setImageResource(R.drawable.ic_cloud_queue_gray_24dp)
//            }
//
//            if (document.isCurrentlyProcessed) {
//                progressBar.visibility = View.VISIBLE
//                desc += "\n${itemView.context.getText(R.string.sync_dir_processing_text)}"
//            } else {
//                progressBar.visibility = View.GONE
//                if (document.isAwaitingUpload && !document.isUploaded)
//                    desc += "\n${itemView.context.getText(R.string.sync_dir_pending_text)}"
//            }

            viewBinding.documentDescriptionTextview.text = "$desc"
            viewBinding.documentMoreButton.setOnClickListener { optionsListener(document) }
        }
    }
}

private fun loadThumbnail(thumbnail: ImageView, file: File, context: Context) {
// TODO: This is duplicated across the entire app, introduce a GlideHandler class for this purpose.
    var exifOrientation = -1

    try {
        exifOrientation = Helper.getExifOrientation(file)

    } catch (e: IOException) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
    var requestOptions = RequestOptions()
    val radius = context.resources.getDimensionPixelSize(R.dimen.document_preview_corner_radius)
    requestOptions = requestOptions.transforms(CenterCrop(), RoundedCorners(radius))
    if (exifOrientation != -1) {
        GlideApp.with(context)
            .load(file?.path)
            .signature(MediaStoreSignature("", file.lastModified(), exifOrientation))
            .apply(requestOptions)
            .into(thumbnail)
    } else {
        GlideApp.with(context)
            .load(file)
            .signature(MediaStoreSignature("", file.lastModified(), 0))
            .apply(requestOptions)
            .into(thumbnail)
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
        return oldItem.document.title == newItem.document.title && oldItem.pages.size == newItem.pages.size
    }
}