package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.ui.LoginActivity;

import static android.content.Context.MODE_PRIVATE;
import static at.ac.tuwien.caa.docscan.ui.LoginActivity.PARENT_ACTIVITY_NAME;

/**
 * Created by fabian on 18.08.2017.
 */

public class DropboxUtils {

    // Singleton:
    private static DropboxUtils mInstance;

    private DbxClientV2 mClient;

    public static DropboxUtils getInstance() {

        if (mInstance == null)
            mInstance = new DropboxUtils();

        return mInstance;
    }

    private DropboxUtils() {

    }

    public void connectToDropbox(DropboxConnectorCallback callback, String token) {

        new DropboxConnector(callback).execute(token);

    }

    public void loginToDropbox(LoginRequest.LoginCallback callback, String token) {

        new DropboxLogin(callback).execute(token);

    }

    public void uploadFile(SyncInfo.Callback callback, SyncInfo.FileSync file) {
        new UploadFileTask(callback, file).execute();

    }

    public boolean startAuthentication(Context context) {

//        TODO: handle cases in which the user rejects the authentication

        try {
            Auth.startOAuth2Authentication(context, BuildConfig.DropboxApiKey);
        }
        catch (Exception e) {
//                This happens if a wrong api key is provided
            return false;
        }

        return true;

    }

    public void authenticate(DropboxConnectorCallback callback, Context context) {


//        boolean isDropboxLoaded = UserHandler.loadDropboxToken(context);
//
//        String accessToken;
//        if (!isDropboxLoaded) {
//            // Retrieve the authorization token from the Dropbox developers website:
//            Auth.startOAuth2Authentication(context, context.getString(R.string.sync_dropbox_app_key));
//            accessToken = Auth.getOAuth2Token();
//            if (accessToken != null) {
//                UserHandler.saveDropboxToken(context);
//            }
//        } else {
//            accessToken = User.getInstance().getDropboxToken();
//        }


        SharedPreferences prefs = context.getSharedPreferences("dropbox-access", MODE_PRIVATE);
//        String accessToken = prefs.getString("access-tokenx", null);
//
//        if (accessToken == null) {
            // Retrieve the authorization token from the Dropbox developers website:

//            Auth.startOAuth2Authentication(context, context.getString(R.string.sync_dropbox_app_key));

            String accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString("access-token", accessToken).apply();
//                initAndLoadData(accessToken);
            }
//        } else {
////            initAndLoadData(accessToken);
//        }



        if (accessToken == null) {
            Toast.makeText(context, R.string.sync_no_dropbox_access, Toast.LENGTH_LONG);
            return;
        }

        // TODO: not sure for what the UID is needed:
//        String uid = Auth.getUid();
//        String storedUid = prefs.getString("user-id", null);
//        if (uid != null && !uid.equals(storedUid)) {
//            prefs.edit().putString("user-id", uid).apply();
//        }

//        DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("DocScan")
//                .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
//                .build();

