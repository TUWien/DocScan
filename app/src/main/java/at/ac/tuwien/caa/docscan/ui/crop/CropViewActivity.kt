package at.ac.tuwien.caa.docscan.ui.crop

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityCropViewBinding
import at.ac.tuwien.caa.docscan.databinding.CropInfoDialogBinding
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.ConsumableEvent
import at.ac.tuwien.caa.docscan.logic.DocumentPage
import at.ac.tuwien.caa.docscan.logic.GlideHelper
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Represents the activity for cropping and rotating a [Page]'s image file.
 *
 * The activity uses a custom imageView to display the page, in the very first step, the page file
 * is loaded into the imageView, the image is initially rotated INSIDE the imageview, i.e. the rotation
 * of the customImageView and the rotation of the image itself doesn't correspond!.
 *
 * Any subsequent operations performed on the image, like the rotation, is performed on the outer
 * custom view, this is important because otherwise the cropping points wouldn't get rotated and
 * scaled correctly. At this stage, the image is no longer rotated via the exif information of the image.
 *
 * As soon as the save operation is requested, the modified meta data is translated in the [Page]'s
 * structure and saved in the DB.
 *
 */
class CropViewActivity : BaseActivity() {

    companion object {
        private const val INITIAL_SCALE = 1.0F
        private const val EXCLUSION_THRESHOLD = 0.2F

        const val EXTRA_PAGE = "EXTRA_PAGE"
        fun newInstance(context: Context, page: Page): Intent {
            return Intent(context, CropViewActivity::class.java).apply {
                putExtra(EXTRA_PAGE, page)
            }
        }
    }

    private lateinit var binding: ActivityCropViewBinding
    private val viewModel: CropViewModel by viewModel { parametersOf(intent!!.extras) }

