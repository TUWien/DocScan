package at.ac.tuwien.caa.docscan.ui.docviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.fragment_pdfs.*
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

    fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
            // TODO: check if we can use here the default folder...
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

//        TODO: use a meaningfull int here:
        startActivityForResult(intent, 42)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == 42
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                saveDirectory(uri)
            }
        }
    }


    fun saveDirectory(uri: Uri?) {
        if (uri == null) {
            return
        }
        context?.contentResolver?.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString(getString(R.string.key_pdf_dir), uri.toString())
        editor.commit()

    }

    /**
     *  Updates a file with a given path name if it is contained in the list. Otherwise it is added
     *  to the list.
     */
    fun updateFile(path: String?) {

        if (path == null)
            return

//        Update the list, if the path is contained:
        if (!updateList(path)) {
//            Otherwise add it to the list:
            addPdfToList(path)
        }

    }

    /**
     * Returns the directory in which pdf's are saved.
     */
    private fun getPdfDirectory(): String? {
        val dir: String?
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        dir = sharedPref.getString(getString(R.string.key_pdf_dir), null)

        return dir
    }

    /**
     * Adds a new pdf to the list and marks it with a badge in the UI.
     */
    private fun addPdfToList(path: String) {

        val dir = getPdfDirectory() ?: return

        val uri = Uri.parse(dir)
        val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        val fileName = File(path).name

        val documentId = DocumentsContract.getDocumentId(uriFolder)
        val pdfUri = DocumentsContract.buildDocumentUriUsingTree(uri, "$documentId/$fileName")
        val file = DocumentFile.fromSingleUri(context!!, pdfUri)
        if (file != null) {
            val pdf = fileToPdf(file)
            if (pdf != null) {
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
        }

    }

    /**
     * Iterates over the pdf list and checks if a file path is contained in it. In this case the
     * file is marked with a badge in the UI. Returns true if the path is contained in the list.
     */
    private fun updateList(path: String): Boolean {

        val fileName = File(path).name

        val it = pdfList.iterator()
        var idx = 0
        while (it.hasNext()) {
            val pdf = it.next()
//            Just match here file names, because path is an absolute path from File and pdf.file
//            belongs to a DocumentFile
            if (pdf.file.name.equals(fileName)) {
//                now update the required fields:
                pdf.date = formatDate(pdf.file.lastModified())
                pdf.showBadge = true
                pdf_list.adapter?.notifyItemMoved(idx, 0)

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
                    newPdfs.add(pdf.file.uri.toString())
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
            file: DocumentFile ->
            listener?.onPdfOptions(file) }
        pdf_list.adapter = pdfAdapter

    }

    private fun showDirectoryPermissionRequiredAlert() {

        val text = getString(R.string.pdf_fragment_persisted_permission_text)
        val title = getString(R.string.pdf_fragment_persisted_permission_title)

        val alertDialogBuilder = AlertDialog.Builder(context!!)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.dialog_ok_text) {
                    dialogInterface, i -> openDirectory()
            }
            .setNegativeButton(R.string.dialog_cancel_text, null)
            .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    override fun onResume() {

        super.onResume()

        val dir = getPdfDirectory()

//        The directory is not saved until now:
        if (dir == null) {
            showDirectoryPermissionRequiredAlert()
        }
//        The directory is saved, check if the permission is also given (not sure if this can
//        even happen).
        else if (isPermissionGiven(dir)) {
            updatePdfs()
            pdf_list.layoutManager = LinearLayoutManager(context)
        }
//        No permission is given for the folder - so ask for it:
        else {
            showDirectoryPermissionRequiredAlert()
        }

    }

    /**
     * Returns true if a writable persistedUriPermission is given for a folder
     */
    private fun isPermissionGiven(folder: String): Boolean {

        val permissions = context?.contentResolver?.persistedUriPermissions
        if (permissions != null) {
            for (permission in permissions) {
                if (permission.uri.toString() == folder)
                    return true
            }
        }

        return false

    }

    private fun getPdfs() : MutableList<Pdf> {

        val dir = getPdfDirectory() ?: return ArrayList()

        val uri = Uri.parse(dir)
        val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )

        val pdfList: MutableList<Pdf> = ArrayList()
        if (uriFolder != null && context != null) {
            val cursor = context?.contentResolver?.query(uriFolder,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val pdfUri = DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0))
                    val file = DocumentFile.fromSingleUri(context!!, pdfUri)
                    if (file?.type == "application/pdf") {
                        val pdf = fileToPdf(file)
                        pdf?.let { pdfList.add(it) }
                    }
                }
                while(cursor.moveToNext())
            }

        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList
    }

//    private fun getPdfs_() : MutableList<Pdf> {
//        var dir: String?
//        var folder: DocumentFile?
//
////        TODO: check if this is the correct version:
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            Log.d("pdf", "getPdfs 1");
//            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
////            TODO: DocumentFile.fromTreeUri works only on Lollipop and above:
//            dir = sharedPref.getString(getString(R.string.key_pdf_dir), null)
//            folder = DocumentFile.fromTreeUri(context!!, Uri.parse(dir))
//        } else {
//            Log.d("pdf", "getPdfs 2");
//            dir = Helper.getPDFStorageDir(getString(R.string.app_name)).toString()
//            folder = DocumentFile.fromFile(File(dir))
//        }
//        val pdfList: MutableList<Pdf> = ArrayList()
//        if (dir != null && context != null && folder != null) {
//            val files = folder.listFiles()
//            for (file in files) {
//                if (file.type != "application/pdf")
//                    continue
//                val pdf = fileToPdf(file)
////                    TODO: do something here:
////                    if (newPdfs.contains(file.absolutePath))
////                        pdf.showBadge = true
//
//                pdfList.add(pdf)
//            }
//
//        }
//
////        sort the list based on the last modified date:
//        pdfList.sort()
//
//        return pdfList
//
//    }

    private fun fileToPdf(file: DocumentFile): Pdf? {
        val fileSizeInBytes = file.length()
        val fileSize: Float = (fileSizeInBytes / (1024 * 1024).toFloat())
        val fileSizeInMB: String = "%.1f".format(fileSize)
        val date = formatDate(file.lastModified())

        return file.name?.let { Pdf(it, file, fileSizeInMB, date) }
    }

    private fun formatDate(date: Long): String {
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss")
        val fDate = format.format(date).toString()
        return fDate
    }

    class Pdf(val name: String, val file: DocumentFile, val fileSize: String, var date: String)
        : Comparable<Pdf> {

        //        the badge shows that the pdf is new:
        var showBadge = false

        override fun compareTo(other: Pdf): Int {
            return other.file.lastModified().compareTo(file.lastModified())
        }

    }

    interface PdfListener {
        fun onPdfOptions(file: DocumentFile)
    }
}