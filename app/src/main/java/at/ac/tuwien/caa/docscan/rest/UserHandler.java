package at.ac.tuwien.caa.docscan.rest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static at.ac.tuwien.caa.docscan.rest.User.SYNC_DROPBOX;

/**
 * Created by fabian on 09.02.2017.
 */
public class UserHandler {


    private static final String FIRST_NAME_KEY =    "firstName";
    private static final String LAST_NAME_KEY =     "lastName";
    private static final String NAME_KEY =          "userName";
    private static final String TRANSKRIBUS_PASSWORD_KEY =      "userPassword";
    private static final String DROPBOX_TOKEN_KEY = "dropboxToken";
    private static final String CONNECTION_KEY =    "connection";

    public static void clearUser(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(FIRST_NAME_KEY);
        editor.remove(LAST_NAME_KEY);
        editor.remove(NAME_KEY);
        editor.remove(TRANSKRIBUS_PASSWORD_KEY);
        editor.remove(DROPBOX_TOKEN_KEY);
        editor.remove(CONNECTION_KEY);
        editor.apply();
        editor.commit();

    }

    public static void saveDropboxToken(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(CONNECTION_KEY, SYNC_DROPBOX);
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

//    public static void loadSeriesName(Context context) {
//
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//        String seriesName = sharedPref.getString(
//                context.getResources().getString(R.string.series_name_key),
//                context.getResources().getString(R.string.series_name_default));
//
//        User.getInstance().setDocumentName(seriesName);
//
//    }
//
//    public static void saveSeriesName(Context context) {
//
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putString(
//                context.getResources().getString(R.string.series_name_key),
//                User.getInstance().getDocumentName());
//        editor.apply();
//        editor.commit();
//
//    }

    public static boolean loadUserNames(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);


        User.getInstance().setConnection(loadConnection(context));
        String firstName = sharedPref.getString(FIRST_NAME_KEY, null);
        String lastName = sharedPref.getString(LAST_NAME_KEY, null);

        if (lastName == null)
            return false;
        else {
            User.getInstance().setFirstName(firstName);
            User.getInstance().setLastName(lastName);
            return true;
        }

    }

    public static void saveUserName(Activity activity) {

        //        TODO: use here encryption
//        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(FIRST_NAME_KEY, User.getInstance().getFirstName());
        editor.putString(LAST_NAME_KEY, User.getInstance().getLastName());
//        editor.putInt(CONNECTION_KEY, User.getInstance().getConnection());
        editor.apply();
        editor.commit();

    }

    public static void saveTranskribusCredentials(Activity activity) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(NAME_KEY, User.getInstance().getUserName());
        editor.putString(TRANSKRIBUS_PASSWORD_KEY, User.getInstance().getPassword());
        editor.putInt(CONNECTION_KEY, User.getInstance().getConnection());
        editor.apply();
        editor.commit();

    }


    public static int loadConnection(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        int noValue = -1;
        int connection = sharedPref.getInt(CONNECTION_KEY, noValue);

        return connection;

    }

    public static boolean loadCredentials(Context context) {

        int connection = loadConnection(context);
        User.getInstance().setConnection(connection);

        boolean userLoaded = false;

        switch (connection) {

            case User.SYNC_DROPBOX:
                userLoaded = loadDropboxToken(context);
                break;

            case User.SYNC_TRANSKRIBUS:
                userLoaded = initTranskribusUser(context);
                break;

        }

        return userLoaded;

    }


    private static boolean initTranskribusUser(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);


        User.getInstance().setConnection(loadConnection(context));
//        SharedPreferences sharedPref = activity.getApplicationContext().getPreferences(Context.MODE_PRIVATE);
        String name = sharedPref.getString(NAME_KEY, null);
        String defaultPassword = null;
        String password = sharedPref.getString(TRANSKRIBUS_PASSWORD_KEY, defaultPassword);

        if (name == null)
            return false;
        else {
            User.getInstance().setUserName(name);
            User.getInstance().setPassword(password);
            return true;
        }

    }
}
