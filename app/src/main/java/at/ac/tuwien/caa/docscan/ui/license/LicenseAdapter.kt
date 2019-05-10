package at.ac.tuwien.caa.docscan.ui.license

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.license_list_item.view.*
import java.util.*


class LicenseAdapter(private val licenseList: ArrayList<LicenseActivity.License>) : RecyclerView.Adapter<LicenseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.license_list_item, parent, false))

    }

    override fun getItemCount(): Int {

        return licenseList.size

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val license = licenseList[position]

        with (holder) {
            title.text = license.mName
            copyright.text = license.mCopyright
            licenseText.text = license.mLicense
            url.text = license.mUrl
        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.title!!
        val copyright = view.copyright!!
        val licenseText = view.license!!
        val url = view.url!!

    }

}