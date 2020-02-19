package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_images.*
import java.io.File


class ImagesFragment : Fragment() {

    companion object {
        fun newInstance(bundle: Bundle): ImagesFragment {
            val fragment = ImagesFragment()
            fragment.arguments = bundle
            return fragment
        }

        val TAG = "ImagesFragment"
        val DOCUMENT_NAME_KEY = "DOCUMENT_NAME_KEY"
    }


    private lateinit var document: Document
    private lateinit var galleryAdapter: ImagesAdapter
    private var scrollToFileName = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        if (::galleryAdapter.isInitialized) {
            images_list.adapter = galleryAdapter
//        TODO: add here more columns for landscape mode:
            images_list.layoutManager = GridLayoutManager(context, 2)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.images_menu, menu)

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(DOCUMENT_NAME_KEY)?.let {
            val d: Document? = DocumentStorage.getInstance(context).getDocument(it)
            if (d != null) {
                document = d
                galleryAdapter = ImagesAdapter(context, document)
                galleryAdapter.setDocumentName(document?.title)
                galleryAdapter.setScrollFileName(scrollToFileName)
            }
        }

    }


    fun scrollToFile(fileName: String) {

        Log.d(TAG, "scrollToFile" + fileName)
        scrollToFileName = fileName
        Log.d(TAG, "not empty: " + scrollToFileName.isNotEmpty())

    }


    override fun onResume() {
        super.onResume()

        if (!emptyImageList() && scrollToFileName.isNotEmpty()) {
            val idx = document!!.filePaths.indexOf(scrollToFileName)
            images_list.scrollToPosition(idx)
        }


    }

    /**
     * Updates the UI in case the images list is empty and returns true.
     */
    private fun emptyImageList(): Boolean {

        if (document.pages.isEmpty()) {
            images_list.visibility = View.INVISIBLE
            images_empty_layout.visibility = View.VISIBLE
            return true
        }

        return false

    }

    /**
     * This updates all items. This is particulary necessary to hide all checkboxes:
     */
    fun redrawItems() {
        galleryAdapter.notifyDataSetChanged()
    }

    fun deselectAllItems() {
        galleryAdapter.deselectAllItems()
    }

    fun getSelectedFiles(): ArrayList<File> {
        return galleryAdapter.selectedFiles
    }

    fun getSelectionCount(): Int {
        return galleryAdapter.selectedFiles.size
    }


    /**
     * Just updates the adapter, to show the loading circles.
     */
    fun showCropStart() {
        galleryAdapter.notifyDataSetChanged()
    }

    fun updateDocumentName(fileName: String) {
        galleryAdapter.setDocumentName(fileName)
    }

    fun updateGallery(fileName: String) {
        Log.d(TAG, "updateGallery")
        galleryAdapter.updateImageView(fileName)
    }

    fun selectAll() {
        galleryAdapter.selectAllItems()
    }

    fun deleteSelections() {

        if (document == null || document.pages == null) {
            Helper.crashlyticsLog(TAG, "rotateSelectedItems",
                    "mDocument == null || mAdapter == null || mDocument.getPages() == null || " + "                mAdapter == null")
            return
        }

        val selections = galleryAdapter.selectionIndices

        for (i in selections.indices.reversed()) {

            val selIdx = selections[i]

            val page = document.pages.removeAt(selIdx)
            val fileName = page.file.absolutePath
            //            Log.d(CLASS_NAME, "deleteSelections: deleting index: " + selIdx + " filename: " + fileName);

            val isFileDeleted = File(fileName).delete()
            if (!isFileDeleted)
                Helper.crashlyticsLog(TAG, "deleteSelections",
                        "file not deleted")

            galleryAdapter.notifyItemRemoved(selIdx)
            DocumentStorage.saveJSON(context)

        }

        galleryAdapter.deselectAllItems()

        emptyImageList()

    }

}