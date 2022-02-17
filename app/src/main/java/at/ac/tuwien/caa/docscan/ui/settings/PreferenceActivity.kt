package at.ac.tuwien.caa.docscan.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer.NavigationItem
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.show
import at.ac.tuwien.caa.docscan.worker.cancelAllScheduledAndRunningUploadJobs
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Created by fabian on 26.01.2018.
 */
class PreferenceActivity : BaseNavigationActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_framelayout, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            val preferenceKey = resources.getString(R.string.key_debug_settings)
            findPreference<Preference>(preferenceKey)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    val intent = Intent(requireActivity(), DebugPreferenceActivity::class.java)
                    startActivity(intent)
                    true
                }
            val exifKey = resources.getString(R.string.key_exif_settings)
            findPreference<Preference>(exifKey)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    val intent = Intent(requireActivity(), ExifPreferenceActivity::class.java)
                    startActivity(intent)
                    true
                }
            val mobileKey = resources.getString(R.string.key_upload_mobile_data)
            findPreference<Preference>(mobileKey)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                    if (cancelAllScheduledAndRunningUploadJobs(
                            requireContext(),
                            WorkManager.getInstance(requireContext())
                        )
                    ) {
                        ADialog.DialogAction.UPLOADS_CANCELLED_DUE_CONSTRAINTS_CHANGE.show(
                            parentFragmentManager
                        )
                    }
                    true
                }
            val crashReportsKey = resources.getString(R.string.key_crash_reports)
            findPreference<Preference>(crashReportsKey)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    if (newValue is Boolean) {
                        // crashlytics and analytics are kind of tight together, therefore both needs to be explicitly enabled/disabled.
                        FirebaseCrashlytics.getInstance()
                            .setCrashlyticsCollectionEnabled(newValue as Boolean?)
                        FirebaseAnalytics.getInstance(requireContext())
                            .setAnalyticsCollectionEnabled((newValue as Boolean?)!!)
                    }
                    true
                }
        }
    }

    override val selfNavDrawerItem = NavigationItem.SETTINGS

    companion object {
        fun newInstance(context: Context?): Intent {
            return Intent(context, PreferenceActivity::class.java)
        }
    }
}
