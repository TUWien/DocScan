package at.ac.tuwien.caa.docscan.ui.docviewer.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.LayoutPdflistRowBinding
import at.ac.tuwien.caa.docscan.logic.Helper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File

// TODO: EXPORT_LOGIC - Refactor this fragment to allow exports of documents as before.
class PdfAdapter(
    private val context: Context,
    private val pdfs: MutableList<PdfFragment.Pdf>,
    private val optionsListener: (DocumentFile) -> Unit
) :
    RecyclerView.Adapter<PdfAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(LayoutPdflistRowBinding.inflate(layoutInflater, parent, false))
    }

    override fun getItemCount() = pdfs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pdfs[position])
    }


    inner class ViewHolder(val binding: LayoutPdflistRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pdf: PdfFragment.Pdf) {
            binding.layoutPdflistRowTitle.text = pdf.name
            binding.layoutPdflistRowFilesize.text = pdf.fileSize + " MB"
            binding.layoutPdflistRowDate.text = pdf.date
            val iconRes =
                if (pdf.showBadge) R.drawable.ic_pdf_icon_badge else R.drawable.ic_pdf_icon
            binding.layoutPdflistRowIcon.setImageResource(iconRes)
            binding.layoutPdflistMoreButton.setOnClickListener { optionsListener(pdf.file) }
            pdf.showBadge = false

            itemView.setOnClickListener {

//                Determine the type of uri:
                val uri =
                    when (pdf.file.uri.scheme) {
                        "content" -> {
                            pdf.file.uri
                        }
//                        These are old fashioned direct file paths that were used before scoped
//                        storage. Use a file provider, because otherwise the location is exposed
//                        and an exception is thrown.
                        "file" -> {

                            val file = File(
                                Helper.getPDFStorageDir(
                                    context.getString(R.string.app_name)
                                ), pdf.file.name
                            )
                            FileProvider.getUriForFile(
                                context, "at.ac.tuwien.caa.fileprovider",
                                file
                            )
                        }
                        else -> {
                            null
                        }
                    }

                // TODO: Use Intent extensions for this check
                // uri should never be null, because the list is filtered for *.pdf extension
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        FirebaseCrashlytics.getInstance().recordException(e)

//                        Helper.showActivityNotFoundAlert(context)
                    }
                }
            }
        }
    }
}