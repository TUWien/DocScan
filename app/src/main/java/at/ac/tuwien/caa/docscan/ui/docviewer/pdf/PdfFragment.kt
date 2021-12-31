package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.SheetAction
import at.ac.tuwien.caa.docscan.databinding.FragmentPdfsBinding
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat

/**
 * TODO: EXPORT_LOGIC - Refactor this fragment to allow exports of documents as before.
 * TODO: EXPORT_LOGIC - Check the necessary requirements of the access storage framework.
 */
class PdfFragment : BaseFragment() {

    companion object {

        fun newInstance() = PdfFragment()

        const val TAG = "PdfFragment"
        const val PERSISTABLE_URI_PERMISSION = 0
    }

    private lateinit var binding: FragmentPdfsBinding

    //    private lateinit var newPdfs: MutableList<String>
    private lateinit var pdfList: MutableList<Pdf>
//    private lateinit var newPdfs: MutableList<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPdfsBinding.inflate(layoutInflater)
        return binding.root
    }

//    override fun onPdfSheetSelected(pdf: DocumentFile, sheetAction: ActionSheet.SheetAction) {
//
//        when (sheetAction.mId) {
//            R.id.action_pdf_share_item -> {
//                sharePdf(pdf)
//            }
//            R.id.action_pdf_delete_item -> {
//                showDeletePdfConfirmationDialog(pdf)
//            }
//        }
//    }

    private fun showDeletePdfConfirmationDialog(pdf: DocumentFile) {


//        val deleteText = resources.getString(R.string.sync_confirm_delete_prefix_text)

        // TODO:
        val deleteText = getString(R.string.viewer_delete_pdf_text)
        val deleteTitle = "${getString(R.string.viewer_delete_pdf_title)}: ${pdf.name}?"

        val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(deleteTitle)
            .setMessage(deleteText)
            .setPositiveButton(R.string.sync_confirm_delete_button_text) { dialogInterface, i ->
//                deletePdf(pdf)
            }
            .setNegativeButton(R.string.sync_cancel_delete_button_text, null)
            .setCancelable(true)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun getDocScanPdfUri(): Uri {

        val sm = requireContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager

        val rootId =
            if (sm.primaryStorageVolume.isEmulated)
                "primary"
            else
                sm.primaryStorageVolume.uuid

        val rootUri =
            DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", rootId)
        val documentsDir = Environment.DIRECTORY_DOCUMENTS
        val docScanDir = getString(R.string.app_name)
        val concatedDir = ":$documentsDir/$docScanDir"
        val encodedDir = URLEncoder.encode(concatedDir, "utf-8")
        val absoluteDir = "$rootUri$encodedDir"

        return Uri.parse(absoluteDir)
    }

    private fun openDocScanDocumentDir() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (context != null) {
                val docScanPdfUri = getDocScanPdfUri()
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, docScanPdfUri)
                    putExtra("android.provider.extra.SHOW_ADVANCED", true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }

                startActivityForResult(intent, PERSISTABLE_URI_PERMISSION)

            }
        }

    }


    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (requestCode == PERSISTABLE_URI_PERMISSION
            && resultCode == Activity.RESULT_OK
        ) {
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
        context?.contentResolver?.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString(getString(R.string.key_pdf_dir), uri.toString())
        editor.apply()

    }

    private fun sharePdf(pdf: DocumentFile) {
        val uri = pdf.uri
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        //                    check here if the content resolver is null
        shareIntent.setDataAndType(uri, requireContext().contentResolver.getType(uri))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/pdf"
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.page_slide_fragment_share_choose_app_text)
            )
        )
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

    // TODO: replace this with KtHelper.getPdfDirectory
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
    private fun addPdfToList(fileName: String) {

        val file: DocumentFile?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dir = getPdfDirectory() ?: return

            val uri = Uri.parse(dir)
            val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )

            val documentId = DocumentsContract.getDocumentId(uriFolder)
            val pdfUri = DocumentsContract.buildDocumentUriUsingTree(uri, "$documentId/$fileName")
            file = DocumentFile.fromSingleUri(requireContext(), pdfUri)
        } else {
            val pdfFolder = Helper.getPDFStorageDir(getString(R.string.app_name))
            val directPathFile = File(pdfFolder, fileName)
            file = DocumentFile.fromFile(directPathFile)
        }

        if (file != null) {
            val pdf = fileToPdf(file)
            if (pdf != null) {
                pdf.showBadge = true

//                initPdfList()
//        In case the list was empty before, we need to show the list and initialize the adapter:
                if (pdfList.isEmpty()) {
                    binding.pdfList.visibility = View.VISIBLE
                    binding.pdfEmptyLayout.visibility = View.INVISIBLE
//                    updatePdfAdapter()
                }

                pdfList.add(0, pdf)

                binding.pdfList.adapter?.notifyItemChanged(0)

            }
        }

    }

    /**
     * Iterates over the pdf list and checks if a file path is contained in it. In this case the
     * file is marked with a badge in the UI. Returns true if the path is contained in the list.
     */
    private fun updateList(fileName: String): Boolean {


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
                binding.pdfList.adapter?.notifyItemMoved(idx, 0)
                binding.pdfList.scrollToPosition(0)

                return true
            }
            idx++
        }

        return false
    }

    override fun onStop() {

//        Save the current adapter state to newPdfs list:
        // TODO: kotlin.UninitializedPropertyAccessException: lateinit property newPdfs has not been initialized
////        newPdfs.clear()
//        if (::pdfList.isInitialized) {
//            val it = pdfList.iterator()
//            while (it.hasNext()) {
//                val pdf = it.next()
//                if (pdf.showBadge)
////                    newPdfs.add(pdf.file.uri.toString())
//            }
//        }

        super.onStop()

    }
