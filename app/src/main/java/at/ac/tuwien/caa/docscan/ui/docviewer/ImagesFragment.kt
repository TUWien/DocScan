package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_images.*
import java.io.File


class ImagesFragment(private val document: Document?) : Fragment() {

    companion object {
        val TAG = "ImagesFragment"
    }


    private lateinit var galleryAdapter: ImagesAdapter
    private var scrollToFileName = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(at.ac.tuwien.caa.docscan.R.layout.fragment_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated: scrollToFile: " + scrollToFileName)
        galleryAdapter = ImagesAdapter(context, document)
        galleryAdapter.setDocumentName(document?.title)
        galleryAdapter.setScrollFileName(scrollToFileName)
        images_list.adapter = galleryAdapter
//        TODO: add here more columns for landscape mode:
        images_list.layoutManager = GridLayoutManager(context, 2)

    }

    fun scrollToFile(fileName: String) {

        Log.d(TAG, "scrollToFile" + fileName)
        scrollToFileName = fileName
        Log.d(TAG, "not empty: " + scrollToFileName.isNotEmpty())

    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: " + scrollToFileName)

        if (scrollToFileName.isNotEmpty()) {

            Log.d(TAG, "scroll: " + scrollToFileName)
            val idx = document!!.filePaths.indexOf(scrollToFileName)
            Log.d(TAG, "scrollidx: " + idx)
            images_list.scrollToPosition(idx)
//            startPostponedEnterTransition()
//            images_list.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
//                override fun onPreDraw(): Boolean {
//                    images_list.viewTreeObserver.removeOnPreDrawListener(this)
//                    // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
//                    images_list.requestLayout()
//                    startPostponedEnterTransition()
//                    return true
//                }
//            })
        }


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
//            galleryAdapter.notifyItemRangeChanged(selIdx, document.pages.size)

            DocumentStorage.saveJSON(context)

        }

        galleryAdapter.deselectAllItems()


    }

}