        // This must be called in an own thread:
        new DropboxConnector(callback).execute(accessToken);

//        new DropboxConnector(callback).execute(accessToken);
//        mClient = new DbxClientV2(requestConfig, accessToken);

//        FullAccount account = null;
//        try {
//            account = mClient.users().getCurrentAccount();
//            Log.d(this.getClass().getName(), "dropbox account name: " + account.getName().getDisplayName());
//
//            // Get files and folder metadata from Dropbox root directory
//            ListFolderResult result = mClient.files().listFolder("");
//            while (true) {
//                for (Metadata metadata : result.getEntries()) {
//                    Log.d(this.getClass().getName(), "path: " + metadata.getPathLower());
//                }
//
//                if (!result.getHasMore()) {
//                    break;
//                }
//
//                result = mClient.files().listFolderContinue(result.getCursor());
//            }
//
//
//        } catch (DbxException e) {
//            e.printStackTrace();
//        }
//
//
//        Log.d(this.getClass().getName(), "token for dropbox: " + accessToken);



//        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
//
//        DbxRequestConfig config = new DbxRequestConfig(
//                "JavaTutorial/1.0", Locale.getDefault().toString());
//        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
//
//        String authorizeUrl = webAuth.start();
//
//        Uri uri = Uri.parse(authorizeUrl);
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        context.startActivity(intent);



    }


    public interface DropboxConnectorCallback {
        void onDropboxConnected(User user);
    }


    /**
     * Simply connects to the dropbox account. Note this must be done in an own thread cause
     * otherwise an android.os.NetworkOnMainThreadException exception is thrown.
     */
    private class DropboxLogin extends AsyncTask<String, Void, Boolean> {

        private LoginRequest.LoginCallback mCallback;

        private DropboxLogin(LoginRequest.LoginCallback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            Log.d(this.getClass().getName(), "connecting to dropbox");

            DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
            mClient = new DbxClientV2(config, params[0]);

            if (mClient == null)
                return false;

            // Get current account info
            FullAccount account = null;
            try {
                account = mClient.users().getCurrentAccount();
                Log.d(this.getClass().getName(), "dropbox account name: " + account.getName().getDisplayName());

                User.getInstance().setLoggedIn(true);
                User.getInstance().setFirstName(account.getName().getGivenName());
                User.getInstance().setLastName(account.getName().getSurname());
                User.getInstance().setConnection(User.SYNC_DROPBOX);

            } catch (DbxException e) {
                e.printStackTrace();
            }

            return true;
        }


        protected void onPostExecute(Boolean isConnected){
            if (isConnected)
                mCallback.onLogin(User.getInstance());
            else
                mCallback.onLoginError();
        }
    }

    /**
     * Simply connects to the dropbox account. Note this must be done in an own thread cause
     * otherwise an android.os.NetworkOnMainThreadException exception is thrown.
     */
    private class DropboxConnector extends AsyncTask<String, Void, Boolean> {

        private DropboxConnectorCallback mCallback;

        private DropboxConnector(DropboxConnectorCallback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            Log.d(this.getClass().getName(), "connecting to dropbox");

            DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
            mClient = new DbxClientV2(config, params[0]);

            if (mClient == null)
                return false;

            // Get current account info
            FullAccount account = null;
            try {
                account = mClient.users().getCurrentAccount();
                Log.d(this.getClass().getName(), "dropbox account name: " + account.getName().getDisplayName());

                User.getInstance().setLoggedIn(true);
//                User.getInstance().setSessionID(id);
                User.getInstance().setFirstName(account.getName().getGivenName());
                User.getInstance().setLastName(account.getName().getSurname());
////            Now update the GUI with the user data:
                mCallback.onDropboxConnected(User.getInstance());
//                ((LoginCallback) mRestCallback).onLogin(User.getInstance());
//
//                // Get files and folder metadata from Dropbox root directory
//                ListFolderResult result = mClient.files().listFolder("");
//                while (true) {
//                    for (Metadata metadata : result.getEntries()) {
//                        Log.d(this.getClass().getName(), "path: " + metadata.getPathLower());
//                    }
//
//                    if (!result.getHasMore()) {
//                        break;
//                    }
//
//                    result = mClient.files().listFolderContinue(result.getCursor());
//                }


            } catch (DbxException e) {
                e.printStackTrace();
            }

            return true;
        }


        protected void onPostExecute(Boolean isConnected){
            if (isConnected)
                mCallback.onDropboxConnected(User.getInstance());
//TODO: error handling
        }
    }

    /**
     * Async task to upload a file to a directory
     * Taken from: @see <a href="https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/UploadFileTask.java"/>
     */
    private class UploadFileTask extends AsyncTask<Void, Void, FileMetadata> {

        private final SyncInfo.Callback mCallback;
        private Exception mException;
        private SyncInfo.FileSync mFileSync;


        UploadFileTask(SyncInfo.Callback callback, SyncInfo.FileSync fileSync) {
            mCallback = callback;
            mFileSync = fileSync;
        }

        @Override
        protected void onPostExecute(FileMetadata result) {
            super.onPostExecute(result);
            if (mException != null) {
                mCallback.onError(mException);
            } else if (result == null) {
                mCallback.onError(null);
            } else {
                mCallback.onUploadComplete(mFileSync);
            }
        }

        @Override
        protected FileMetadata doInBackground(Void... params) {


            File localFile = mFileSync.getFile();

            if (localFile != null) {
//                String remoteFolderPath = params[1];

                // Note - this is not ensuring the name is a valid dropbox file name
                String remoteFileName = localFile.getName();

                try  {

                    InputStream inputStream = new FileInputStream(localFile);

                    return mClient.files().uploadBuilder("/" + remoteFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);

//                    return mClient.files().uploadBuilder(remoteFolderPath + "/" + remoteFileName)
//                            .withMode(WriteMode.OVERWRITE)
//                            .uploadAndFinish(inputStream);
                } catch (DbxException | IOException e) {
                    mException = e;
                    Log.d("DropboxUtils", "exception: " + e);
                }
            }

            return null;
        }
    }
}
