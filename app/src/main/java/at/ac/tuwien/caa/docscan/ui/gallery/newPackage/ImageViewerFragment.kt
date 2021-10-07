package at.ac.tuwien.caa.docscan.ui.gallery.newPackage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import at.ac.tuwien.caa.docscan.databinding.FragmentImageViewerBinding
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.Helper
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.*

class ImageViewerFragment : Fragment() {

    private val viewModel: ImageViewModel by viewModel { parametersOf(requireArguments()) }
    private val fileHandler by inject<FileHandler>()
    private lateinit var binding: FragmentImageViewerBinding

    companion object {
        const val ARG_PAGE_ID = "ARG_PAGE_ID"
        fun newInstance(pageId: UUID): ImageViewerFragment {
            return ImageViewerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PAGE_ID, pageId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageViewerBinding.inflate(inflater, container, false)
        binding.imageViewerImageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
    }

    private fun observe() {
        viewModel.observablePage.observe(viewLifecycleOwner, { page ->
            // TODO: Add loading state
            binding.imageViewerImageView.apply {
                transitionName = page.id.toString()

                // TODO: Check if the nullability should be ever handled
                fileHandler.getFileByPage(page)?.let {
                    setImage(ImageSource.uri(it.absolutePath))

                    // TODO: Check if this decoding stuff needs to be performed here
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(it.absolutePath, options)

                    var imageHeight = options.outHeight
                    var imageWidth = options.outWidth

                    val angle = Helper.getAngleFromExif(Helper.getSafeExifOrientation(it))
                    if (angle == 0 || angle == 180) {
                        imageHeight = options.outWidth;
                        imageWidth = options.outHeight;
                    }

                    // TODO: Add this to the page meta data, so it can be set here.
//                    PageDetector.PageFocusResult result =
//                    PageDetector.getScaledCropPoints(mFileName, imageHeight, imageWidth);
//                    if (result != null)
//                        mImageView.setPoints(result.getPoints(), result.isFocused());

                    // TODO: If not cropped, then reset points
//                    resetPoints()

                }
            }
        })
    }

}