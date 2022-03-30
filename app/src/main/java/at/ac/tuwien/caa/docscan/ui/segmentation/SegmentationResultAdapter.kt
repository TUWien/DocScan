package at.ac.tuwien.caa.docscan.ui.segmentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.databinding.ItemSegmentationResultBinding
import at.ac.tuwien.caa.docscan.ui.segmentation.model.ModelExecutionResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

/**
 * @author matejbart
 */
class SegmentationResultAdapter(val estimatedItemWidth: Int) :
    ListAdapter<ModelExecutionResult, SegmentationResultAdapter.ViewHolder>(
        ModelExecutionResultDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSegmentationResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemSegmentationResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ModelExecutionResult) {
            binding.container.layoutParams.height = estimatedItemWidth
            binding.container.layoutParams.width = estimatedItemWidth
            Glide.with(binding.root.context).load(model.bitmapResult).transition(
                DrawableTransitionOptions.withCrossFade()
            )
                .into(binding.image)
            binding.model = model
        }
    }

}

class ModelExecutionResultDiffCallback : DiffUtil.ItemCallback<ModelExecutionResult>() {
    override fun areItemsTheSame(
        oldItem: ModelExecutionResult,
        newItem: ModelExecutionResult
    ) = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: ModelExecutionResult,
        newItem: ModelExecutionResult
    ) = oldItem == newItem
}
