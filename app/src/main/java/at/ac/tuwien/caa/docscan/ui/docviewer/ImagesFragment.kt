package at.ac.tuwien.caa.docscan.ui.docviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.gallery.GalleryAdapter
import at.ac.tuwien.caa.docscan.logic.Document
import kotlinx.android.synthetic.main.fragment_documents.*
import kotlinx.android.synthetic.main.fragment_images.*
import me.drakeet.support.toast.ToastCompat

class ImagesFragment(private val document: Document?) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val adapter = GalleryAdapter(context, document)
        adapter.setFileName(document?.title)
        images_list.adapter = adapter
//        images_list.adapter
//        TODO: add here more columns for landscape mode:
        images_list.layoutManager = GridLayoutManager(context, 2)

    }
}