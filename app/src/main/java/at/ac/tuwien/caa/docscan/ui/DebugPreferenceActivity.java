package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 26.01.2018.
 */

public class DebugPreferenceActivity extends BaseNoNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug_settings);

        super.initToolbarTitle(R.string.no_file_found_title);

//        Note we use a PreferenceFragment here instead of PreferenceActivity because it is advised in the API for versions > android 3.0
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_framelayout, new DebugPreferenceFragment())
                .commit();


    }

    public static class DebugPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.debug_preferences);

        }
    }

}
