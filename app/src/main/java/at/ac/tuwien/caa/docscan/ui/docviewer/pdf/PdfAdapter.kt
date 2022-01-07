package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.LayoutPdflistRowBinding
import at.ac.tuwien.caa.docscan.extensions.sizeMB
import java.text.SimpleDateFormat
import java.util.*

class PdfAdapter(private val select: (ExportList) -> Unit, private val options: (ExportList) -> Unit)
    : ListAdapter<ExportList, PdfAdapter.ViewHolder>(PDFAdapterDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(LayoutPdflistRowBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(val binding: LayoutPdflistRowBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(export: ExportList) {
            when (export) {
                is ExportList.ExportHeader -> {

                }
                is ExportList.File -> {
                    binding.layoutPdflistRowTitle.text = export.name
                    binding.layoutPdflistRowFilesize.text = export.documentFile.sizeMB() + " MB"
                    binding.layoutPdflistRowDate.text = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(export.documentFile.lastModified()).toString()
                    // TODO: Check badge support
                    val iconRes =
                            if (true) R.drawable.ic_pdf_icon_badge else R.drawable.ic_pdf_icon
                    binding.layoutPdflistRowIcon.setImageResource(iconRes)
                    binding.layoutPdflistMoreButton.setOnClickListener {
                        options(export)
                    }
//                    pdf.showBadge = false

                    itemView.setOnClickListener {
                        select.invoke(export)
                    }
                }
            }
        }
    }
}

// TODO: Check diff utils
class PDFAdapterDiff : DiffUtil.ItemCallback<ExportList>() {
    override fun areItemsTheSame(
            oldItem: ExportList,
            newItem: ExportList
    ): Boolean {
        val oldFile = oldItem as? ExportList.File
        val newFile = newItem as? ExportList.File
        return oldFile?.name == newFile?.name && oldFile?.pageFileType == newFile?.pageFileType
    }

    override fun areContentsTheSame(
            oldItem: ExportList,
            newItem: ExportList
    ): Boolean {
        val oldFile = oldItem as? ExportList.File
        val newFile = newItem as? ExportList.File
        return oldFile?.name == newFile?.name && oldFile?.pageFileType == newFile?.pageFileType
    }
}
