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

    private boolean mUseFastPageSegmentation;

    private Settings() {

        mUseFastPageSegmentation = true;

    }

    public static Settings getInstance() {

        if (mSettings == null)
            mSettings = new Settings();

        return mSettings;

    }

    public enum SettingEnum {

        HIDE_SERIES_DIALOG_KEY("HIDE_SERIES_DIALOG_KEY", false);

        private String mKey;
        private boolean mDefaultBooleanValue, mBooleanValue;
        private int mType;

        // Boolean value:
        SettingEnum(String key, boolean defaultValue) {
            mKey = key;
            mDefaultBooleanValue = defaultValue;
        }
    }


    public boolean loadKey(Activity activity, SettingEnum setting) {

        SharedPreferences sharedPref = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean value = sharedPref.getBoolean(setting.mKey, setting.mBooleanValue);
        return value;

    }

    public void saveKey(Activity activity, SettingEnum setting, boolean value) {

//        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(setting.mKey, value);
        editor.commit();

    }


//    private void saveBoolean(String key, )

    public boolean getUseFastPageSegmentation() {
        return mUseFastPageSegmentation;
    }

    public void setUseFastPageSegmentation(boolean fast) {
        mUseFastPageSegmentation = fast;
    }
}
