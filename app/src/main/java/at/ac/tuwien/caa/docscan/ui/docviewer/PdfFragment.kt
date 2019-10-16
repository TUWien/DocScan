package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.ui.pdf.PdfActivity
import at.ac.tuwien.caa.docscan.ui.pdf.PdfAdapter
import kotlinx.android.synthetic.main.activity_pdf.*
import java.io.File
import java.text.SimpleDateFormat

class PdfFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pdfs, container, false)
    }

    override fun onResume() {
        super.onResume()

        // Acquire the pdfs in the directory:
        val pdfList = getPdfs()
//        Set the adapter:
        val pdfAdapter = context?.let { PdfAdapter(it, pdfList) }
        pdf_list_view.adapter = pdfAdapter
    }

    private fun getPdfs() : MutableList<PdfActivity.Pdf> {

        val dir : File? = Helper.getPDFStorageDir(getString(R.string.app_name))
        val pdfList: MutableList<PdfActivity.Pdf> = ArrayList()

        if (dir != null) {
            val files = dir.listFiles()

            for (file in files) {
                val fileSizeInBytes = file.length()
                val fileSize : Float = (fileSizeInBytes / (1024*1024).toFloat())
                val fileSizeInMB: String = "%.1f".format(fileSize)

                val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss")
                val date = format.format(file.lastModified()).toString()

                pdfList.add(PdfActivity.Pdf(file.name, file, fileSizeInMB, date))
            }
        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList

    }
}