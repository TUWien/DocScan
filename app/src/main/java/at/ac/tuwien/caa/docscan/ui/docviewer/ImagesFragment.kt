package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        galleryAdapter = ImagesAdapter(context, document)
        galleryAdapter.setFileName(document?.title)
        images_list.adapter = galleryAdapter
//        TODO: add here more columns for landscape mode:
        images_list.layoutManager = GridLayoutManager(context, 2)

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

    fun updateGallery(fileName: String) {
        Log.d(TAG, "updateGallery")
        galleryAdapter.updateImageView(fileName)
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