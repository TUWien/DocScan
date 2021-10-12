package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.Helper
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.select_document_row_layout.view.*
import java.io.File
import java.io.IOException
import java.util.*

class SelectDocumentAdapter(
    private val documents: ArrayList<Document>,
    private val clickListener: (Document) -> Unit
) :
    RecyclerView.Adapter<SelectDocumentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.select_document_row_layout,
                parent,
                false
            )
        )

    }

    override fun getItemCount(): Int {

        return documents.size

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val document = documents[position]

        with(holder) {

            title.text = document.title

            itemView.setOnClickListener { clickListener(document) }



            if (document.pages != null && !document.pages.isEmpty()) {
                loadThumbnail(thumbnail, document.pages[0].file, itemView.context)
                thumbnail.transitionName = document.pages[0].file.absolutePath
            } else {
                thumbnail.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)
                thumbnail.setBackgroundColor(itemView.resources.getColor(R.color.second_light_gray))
            }

//            1 image or  many images?
            val docDesc =
                if (document.pages.size == 1) itemView.context.getText(R.string.sync_doc_image)
                else itemView.context.getText(R.string.sync_doc_images)

            var desc = "${document.pages.size} $docDesc"
            description.text = "$desc"

        }

    }

    private fun loadThumbnail(thumbnail: ImageView, file: File, context: Context) {

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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val thumbnail = view.document_thumbnail_imageview
        val title = view.document_title_text
        val description = view.document_description_textview

    }

}