package at.ac.tuwien.caa.docscan.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer;

/**
 * Created by fabian on 26.01.2018.
 */

public class PreferenceActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

//        Note we use a PreferenceFragment here instead of PreferenceActivity because it is advised in the API for versions > android 3.0
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_framelayout, new SettingsFragment())
                .commit();


    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            String preferenceKey = getResources().getString(R.string.key_debug_settings);
            findPreference(preferenceKey).setOnPreferenceClickListener(new android.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), DebugPreferenceActivity.class);
                    startActivity(intent);
//                    getFragmentManager().beginTransaction().replace( R.id.settings_framelayout, new NestedSettingsFragment() ).addToBackStack( NestedSettingsFragment.class.getSimpleName() ).commit();
                    return true;
                }

            });

            String exifKey = getResources().getString(R.string.key_exif_settings);
            findPreference(exifKey).setOnPreferenceClickListener(new android.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    Intent intent = new Intent(getActivity().getApplicationContext(), ExifPreferenceActivity.class);
                    startActivity(intent);
//                    getFragmentManager().beginTransaction().replace( R.id.settings_framelayout, new NestedSettingsFragment() ).addToBackStack( NestedSettingsFragment.class.getSimpleName() ).commit();
                    return true;
                }

            });
        }
    }

    public static class NestedSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.debug_preferences);

        }

    }


    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.SETTINGS;
    }


}