    // keep track of the image load, this is only necessary to happen exactly once per view lifetime.
    private var initialImageLoad = true
    private var scale = INITIAL_SCALE
    private val exclusionOffset by lazy { resources.getDimensionPixelSize(R.dimen.crop_view_exclusion_offset) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()

        // gesture navigation has been introduced since android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // some devices may use gestures for system wide back navigation, therefore,
            // the exclusion areas have to be determined, since otherwise, a cropping point on the edge
            // would trigger a back gesture very easily.
            lifecycleScope.launchWhenResumed {
                // this loop is necessary since the CropView doesn't have a callback when cropping points change.
                while (isActive) {
                    val exclusionRects = mutableListOf<Rect>()
                    with(binding.cropView) {
                        val bitmap = bitmap
                        if (cropPoints != null && bitmap != null) {
                            // the cropview's height is always its parent height, so in order to determine
                            // the correct coordinates, the bitmap, which is centered inside the cropview,
                            // needs to be taken into account for calculation of the offset.
                            val topOffset by lazy { (height - bitmap.height) / 2 }
                            // loop through all possible cropping points and check if they are on the
                            // start/end edges.
                            cropPoints.forEach {
                                if (it.x < EXCLUSION_THRESHOLD || it.x > (1 - EXCLUSION_THRESHOLD)) {
                                    val height = topOffset + (bitmap.height * it.y).toInt()
                                    val top = height - (exclusionOffset / 2)
                                    val bottom = height + (exclusionOffset / 2)
                                    val left =
                                        if (it.x < EXCLUSION_THRESHOLD) 0 else width - exclusionOffset
                                    val right =
                                        if (it.x < EXCLUSION_THRESHOLD) exclusionOffset else width
                                    exclusionRects.add(Rect(left, top, right, bottom))
                                }
                            }
                        }
                    }
                    ViewCompat.setSystemGestureExclusionRects(binding.cropView, exclusionRects)
                    delay(250)
                }
            }
        }
        observe()
    }

    private fun observe() {
        viewModel.observableInitBackNavigation.observe(this, ConsumableEvent {
            finish()
        })
        viewModel.observableError.observe(this, ConsumableEvent { throwable ->
            throwable.handleError(this, logAsWarning = true)
        })
        viewModel.observableShowCroppingInfo.observe(this, ConsumableEvent {
            MaterialAlertDialogBuilder(this).apply {
                val binding = CropInfoDialogBinding.inflate(layoutInflater)
                setView(binding.root)
                setTitle(R.string.crop_view_crop_dialog_title)
                setMessage(R.string.crop_view_crop_dialog_text)
                setCancelable(false)
                binding.openDocumentViewerButton.setOnClickListener {
                    startActivity(
                        DocumentViewerActivity.newInstance(
                            this@CropViewActivity,
                            DocumentPage(viewModel.page.docId, viewModel.page.id)
                        )
                    )
                    finish()
                }
                setPositiveButton(getString(R.string.button_ok)) { _, _ ->
                    viewModel.preferencesHandler.showCroppingInfo = !binding.skip.isChecked
                    viewModel.navigateBack()
                }
            }.show()
        })
        viewModel.observableModel.observe(this) {
            binding.cropView.setPoints(it.points)
            // the initial image load will position the image into the correct rotation.
            if (initialImageLoad) {
                initialImageLoad = false
                GlideHelper.loadFileIntoImageView(it.file, it.rotation, binding.cropView,
                    GlideHelper.GlideStyles.DEFAULT, {

                    }, { _, exception ->
                        Timber.e(exception, "Loading image into CropView has failed!")
                    }
                )
            } else {

                val measuredHeight = binding.cropView.height
                val measuredWidth = binding.cropView.width

                // it is important to note that his rotation does not correspond to the one of the
                // current image, this is because on the initial load, Glide may rotate the image
                // based on exif data but the rotation of the cropView is only performed afterwards.
                val currentImageViewRotation = binding.cropView.rotation
                // detect if rotation is requested
                val isRotationRequested =
                    it.previousRotation.angle.toFloat() != it.rotation.angle.toFloat()

                // only if the rotation is requested, the new scale is performed, this is necessary
                // because the whole custom imageView is rotated but not the image inside of it
                if (isRotationRequested) {
                    // we assume that a rotation is performed in 90CW steps, i.e. the scale fallbacks
                    // to its initial value
                    scale = if (scale != INITIAL_SCALE) {
                        INITIAL_SCALE
                    } else {
                        // 1. determine the new scale based on the ratio of the current device's imageView
                        // 2. we assume that the initial scale is adapted to the ratio of the current image,
                        // therefore the new scale can be performed based on the same ratio.
                        if (measuredHeight > measuredWidth) {
                            (it.meta.height.toFloat() / it.meta.width.toFloat())
                        } else {
                            (it.meta.width.toFloat() / it.meta.height.toFloat())
                        }
                    }
                }
                binding.cropView.resizeDimensions(scale)
                val animation = binding.cropView.animate()
                animation.apply {
                    // if the rotation differs, we assume that the user rotated the image 90°CW
                    if (isRotationRequested) {
                        rotation(currentImageViewRotation + 90F)
                        scaleX(scale)
                        scaleY(scale)
                    }
                    setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator?) {
                        }

                        override fun onAnimationEnd(p0: Animator?) {
                            viewModel.isRotating = false
                            binding.cropView.invalidate()
                        }

                        override fun onAnimationCancel(p0: Animator?) {
                            viewModel.isRotating = false
                        }

                        override fun onAnimationRepeat(p0: Animator?) {
                        }

                    })
                }

            }
        }
    }

    override fun onBackPressed() {
        viewModel.navigateBack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.crop_menu, menu)
        return true
    }

    override fun onPause() {
        super.onPause()
        binding.cropView.cropPoints?.let {
            viewModel.updateCroppingPoints(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (viewModel.isRotating) {
            return false
        }
        when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
            }
            R.id.rotate -> {
                viewModel.rotateBy90Degree(binding.cropView.cropPoints)
            }
            R.id.save -> {
                viewModel.save(binding.cropView.cropPoints)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbarInclude.mainToolbar)
        binding.toolbarInclude.mainToolbar.apply {
            setNavigationIcon(R.drawable.ic_clear_black_24dp)
        }
        supportActionBar?.title = ""
    }
}
