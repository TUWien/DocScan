package at.ac.tuwien.caa.docscan.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity

class ExifPreferenceActivity : BaseNoNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_preferences)
        initToolbarTitle(R.string.exif_settings_title)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_framelayout, ExifPreferenceFragment())
            .commit()
    }

    class ExifPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.exif_preferences, rootKey)
        }
    }
}
