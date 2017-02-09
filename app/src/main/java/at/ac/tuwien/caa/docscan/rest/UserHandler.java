package at.ac.tuwien.caa.docscan.rest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by fabian on 09.02.2017.
 */
public class UserHandler {

    private static final String NAME_KEY = "userName";
    private static final String PASSWORD_KEY = "userPassword";

    public static void saveCredentials(Activity activity) {

        //        TODO: use here encryption
//        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        String n = User.getInstance().getUserName();
        editor.putString(NAME_KEY, User.getInstance().getUserName());
        editor.putString(PASSWORD_KEY, User.getInstance().getPassword());
        editor.apply();
        editor.commit();

    }

    public static boolean loadCredentials(Activity activity) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

//        SharedPreferences sharedPref = activity.getApplicationContext().getPreferences(Context.MODE_PRIVATE);
        String name = sharedPref.getString(NAME_KEY, null);
        String defaultPassword = "";
        String password = sharedPref.getString(PASSWORD_KEY, defaultPassword);

        if (name == null)
            return false;
        else {
            User.getInstance().setUserName(name);
            User.getInstance().setPassword(password);
            return true;
        }

    }
}
