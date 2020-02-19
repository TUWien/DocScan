package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Helper
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.layout_pdflist_row.view.*
import java.io.File

class PdfAdapter(private val context: Context,
                 private val pdfs: MutableList<PdfFragment.Pdf>,
                 private val optionsListener: (File) -> Unit) :
        RecyclerView.Adapter<PdfAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_pdflist_row, parent, false))

    }

    override fun getItemCount(): Int {
        return pdfs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val pdf = pdfs[position]

        with (holder) {
            title.text = pdf.name
            fileSize.text = pdf.fileSize + " MB"
            date.text = pdf.date
            val iconRes = if (pdf.showBadge) R.drawable.ic_pdf_icon_badge else R.drawable.ic_pdf_icon
            icon.setImageResource(iconRes)
            moreButton.setOnClickListener { optionsListener(pdf.file)}

            itemView.setOnClickListener {
                val path = FileProvider.getUriForFile(context, "at.ac.tuwien.caa.fileprovider",
                        File(pdfs[position].file.absolutePath))
                val intent = Intent(Intent.ACTION_VIEW)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.setDataAndType(path, "application/pdf")
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                pdf.showBadge = false
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Crashlytics.logException(e)
                    Helper.showActivityNotFoundAlert(context)
                }
            }
        }

    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.layout_pdflist_row_title!!
        val fileSize = view.layout_pdflist_row_filesize!!
        val date = view.layout_pdflist_row_date!!
        val icon = view.layout_pdflist_row_icon!!
        var moreButton = view.layout_pdflist_more_button!!

    }

}