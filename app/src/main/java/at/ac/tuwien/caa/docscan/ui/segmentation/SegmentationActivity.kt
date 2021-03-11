package at.ac.tuwien.caa.docscan.ui.segmentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.AdapterView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivitySegmentationBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * The segmentation activity which displays segmentation masks/results.
 *
 * @author matejbart
 */
class SegmentationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySegmentationBinding
    private val viewModel: SegmentationViewModel by viewModel { parametersOf(intent!!.extras) }
    private lateinit var adapter: SegmentationResultAdapter

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_segmentation)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        bottomSheetBehavior = from(binding.bottomSheet)
        bottomSheetBehavior.state = STATE_COLLAPSED
        bottomSheetBehavior.state = STATE_HIDDEN
        subscribeToModel()

        val result = computeBestItemSize()

        adapter = SegmentationResultAdapter(result.second)
        // the dropdown needs to be set only once when the activity is first created, otherwise
        // it will drop the adapter.
        binding.model.setText(viewModel.latestModel.title, false)
        ArrayAdapterNoFilter(
            this,
            android.R.layout.simple_list_item_1,
            viewModel.models.map { model -> model.title }
        ).also { adapter ->
            binding.model.setAdapter(adapter)
        }

        binding.model.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val current = parent?.getItemAtPosition(position).toString()
                viewModel.performSegmentationByModelName(
                    current,
                    binding.cbGpu.isChecked,
                    binding.cbAll.isChecked
                )
            }
        binding.cbGpu.setOnCheckedChangeListener { view, isChecked ->
            if (!view.isPressed) {
                return@setOnCheckedChangeListener
            }
            viewModel.performSegmentationByModelName(null, isChecked, binding.cbAll.isChecked)
        }

        binding.cbAll.setOnCheckedChangeListener { view, isChecked ->
            if (!view.isPressed) {
                return@setOnCheckedChangeListener
            }
            viewModel.performSegmentationByModelName(null, binding.cbGpu.isChecked, isChecked)
            binding.model.isEnabled = !isChecked
            binding.modelInputLayout.isEnabled = !isChecked
        }

        binding.list.layoutManager = GridLayoutManager(this, result.first)
        binding.list.adapter = adapter
    }

    private fun subscribeToModel() {
        viewModel.observableImagePath.observe(this, {
            it.getContentIfNotHandled()?.let { imagePath ->
                Glide.with(this).load(File(imagePath))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.image)
            }
        })

        viewModel.observableResults.observe(this, {
            adapter.submitList(it)
            it.firstOrNull()?.let { first ->
                binding.result = first
            }
            if (it.size < 2) {
                binding.list.visibility = View.GONE
                binding.image.visibility = View.VISIBLE
                Glide.with(this).load(it.firstOrNull()?.bitmapResult).transition(
                    DrawableTransitionOptions.withCrossFade()
                ).into(binding.image)
            } else {
                binding.list.visibility = View.VISIBLE
                binding.image.visibility = View.GONE
            }
            bottomSheetBehavior.isHideable = false
            bottomSheetBehavior.state = STATE_COLLAPSED
        })

        viewModel.observableError.observe(this, {
            it.getContentIfNotHandled()?.let { exception ->
                MaterialAlertDialogBuilder(this).setTitle("Error").setMessage(exception.message)
                    .create().show()
            }
        })
    }

    /**
     * @return the best span count and item size for the adapter so that it will best fit.
     */
    private fun computeBestItemSize(): Pair<Int, Int> {
        val itemMargin =
            resources.getDimensionPixelSize(R.dimen.item_model_result_margin)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        // window size minus the
        val availableSize = width - 2 * itemMargin

        val defaultItemSize =
            resources.getDimensionPixelSize(R.dimen.item_model_result_size)
        val stepSize =
            resources.getDimensionPixelSize(R.dimen.item_model_result_size_step)
        val bufferSize =
            resources.getDimensionPixelSize(R.dimen.item_model_result_buffer)

        var bestItemSize = defaultItemSize - itemMargin
        var spanCount = availableSize / bestItemSize
        var remainingSize =
            availableSize - spanCount * bestItemSize

        // loop between minus buffer and plus buffer and get the best span and size with the least remaining size
        // so in the end we have a size and a fitter where the space between the elements is as small as possible.
        (defaultItemSize - bufferSize until defaultItemSize + bufferSize step stepSize).forEach {
            val currentItemSize = it
            val amount = availableSize / (currentItemSize + itemMargin)
            val currentRemaining = availableSize - (amount * (currentItemSize + itemMargin))
            if (currentRemaining < remainingSize) {
                remainingSize = currentRemaining
                // we need to subtract the margin since the offset won't be automatically set
                bestItemSize = currentItemSize - itemMargin
                spanCount = amount
            }
        }
        return Pair(spanCount, bestItemSize)
    }


    companion object {

        const val EXTRA_IMAGE_FILE_PATH = "EXTRA_IMAGE_FILE_PATH"

        fun newInstance(context: Context, path: String): Intent {
            return Intent(context, SegmentationActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_FILE_PATH, path)
            }
        }
    }
}

data class Label(val name: String, @ColorInt val colorInt: Int)
