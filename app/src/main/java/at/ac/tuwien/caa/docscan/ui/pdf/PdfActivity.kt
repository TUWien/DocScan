package at.ac.tuwien.caa.docscan.ui.pdf

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer
import kotlinx.android.synthetic.main.activity_pdf.*
import java.io.File
import java.text.SimpleDateFormat


class PdfActivity : BaseNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        val fab: FloatingActionButton? = findViewById(R.id.camera_fab)
        fab?.setOnClickListener{
            Helper.startCameraActivity(this)
        }

    }

    override fun onResume() {
        super.onResume()

        // Acquire the pdfs in the directory:
        val pdfList = getPdfs()
//        Set the adapter:
        val pdfAdapter = PdfAdapter(this, pdfList)
        pdf_list_view.adapter = pdfAdapter
    }

    override fun getSelfNavDrawerItem(): NavigationDrawer.NavigationItemEnum {
        return NavigationDrawer.NavigationItemEnum.PDF
    }


    private fun getPdfs() : MutableList<Pdf> {

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

                pdfList.add(Pdf(file.name, file, fileSizeInMB, date))
            }
        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList

    }

    class Pdf(name: String, path: File, fileSize: String, date: String) : Comparable<Pdf> {

        override fun compareTo(other: Pdf): Int {
            return other.mPath.lastModified().compareTo(mPath.lastModified())
        }

        var mName: String = name
        var mPath: File = path
        var mFileSize: String = fileSize
        var mDate: String = date

    }



}