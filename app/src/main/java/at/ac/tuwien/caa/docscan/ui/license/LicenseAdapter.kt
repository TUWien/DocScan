package at.ac.tuwien.caa.docscan.ui.license

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.databinding.LicenseListItemBinding
import java.util.*


class LicenseAdapter(private val licenseList: ArrayList<LicenseActivity.License>) :
    RecyclerView.Adapter<LicenseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(LicenseListItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun getItemCount() = licenseList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(licenseList[position])
    }

    class ViewHolder(val binding: LicenseListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(license: LicenseActivity.License) {
            binding.apply {
                title.text = license.mName
                copyright.text = license.mCopyright
                this.license.text = license.mLicense
                url.text = license.mUrl
            }
        }
    }
}
