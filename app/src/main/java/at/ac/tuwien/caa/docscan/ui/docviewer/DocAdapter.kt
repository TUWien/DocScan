package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
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

class DocAdapter(private val documents: ArrayList<Document>,
                 private val clickListener: (Document) -> Unit,
                 private val optionsListener: (Document) -> Unit,
                 private val activeDocument: Document) :
        RecyclerView.Adapter<DocAdapter.ViewHolder>() {

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

            if (document.pages != null && !document.pages.isEmpty())
                loadThumbnail(thumbnail, document.pages[0].file, itemView.context)
            else
                thumbnail.setImageResource(R.drawable.ic_do_not_disturb_black_24dp)

//            Show the number of pages:
            var desc = "${itemView.context.getText(R.string.sync_pages_text)} ${document.pages.size}"

//            Display the active document in a special color - and print it on the screen:
            if (document == activeDocument) {
                title.setTextColor(itemView.resources.getColor(R.color.text_selection))
                desc = "$desc ${itemView.context.getText(R.string.sync_doc_active_text)}"
                description.setTextColor(itemView.resources.getColor(R.color.text_selection))
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

        if (exifOrientation != -1) {
            GlideApp.with(context)
                    .load(file?.path)
                    .signature(MediaStoreSignature("", 0, exifOrientation))
                    .into(thumbnail)
        } else {
            GlideApp.with(context)
                    .load(file)
                    .into(thumbnail)
        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val thumbnail = view.document_thumbnail_imageview
        val title = view.document_title_text
        val description = view.document_description_textview
        val moreButton = view.document_more_button

    }

}