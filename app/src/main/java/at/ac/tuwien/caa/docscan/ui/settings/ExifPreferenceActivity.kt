package at.ac.tuwien.caa.docscan.ui.settings

import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceFragment
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity

class ExifPreferenceActivity : BaseNoNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_preferences)

        initToolbarTitle(R.string.exif_settings_title)
//        Note we use a PreferenceFragment here instead of PreferenceActivity because it is advised in the API for versions > android 3.0
        // Display the fragment as the main content.
        fragmentManager.beginTransaction()
            .replace(R.id.settings_framelayout, ExifPreferenceFragment())
            .commit()

    }

    class ExifPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.exif_preferences)
        }


    }

}