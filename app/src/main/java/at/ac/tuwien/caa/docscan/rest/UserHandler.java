package at.ac.tuwien.caa.docscan.rest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by fabian on 09.02.2017.
 */
public class UserHandler {

    private static final String NAME_KEY =          "userName";
    private static final String PASSWORD_KEY =      "userPassword";
    private static final String DROPBOX_TOKEN_KEY =  "dropboxToken";

    public static void saveDropboxToken(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(DROPBOX_TOKEN_KEY, User.getInstance().getDropboxToken());
        editor.apply();
        editor.commit();

    }

    public static boolean loadDropboxToken(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        String token = sharedPref.getString(DROPBOX_TOKEN_KEY, null);

        if (token == null)
            return false;
        else {
            User.getInstance().setDropboxToken(token);
            return true;
        }

    }

    public static void saveCredentials(Activity activity) {

        //        TODO: use here encryption
//        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(NAME_KEY, User.getInstance().getUserName());
        editor.putString(PASSWORD_KEY, User.getInstance().getPassword());
        editor.apply();
        editor.commit();

    }

    public static boolean loadCredentials(Activity activity) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

//        SharedPreferences sharedPref = activity.getApplicationContext().getPreferences(Context.MODE_PRIVATE);
        String name = sharedPref.getString(NAME_KEY, null);
        String defaultPassword = null;
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
