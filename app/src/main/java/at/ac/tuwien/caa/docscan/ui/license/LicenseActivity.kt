package at.ac.tuwien.caa.docscan.ui.license

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.Toolbar
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity
import kotlinx.android.synthetic.main.activity_licenses.*

class LicenseActivity : BaseNoNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)
        val licenseAdapter = LicenseAdapter(generateLicenses())

        licenses_recycler_view.adapter = licenseAdapter
        licenses_recycler_view.layoutManager = LinearLayoutManager(this@LicenseActivity)

        var toolbar: Toolbar = findViewById(R.id.main_toolbar)
        toolbar.title = getString(R.string.license_title)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    class License(name: String, copyright: String, license: String, url: String) {

        var mName: String = name
        var mCopyright: String = copyright
        var mLicense: String = license
        var mUrl: String = url

    }

}