// TODO: Activity results
//    DOCUMENT_PDF_SELECTION_REQUEST -> {
//        if (resultCode == Activity.RESULT_OK && data!!.data != null) {
//            val docName = data.data!!.toString()
//// TODO: The selected doc via the chooser needs to be somehow retrieved in a different manner
////                    val document = DocumentStorage.getInstance(this).getDocument(docName)
////                    openPDFsView()
////                    selectBottomNavigationViewMenuItem(R.id.viewer_pdfs)
////                    showFAB(R.id.viewer_add_pdf_fab)
////                    toolbar.title = getText(R.string.document_navigation_pdfs)
////
////                    if (Helper.isDocumentCropped(document))
////                        showPdfOcrDialog(document)
////                    else
////                        showNotCropDialog(document, false)
//        }
//    }

    ////            Permissions:
//            PERSISTABLE_URI_PERMISSION -> {
//                if (resultCode == Activity.RESULT_OK) {
//                    // The result data contains a URI for the document or directory that
//                    // the user selected.
//                    data?.data?.also { uri ->
//                        KtHelper.saveDocumentDir(this, uri)
//                        showDocumentDirSetDialog()
//                    }
//                }
//            }


    /**
     * Updates the UI in case the pdfList is empty and returns true.
     */
    private fun showEmptyList(): Boolean {


        if (pdfList.isEmpty()) {
            binding.pdfList.visibility = View.INVISIBLE
            binding.pdfEmptyLayout.visibility = View.VISIBLE
            return true
        }

        return false

    }


    //    TODO: check which functions can be taken from KtHelper
    private fun showDirectoryPermissionRequiredAlert() {

        val text = getString(R.string.pdf_fragment_persisted_permission_text)
        val title = getString(R.string.pdf_fragment_persisted_permission_title)

        val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.dialog_ok_text) { _, _ ->
                openDocScanDocumentDir()
            }
            .setNegativeButton(R.string.dialog_cancel_text, null)
            .setCancelable(true)
        alertDialogBuilder.create().show()

    }

    override fun onResume() {

        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val dir = getPdfDirectory()

//        The directory is not saved until now:
            when {
                dir == null -> {
                    showDirectoryPermissionRequiredAlert()
                }
                //        The directory is saved, check if the permission is also given (not sure if this can
                //        even happen).
                isPermissionGiven(dir) -> {
//                    updatePdfs()
                    binding.pdfList.layoutManager = LinearLayoutManager(context)
                }
                //        No permission is given for the folder - so ask for it:
                else -> {
                    showDirectoryPermissionRequiredAlert()
                }
            }
        } else {
//            updatePdfs()
            binding.pdfList.layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Returns true if a writable persistedUriPermission is given for a folder
     */
    private fun isPermissionGiven(folder: String): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val permissions = context?.contentResolver?.persistedUriPermissions
            if (permissions != null) {
                for (permission in permissions) {
                    if (permission.uri.toString() == folder)
                        return true
                }
            }
            return false
        } else
            return true

    }

    private fun getPdfs(): MutableList<Pdf> {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            getPdfsWithScopedStorage()
        else
            getPdfsWithDirectFilePaths()

    }

    private fun getPdfsWithDirectFilePaths(): MutableList<Pdf> {

        val dir: File? = Helper.getPDFStorageDir(getString(R.string.app_name))
        val pdfList: MutableList<Pdf> = ArrayList()

        if (dir != null) {
            val files = dir.listFiles() ?: return pdfList
            for (file in files) {
                val pdf = fileToPdf(DocumentFile.fromFile(file))
                pdf?.let {
//                    if (newPdfs.contains(file.absolutePath))
//                        it.showBadge = true
//                    pdfList.add(it)
                }

            }
        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList

    }

    // TODO: recheck this
    private fun getPdfsWithScopedStorage(): MutableList<Pdf> {

        val dir = getPdfDirectory() ?: return ArrayList()

        val uri = Uri.parse(dir)
        val uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )

        val pdfList: MutableList<Pdf> = ArrayList()
        if (uriFolder != null && context != null) {
            val cursor = context?.contentResolver?.query(
                uriFolder,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val pdfUri =
                        DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0))
                    val file = DocumentFile.fromSingleUri(requireContext(), pdfUri)
                    if (file?.type == "application/pdf") {
                        val pdf = fileToPdf(file)
                        pdf?.let { pdfList.add(it) }
                    }
                } while (cursor.moveToNext())
            }

            cursor?.close()

        }

