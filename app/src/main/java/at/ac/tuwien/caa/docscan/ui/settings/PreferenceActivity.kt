package at.ac.tuwien.caa.docscan.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.repository.DocumentRepository
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer.NavigationItem
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber

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

        private val documentRepository by inject<DocumentRepository>()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            val preferenceKey = resources.getString(R.string.key_debug_settings)
            findPreference<Preference>(preferenceKey)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    val intent = Intent(requireActivity(), DebugPreferenceActivity::class.java)
                    startActivity(intent)
                    true
                }
            val exifKey = resources.getString(R.string.key_exif_settings)
            findPreference<Preference>(exifKey)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    val intent = Intent(requireActivity(), ExifPreferenceActivity::class.java)
                    startActivity(intent)
                    true
                }
            val mobileKey = resources.getString(R.string.key_upload_mobile_data)
            findPreference<Preference>(mobileKey)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    if (newValue is Boolean) {
                        Timber.i("Preference option metered uploads: $newValue")
                    }
                    lifecycleScope.launchWhenResumed {
                        withContext(Dispatchers.IO) {
                            when (val result = documentRepository.cancelAllPendingUploads()) {
                                is Failure -> {
                                    // ignore
                                }
                                is Success -> {
                                    if (result.data) {
                                        ADialog.DialogAction.UPLOADS_CANCELLED_DUE_CONSTRAINTS_CHANGE.show(
                                            parentFragmentManager
                                        )
                                    }
                                }
                            }
                        }
                    }
                    true
                }
            val crashReportsKey = resources.getString(R.string.key_crash_reports)
            findPreference<Preference>(crashReportsKey)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    if (newValue is Boolean) {
                        // crashlytics and analytics are kind of tight together, therefore both needs to be explicitly enabled/disabled.
//                        FirebaseCrashlytics.getInstance()
//                            .setCrashlyticsCollectionEnabled(newValue as Boolean?)
//                        FirebaseAnalytics.getInstance(requireContext())
//                            .setAnalyticsCollectionEnabled((newValue as Boolean?)!!)
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
