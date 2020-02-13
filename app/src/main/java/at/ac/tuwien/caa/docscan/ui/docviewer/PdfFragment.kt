package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_pdfs.*
//import kotlinx.android.synthetic.main.activity_pdf.*
import java.io.File
import java.text.SimpleDateFormat

class PdfFragment : Fragment() {

    companion object {
        fun newInstance(bundle : Bundle) : PdfFragment{
            val fragment = PdfFragment()
            fragment.arguments = bundle
            return fragment
        }

        val TAG = "PdfFragment"
        val NEW_PDFS_KEY = "NEW_PDFS_KEY"
    }

    private lateinit var newPdfs: MutableList<String>
    private lateinit var listener: PdfListener
    private lateinit var pdfList: MutableList<Pdf>
//    private lateinit var newPdfs: MutableList<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pdfs, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getStringArrayList(NEW_PDFS_KEY)?.let {
            newPdfs = it
        }

        listener = context as PdfListener

    }

    /**
     *  Updates a file with a given path name if it is contained in the list. Otherwise it is added
     *  to the list.
     */
    fun updateFile(path: String) {

//        Update the list, if the path is contained:
        if (!updateList(path))
//            Otherwise add it to the list:
            addPdfToList(path)

    }

    /**
     * Adds a new pdf to the list and marks it with a badge in the UI.
     */
    private fun addPdfToList(path: String) {

        val pdf = fileToPdf(File(path))
        pdf.showBadge = true

//        In case the list was empty before, we need to show the list and initialize the adapter:
        if (pdfList.isEmpty()) {
            pdf_list.visibility = View.VISIBLE
            pdf_empty_layout.visibility = View.INVISIBLE
            updatePdfAdapter()
        }

        pdfList.add(0, pdf)

        pdf_list.adapter?.notifyItemChanged(0)

    }

    /**
     * Iterates over the pdf list and checks if a file path is contained in it. In this case the
     * file is marked with a badge in the UI. Returns true if the path is contained in the list.
     */
    private fun updateList(path: String): Boolean {

        val it = pdfList.iterator()
        var idx = 0
        while (it.hasNext()) {
            val pdf = it.next()
            if (pdf.file.absolutePath.equals(path)) {
    //                now update the date
                pdf.date = formatDate(pdf.file.lastModified())
                pdf.showBadge = true
                pdf_list.adapter!!.notifyItemChanged(idx)

                return true
            }
            idx++
        }

        return false
    }

    override fun onStop() {

//        Save the current adapter state to newPdfs list:
        newPdfs.clear()
        if (::pdfList.isInitialized) {
            val it = pdfList.iterator()
            while (it.hasNext()) {
                val pdf = it.next()
                if (pdf.showBadge)
                    newPdfs.add(pdf.file.absolutePath)
            }
        }

        super.onStop()

    }

    fun updatePdfs() {

        pdfList = getPdfs()

//        Show the user that no pdf is contained in the list:
        if (!showEmptyList())
            updatePdfAdapter()

    }

    /**
     * Updates the UI in case the pdfList is empty and returns true.
     */
    private fun showEmptyList(): Boolean {

        if (pdfList.isEmpty()) {
            pdf_list.visibility = View.INVISIBLE
            pdf_empty_layout.visibility = View.VISIBLE
            return true
        }

        return false

    }


    private fun updatePdfAdapter() {

//        Set the adapter:
        val pdfAdapter = PdfAdapter(context!!, pdfList) {
            file: File ->
            listener?.onPdfOptions(file) }
        pdf_list.adapter = pdfAdapter

    }

    override fun onResume() {
        super.onResume()

        updatePdfs()
        pdf_list.layoutManager = LinearLayoutManager(context)

    }

    private fun getPdfs() : MutableList<Pdf> {

        val dir : File? = Helper.getPDFStorageDir(getString(R.string.app_name))
        val pdfList: MutableList<Pdf> = ArrayList()

        if (dir != null) {
            val files = dir.listFiles()

            for (file in files) {
                val pdf = fileToPdf(file)
                if (newPdfs.contains(file.absolutePath))
                    pdf.showBadge = true

                pdfList.add(pdf)
            }
        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList

    }

    private fun fileToPdf(file: File): Pdf {
        val fileSizeInBytes = file.length()
        val fileSize: Float = (fileSizeInBytes / (1024 * 1024).toFloat())
        val fileSizeInMB: String = "%.1f".format(fileSize)
        val date = formatDate(file.lastModified())

        val pdf = Pdf(file.name, file, fileSizeInMB, date)
        return pdf
    }

    private fun formatDate(date: Long): String {
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss")
        val fDate = format.format(date).toString()
        return fDate
    }

    class Pdf(val name: String, val file: File, val fileSize: String, var date: String)
        : Comparable<Pdf> {

//        the badge shows that the pdf is new:
        var showBadge = false

        override fun compareTo(other: Pdf): Int {
            return other.file.lastModified().compareTo(file.lastModified())
        }

    }

    interface PdfListener {
        fun onPdfOptions(file: File)
    }
}