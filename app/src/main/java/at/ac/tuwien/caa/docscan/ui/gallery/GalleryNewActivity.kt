package at.ac.tuwien.caa.docscan.ui.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.ac.tuwien.caa.docscan.databinding.ActivityGalleryBinding
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

// TODO: This class is probably not necessary
class GalleryNewActivity : AppCompatActivity() {

    private val viewModel: GalleryViewModel by viewModel { parametersOf(intent.extras!!) }
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var selectionToolbar: SelectionToolbar

    companion object {
        const val EXTRA_DOCUMENT_ID = "EXTRA_DOCUMENT_ID"

        fun newInstance(context: Context, docId: UUID): Intent {
            return Intent(context, GalleryNewActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_ID, docId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        selectionToolbar = SelectionToolbar(this, binding.mainToolbar, binding.galleryAppbar)
        observe()
    }

    private fun observe() {
        viewModel.observableDocument.observe(this, {
            binding.mainToolbar.title = it.document.title
            binding.mainToolbar.title = it.document.title

        })

        viewModel.observableCloseGallery.observe(this, {
            it.getContentIfNotHandled()?.let {
                finish()
            }
        })
    }
}