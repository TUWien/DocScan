package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.FragmentImagesBinding
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import kotlinx.android.synthetic.main.fragment_images.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ImagesFragment : Fragment() {

    companion object {

        /**
         * @param documentId the id of the document for the image files should be displayed.
         * @param fileId the id of the file which should be visible.
         */
        fun newInstance(documentId: UUID, fileId: UUID?): ImagesFragment {
            val fragment = ImagesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DOCUMENT_ID, documentId)
                    putSerializable(ARG_FILE_ID, fileId)
                }
            }
            return fragment
        }

        val TAG = "ImagesFragment"
        // TODO: Remove the key
        const val DOCUMENT_NAME_KEY = "DOCUMENT_NAME_KEY"
        const val ARG_DOCUMENT_ID = "ARG_DOCUMENT_ID"
        const val ARG_FILE_ID = "ARG_FILE_ID"
    }

    private val viewModel: ImagesViewModel by viewModel {
        parametersOf(requireArguments())
    }
    private lateinit var binding: FragmentImagesBinding
    private lateinit var galleryAdapter: ImagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imagesList.adapter = galleryAdapter
        //        TODO: add here more columns for landscape mode:
        binding.imagesList.layoutManager = GridLayoutManager(context, 2)
        observe()
    }

    private fun observe() {
//        viewModel.observableDocument.observe(viewLifecycleOwner, {
//            galleryAdapter.files
//        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.images_menu, menu)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(DOCUMENT_NAME_KEY)?.let {
            val d: Document? = DocumentStorage.getInstance(context).getDocument(it)
            if (d != null) {
                // TODO: Use the new image adapter here
//                document = d
//                galleryAdapter = ImagesAdapter(context, document)
//                galleryAdapter.setDocumentName(document?.title)
//                galleryAdapter.setScrollFileName(scrollToFileName)
            }
        }

    }


    /**
     * Updates the UI in case the images list is empty and returns true.
     */
    private fun emptyImageList(): Boolean {

        // TODO: Check the empty state of the images
//        if (document.pages.isEmpty()) {
//            images_list.visibility = View.INVISIBLE
//            images_empty_layout.visibility = View.VISIBLE
//            return true
//        }

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

//    fun deleteSelections() {
//
//        if (document == null || document.pages == null) {
//            Helper.crashlyticsLog(
//                TAG, "rotateSelectedItems",
//                "mDocument == null || mAdapter == null || mDocument.getPages() == null || " + "                mAdapter == null"
//            )
//            return
//        }
//
//        val selections = galleryAdapter.selectionIndices
//
//        for (i in selections.indices.reversed()) {
//
//            val selIdx = selections[i]
//
//            val page = document.pages.removeAt(selIdx)
//            val fileName = page.file.absolutePath
//            //            Log.d(CLASS_NAME, "deleteSelections: deleting index: " + selIdx + " filename: " + fileName);
//
//            val isFileDeleted = File(fileName).delete()
//            if (!isFileDeleted)
//                Helper.crashlyticsLog(
//                    TAG, "deleteSelections",
//                    "file not deleted"
//                )
//
//            galleryAdapter.notifyItemRemoved(selIdx)
////            DocumentStorage.saveJSON(context)
//
//        }
//
//        DocumentStorage.saveJSON(context)
//
//        galleryAdapter.deselectAllItems()
//
//        emptyImageList()
//
//    }

}