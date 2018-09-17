package at.ac.tuwien.caa.docscan.ui

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.activity_licenses.*

class LicensesActivity : BaseNoNavigationActivity () {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)



//        var list = ArrayList<String>(Arrays.asList(*licensesStrings))
//        var list = ArrayList(resources.getStringArray(R.array.license_array).toMutableList())
//
//        val licenses = convertToLicenseList(list)
        val licenseAdapater = LicenseAdapter(generateLicenses())

//        with(licenses_recycler_view) {
            licenses_recycler_view.adapter = licenseAdapater
            licenses_recycler_view.layoutManager = LinearLayoutManager(this@LicensesActivity)
//        }

        var toolbar: Toolbar = findViewById(R.id.main_toolbar)
        toolbar.title = "Open Source Licenses"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /*
        mToolbar = findViewById(R.id.main_toolbar);
        mToolbar.setTitle(mDocument.getTitle());

        AppBarLayout appBarLayout = findViewById(R.id.gallery_appbar);

//        Enable back navigation in action bar:
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
         */

    }

    class License(name: String, copyright: String, license: String, url: String) {
        var mName: String = name
        var mCopyright: String = copyright
        var mLicense: String = license
        var mUrl: String = url

    }

}