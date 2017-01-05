package at.ac.tuwien.caa.docscan.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 20.12.2016.
 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {

        return NavigationDrawer.NavigationItemEnum.SETTINGS;

    }

    /**
     * The Fragment is added via the R.layout.settings_act layout xml.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            SettingsUtils.registerOnSharedPreferenceChangeListener(getActivity(), this);
        }

        @Override
        public RecyclerView onCreateRecyclerView(final LayoutInflater inflater,
                                                 final ViewGroup parent,
                                                 final Bundle savedInstanceState) {
            // Override the default list which has horizontal padding. Instead place padding on
            // the preference items for nicer touch feedback.
            final RecyclerView prefList = (RecyclerView) inflater.inflate(R.layout.settings_list, parent, false);
            prefList.setHasFixedSize(true);
            return prefList;

        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
//            SettingsUtils.unregisterOnSharedPreferenceChangeListener(getActivity(), this);
        }

//        @Override
//        public void onResume() {
//            super.onResume();
//
//            // configure the fragment's top clearance to take our overlaid controls (Action Bar
//            // and spinner box) into account.
//            int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
//            DrawShadowFrameLayout drawShadowFrameLayout =
//                    (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
//            if (drawShadowFrameLayout != null) {
//                drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
//            }
//            setContentTopClearance(actionBarSize);
//        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // The Calendar Sync requires checking the Calendar permission.
//            if (SettingsUtils.PREF_SYNC_CALENDAR.equals(key)) {
//                // Request permission when it doesn't exist, saving the information about whether
//                // the request enabled or disabled the sync via the requestCode.
//                if (!PermissionsUtils.permissionsAlreadyGranted(getActivity(), CALENDAR_PERMISSIONS)) {
//                    boolean shouldSync = SettingsUtils.shouldSyncCalendar(getActivity());
//                    int requestCode = shouldSync ? REQUEST_PERMISSION_REQUEST_CODE_ENABLE_CALENDAR :
//                            REQUEST_PERMISSION_REQUEST_CODE_DISABLE_CALENDAR;
//                    ActivityCompat.requestPermissions(getActivity(),
//                            CALENDAR_PERMISSIONS, requestCode);
//                    return;
//                }
//                scheduleCalendarSync(getActivity());
//            } else if (BuildConfig.PREF_CONF_MESSAGES_ENABLED.equals(key) ||
//                    BuildConfig.PREF_ATTENDEE_AT_VENUE.equals(key)) {
//                // This will activate re-registering with the correct GCM topic(s).
//                new MessagingRegistrationWithGCM(getActivity()).registerDevice();

            }
        }

//        private void setContentTopClearance(int clearance) {
//            if (getView() != null) {
//                getView().setPadding(getView().getPaddingLeft(), clearance,
//                        getView().getPaddingRight(), getView().getPaddingBottom());
//            }
//        }
}
