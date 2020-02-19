package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.ImageView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.Helper
import com.bumptech.glide.signature.MediaStoreSignature
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.document_row_layout.view.*
import java.io.File
import java.io.IOException
import java.util.*
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions

class DocumentAdapter(private val documents: ArrayList<Document>,
                      private val clickListener: (Document) -> Unit,
                      private val optionsListener: (Document) -> Unit,
                      private val activeDocument: Document?) :
        RecyclerView.Adapter<DocumentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.document_row_layout, parent, false))

    }

    override fun getItemCount(): Int {

        return documents.size

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val document = documents[position]

        with (holder) {

            title.text = document.title

            itemView.setOnClickListener{ clickListener(document) }



            if (document.pages != null && !document.pages.isEmpty()) {
                loadThumbnail(thumbnail, document.pages[0].file, itemView.context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    thumbnail.transitionName = document.pages[0].file.absolutePath
            }
            else {
                thumbnail.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)
                thumbnail.setBackgroundColor(itemView.resources.getColor(R.color.second_light_gray))
            }

//            1 image or  many images?
            val docDesc =
                    if (document.pages.size == 1) itemView.context.getText(R.string.sync_doc_image)
                    else itemView.context.getText(R.string.sync_doc_images)

            var desc = "${document.pages.size} $docDesc"

//            Display the active document in a special color - and print it on the screen:
            if (document == activeDocument) {
                title.setTextColor(itemView.resources.getColor(R.color.text_selection))
                desc = "$desc ${itemView.context.getText(R.string.sync_doc_active_text)}"
                description.setTextColor(itemView.resources.getColor(R.color.text_selection))
            }


//            Show the upload state:
//            if (document.isCurrentlyProcessed)
//                iconView.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)
//            else if (document.isUploaded) {
            if (document.isUploaded) {
                if (document == activeDocument)
                    iconView.setImageResource(R.drawable.ic_cloud_done_blue_24dp)
                else
                    iconView.setImageResource(R.drawable.ic_cloud_done_gray_24dp)
            }
            else if (document.isAwaitingUpload) {
                if (document == activeDocument)
                    iconView.setImageResource(R.drawable.ic_cloud_upload_blue_24dp)
                else
                    iconView.setImageResource(R.drawable.ic_cloud_upload_gray_24dp)

            }
            else {
                if (document == activeDocument)
                    iconView.setImageResource(R.drawable.ic_cloud_queue_blue_24dp)
                else
                    iconView.setImageResource(R.drawable.ic_cloud_queue_gray_24dp)
            }

            if (document.isCurrentlyProcessed) {
                progressBar.visibility = View.VISIBLE
                desc += "\n${itemView.context.getText(R.string.sync_dir_processing_text)}"
            }
            else {
                progressBar.visibility = View.GONE
                if (document.isAwaitingUpload && !document.isUploaded)
                    desc += "\n${itemView.context.getText(R.string.sync_dir_pending_text)}"
            }


            description.text = "$desc"

            moreButton.setOnClickListener { optionsListener(document)}
        }

    }

    private fun loadThumbnail(thumbnail: ImageView, file: File, context: Context) {

        var exifOrientation = -1

        try {
            exifOrientation = Helper.getExifOrientation(file)

        } catch (e: IOException) {
            Crashlytics.logException(e)
            e.printStackTrace()
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val thumbnail = view.document_thumbnail_imageview
        val title = view.document_title_text
        val description = view.document_description_textview
        val iconView = view.document_upload_state_icon
        val moreButton = view.document_more_button
        val progressBar = view.document_progress_bar

    }

}