//        sort the list based on the last modified date:
        pdfList.sort()

        return pdfList
    }


    private fun fileToPdf(file: DocumentFile): Pdf? {
        val fileSizeInBytes = file.length()
        val fileSize: Float = (fileSizeInBytes / (1024 * 1024).toFloat())
        val fileSizeInMB: String = "%.1f".format(fileSize)
        val date = formatDate(file.lastModified())

        return file.name?.let { Pdf(it, file, fileSizeInMB, date) }
    }

    private fun formatDate(date: Long): String {
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss")
        return format.format(date).toString()
    }

    class Pdf(val name: String, val file: DocumentFile, val fileSize: String, var date: String) :
        Comparable<Pdf> {

        //        the badge shows that the pdf is new:
        var showBadge = false

        override fun compareTo(other: Pdf): Int {
            return other.file.lastModified().compareTo(file.lastModified())
        }

    }


    private fun openPDFsView() {

        // TODO: Check badges, title fabs
////        Hide any badge if existing:
//        binding.bottomNav.removeBadge(R.id.viewer_pdfs)
////        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
////        ft.setCustomAnimations(
////            R.anim.translate_left_to_right_in,
////            R.anim.translate_left_to_right_out
////        )
////        ft.replace(R.id.viewer_fragment_layout, PdfFragment.newInstance(), PdfFragment.TAG).commit()
//        showFAB(R.id.viewer_add_pdf_fab)
//
//        toolbar.title = getText(R.string.document_navigation_pdfs)
    }


    private fun showPdfOptions(file: DocumentFile) {

        val sheetActions = ArrayList<SheetAction>()

        sheetActions.add(
            SheetAction(
                R.id.action_pdf_share_item,
                getString(R.string.action_pdf_share),
                R.drawable.ic_share_black_24dp
            )
        )

        sheetActions.add(
            SheetAction(
                R.id.action_pdf_delete_item,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp
            )
        )

//        val actionSheet = PdfActionSheet(file, sheetActions, this, this)
//        supportFragmentManager.beginTransaction().add(actionSheet, "TAG").commit()

    }

    // TODO: Handle deletion of documents
    private fun deletePdf(pdf: DocumentFile) {

        val name = pdf.name
        pdf.delete()

//        supportFragmentManager.findFragmentByTag(PdfFragment.TAG)?.apply {
//            //                    Scan again for the files:
//            if ((this as PdfFragment).isVisible)
//                updatePdfs()
//        }

//        showDocumentsDeletedSnackbar(name)
    }

    interface PdfListener {
        fun onPdfOptions(file: DocumentFile)
    }
}