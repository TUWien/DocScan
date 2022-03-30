package at.ac.tuwien.caa.docscan.ui.settings

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import org.koin.android.ext.android.inject

class ExifPreferenceActivity : BaseNoNavigationActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_preferences)
        initWithTitle(R.string.exif_settings_title)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_framelayout, ExifPreferenceFragment())
            .commit()
    }

    class ExifPreferenceFragment : PreferenceFragmentCompat() {

        private val preferencesHandler by inject<PreferencesHandler>()
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.exif_preferences, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val artistPref = findPreference<EditTextPreference>(getString(R.string.key_exif_artist))
            artistPref?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    (newValue as? String)?.let {
                        setSummary(preference, it)
                    }
                    return@OnPreferenceChangeListener true
                }
            artistPref?.let {
                setSummary(it, preferencesHandler.exifArtist)
            }

            val copyRight =
                findPreference<EditTextPreference>(getString(R.string.key_exif_copyright))
            copyRight?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    (newValue as? String)?.let {
                        setSummary(preference, it)
                    }
                    return@OnPreferenceChangeListener true
                }
            copyRight?.let {
                setSummary(it, preferencesHandler.exifCopyRight)

            }
        }

        private fun setSummary(preference: Preference, summary: String?) {
            summary?.let {
                if (it.isNotEmpty()) {
                    preference.summary = it
                    return
                }
            }
            preference.summary = getString(R.string.settings_exif_none)
            return
        }
    }
}
