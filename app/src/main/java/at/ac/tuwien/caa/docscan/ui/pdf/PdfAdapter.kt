package at.ac.tuwien.caa.docscan.ui.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Helper
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.layout_pdflist_row.view.*
import java.io.File

class PdfAdapter(private val context: Context,
                 private val pdfList: MutableList<PdfActivity.Pdf>) : BaseAdapter() {

    private val layoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

        val viewHolder: ViewHolder
        val rowView: View?

        if (convertView == null) {
            rowView = layoutInflater.inflate(R.layout.layout_pdflist_row, parent, false)

            viewHolder = ViewHolder(rowView)
            rowView.tag = viewHolder

        } else {
            rowView = convertView
            viewHolder = rowView.tag as ViewHolder
        }

//        viewHolder.title.text = pdfList[position].mName
        viewHolder.title.text = pdfList[position].mName
        viewHolder.fileSize.text = pdfList[position].mFileSize + " MB"
        viewHolder.date.text = pdfList[position].mDate

        rowView?.setOnClickListener {
            val path = FileProvider.getUriForFile(context, "at.ac.tuwien.caa.fileprovider",
                    File(pdfList[position].mPath.absolutePath))
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.setDataAndType(path, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(intent)
            }
            catch (e: ActivityNotFoundException) {
                Crashlytics.logException(e);
                Helper.showActivityNotFoundAlert(context)
            }
        }

        return rowView

    }

    override fun getItem(position: Int): Any {
        return 0
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return pdfList.size
    }


    class ViewHolder(view: View) {

        val title = view.layout_pdflist_row_title!!
        val fileSize = view.layout_pdflist_row_filesize!!
        val date = view.layout_pdflist_row_date!!


    }

}