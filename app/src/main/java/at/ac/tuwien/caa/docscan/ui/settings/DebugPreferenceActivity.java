package at.ac.tuwien.caa.docscan.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity;

/**
 * Created by fabian on 26.01.2018.
 */
public class DebugPreferenceActivity extends BaseNoNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_preferences);
        initWithTitle(R.string.debug_settings_title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_framelayout, new DebugPreferenceFragment())
                .commit();
    }

    public static class DebugPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.debug_preferences, rootKey);
        }
    }
}
