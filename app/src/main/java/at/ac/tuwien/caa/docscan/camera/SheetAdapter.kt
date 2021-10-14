package at.ac.tuwien.caa.docscan.camera

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.databinding.SheetActionListItemBinding
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp

class SheetAdapter(
    private val actionList: List<SheetAction>,
    private val listener: (SheetAction) -> Unit
) : RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SheetActionListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = actionList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(actionList[position])
    }

    inner class ViewHolder(val binding: SheetActionListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sheetAction: SheetAction) {
            GlideApp.with(binding.sheetItemImageview).load(sheetAction.icon)
                .into(binding.sheetItemImageview)
            binding.sheetItemTextview.text = sheetAction.text
            binding.root.setOnClickListener {
                listener(sheetAction)
            }
        }
    }
}
