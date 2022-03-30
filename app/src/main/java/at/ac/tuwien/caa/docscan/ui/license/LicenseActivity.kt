package at.ac.tuwien.caa.docscan.ui.license

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityLicensesBinding
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity

class LicenseActivity : BaseNoNavigationActivity() {

    private lateinit var binding: ActivityLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWithTitle(R.string.license_title)
        val licenseAdapter = LicenseAdapter(generateLicenses())

        binding.licensesRecyclerView.adapter = licenseAdapter
        binding.licensesRecyclerView.layoutManager = LinearLayoutManager(this@LicenseActivity)
    }

    class License(name: String, copyright: String, license: String, url: String) {

        var mName: String = name
        var mCopyright: String = copyright
        var mLicense: String = license
        var mUrl: String = url

    }
}
