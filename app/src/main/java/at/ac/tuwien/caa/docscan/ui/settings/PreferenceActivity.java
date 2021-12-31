package at.ac.tuwien.caa.docscan.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer;

/**
 * Created by fabian on 26.01.2018.
 */
public class PreferenceActivity extends BaseNavigationActivity {

    public static Intent newInstance(Context context) {
        return new Intent(context, PreferenceActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_framelayout, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            String preferenceKey = getResources().getString(R.string.key_debug_settings);
            findPreference(preferenceKey).setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(requireActivity(), DebugPreferenceActivity.class);
                startActivity(intent);
                return true;
            });

            String exifKey = getResources().getString(R.string.key_exif_settings);
            findPreference(exifKey).setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(requireActivity(), ExifPreferenceActivity.class);
                startActivity(intent);
                return true;
            });
        }
    }

    @NonNull
    @Override
    protected NavigationDrawer.NavigationItem getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItem.SETTINGS;
    }
}
