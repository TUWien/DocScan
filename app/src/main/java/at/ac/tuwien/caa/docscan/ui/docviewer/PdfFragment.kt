package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.ui.pdf.PdfAdapter
import kotlinx.android.parcel.Parcelize
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

//    fun setNewPdfs(pdfs: MutableList<String>) {
//
//        Log.d(TAG, "setNewPdfs")
//        newPdfs = pdfs
//    }

    fun updateItem(path: String) {

        Log.d(TAG, "updateItem")

        //        Find the item:
        val it = pdfList.iterator()
        var idx = 0
        while(it.hasNext()) {
            val pdf = it.next()
            if (pdf.file.absolutePath.equals(path)) {
                Log.d(TAG, "found at pos: " + idx)
//                now update the date
                pdf.date = formatDate(pdf.file.lastModified())
                pdf.showBadge = true
                pdf_list.adapter!!.notifyItemChanged(idx)

                return
            }
            idx++
        }

    }

    override fun onStop() {

//        Save the current adapter state to newPdfs list:
        newPdfs.clear()
        val it = pdfList.iterator()
        while(it.hasNext()) {
            val pdf = it.next()
            if (pdf.showBadge)
                newPdfs.add(pdf.file.absolutePath)
        }

        super.onStop()

    }

    fun updatePdfAdapter() {

        // Acquire the pdfs in the directory:
        pdfList = getPdfs()
//        Set the adapter:
        val pdfAdapter = PdfAdapter(context!!, pdfList) {
            file: File ->
            listener?.onPdfOptions(file) }
        pdf_list.adapter = pdfAdapter

    }

    override fun onResume() {
        super.onResume()

        pdf_list.layoutManager = LinearLayoutManager(context)
        updatePdfAdapter()

//        pdf_list.layoutManager = LinearLayoutManager(context)


//        val pdfAdapter = context?.let { PdfAdapter(it, pdfList,
//
////                    Inform the DocumentViewerActivity that document options should be shown:
//                })
    }

    private fun getPdfs() : MutableList<Pdf> {

        val dir : File? = Helper.getPDFStorageDir(getString(R.string.app_name))
        val pdfList: MutableList<Pdf> = ArrayList()

        if (dir != null) {
            val files = dir.listFiles()

            for (file in files) {
                val fileSizeInBytes = file.length()
                val fileSize : Float = (fileSizeInBytes / (1024*1024).toFloat())
                val fileSizeInMB: String = "%.1f".format(fileSize)
                val date = formatDate(file.lastModified())

                val pdf = Pdf(file.name, file, fileSizeInMB, date)

                if (newPdfs.contains(file.absolutePath))
                    pdf.showBadge = true
                pdfList.add(pdf)
            }
        }

//        sort the list based on the last modified date:
        pdfList.sort()

//        We do not need the new pdf list from this point on:
//        newPdfs.clear()

        return pdfList

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