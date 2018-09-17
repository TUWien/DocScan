package at.ac.tuwien.caa.docscan.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.license_list_item.view.*
import java.util.*


class LicenseAdapter(val licenseList: ArrayList<LicensesActivity.License>) : RecyclerView.Adapter<LicenseAdapter.ViewHolder>() {




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.license_list_item, parent, false))
    }

    override fun getItemCount(): Int {

        return licenseList.size

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val license = licenseList.get(position)


        holder.title.text = license.mName
        holder.copyright.text = license.mCopyright
        holder.license_text.text = license.mLicense
        holder.url.text = license.mUrl


//        with (holder) {
//            title.text = license.mName
//            copyright.text = license.mCopyright
//            license_text.text = license.mLicense
//            url.text = license.mUrl
//        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title = view.title
        val copyright = view.copyright
        val license_text = view.license
        val url = view.url

    }

}