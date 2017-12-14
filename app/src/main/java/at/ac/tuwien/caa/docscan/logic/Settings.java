package at.ac.tuwien.caa.docscan.logic;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * A class used for loading and saving of settings that are accessed by multiple activities or need
 * to be stored in a static way (by making use of singleton).
 */
public class Settings {

    private static Settings mSettings = null;

    public static final int NO_ENTRY = -1;

    private boolean mUseFastPageSegmentation;
    private static final String SETTINGS_FILE_NAME = "settings";

    private Settings() {

        mUseFastPageSegmentation = true;

    }

    public static Settings getInstance() {

        if (mSettings == null)
            mSettings = new Settings();

        return mSettings;

    }

    public enum SettingEnum {

        DOCUMENT_HINT_SHOWN_KEY("DOCUMENT_HINT_SHOWN_KEY", false),
        INSTALLED_VERSION_KEY("INSTALLED_VERSION_KEY", NO_ENTRY),
        HIDE_SERIES_DIALOG_KEY("HIDE_SERIES_DIALOG_KEY", false),
        SERIES_MODE_ACTIVE_KEY("SERIES_MODE_ACTIVE_KEY", false),
        SERIES_MODE_PAUSED_KEY("SERIES_MODE_PAUSED_KEY", true);

        private String mKey;
        private boolean mDefaultBooleanValue;
        private int mType, mDefaultIntValue;

        // Boolean value:
        SettingEnum(String key, boolean defaultValue) {
            mKey = key;
            mDefaultBooleanValue = defaultValue;
        }

        // Int value:
        SettingEnum(String key, int defaultValue) {
            mKey = key;
            mDefaultIntValue = defaultValue;
        }
    }

    public boolean isServerChangedShown(Activity activity) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        boolean value = sharedPref.getBoolean("server_changed_shown_key", false);
        return value;
    }

    public void serverChangedShown(Activity activity) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("server_changed_shown_key", true);
        editor.commit();
    }

    public boolean loadBooleanKey(Activity activity, SettingEnum setting) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        boolean value = sharedPref.getBoolean(setting.mKey, setting.mDefaultBooleanValue);
        return value;

    }

    public int loadIntKey(Activity activity, SettingEnum setting) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        int value = sharedPref.getInt(setting.mKey, setting.mDefaultIntValue);
        return value;

    }

    public void saveKey(Activity activity, SettingEnum setting, boolean value) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(setting.mKey, value);
        editor.commit();

    }

    public void saveIntKey(Activity activity, SettingEnum setting, int value) {

        SharedPreferences sharedPref = getSharedPrefs(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(setting.mKey, value);
        editor.commit();

    }

    private SharedPreferences getSharedPrefs(Activity activity) {

        return activity.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);

    }


    public boolean getUseFastPageSegmentation() {
        return mUseFastPageSegmentation;
    }

    public void setUseFastPageSegmentation(boolean fast) {
        mUseFastPageSegmentation = fast;
    }
}
