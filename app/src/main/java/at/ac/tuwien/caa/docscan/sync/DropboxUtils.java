package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Class used to access Dropbox. The Dropbox API key is not provided in repository and should never
 * be provided. Instead a not working dummy key is provided in gradle.properties. You can get the
 * API key if you send a mail to docscan@cvl.tuwien.ac.at. Before you replace the dummy key, assure
 * that you do not commit the key with the following command:
 * git update-index --assume-unchanged gradle.properties
 * Then you just have to replace the dummy key in gradle.properties.
 */

public class DropboxUtils {

    // Singleton:
    private static DropboxUtils mInstance;

    private DbxClientV2 mClient;
    private TranskribusUtils.TranskribusUtilsCallback mCallback;

    public static DropboxUtils getInstance() {

        if (mInstance == null)
            mInstance = new DropboxUtils();

        return mInstance;
    }

    private DropboxUtils() {

    }

    public void startUpload(Context context, TranskribusUtils.TranskribusUtilsCallback callback) {

        if (SyncInfo.getInstance().getUploadDirs() != null &&
                !SyncInfo.getInstance().getUploadDirs().isEmpty()) {

            uploadDirs(context, SyncInfo.getInstance().getUploadDirs(), callback);
        }

    }


    private void uploadDirs(Context context, ArrayList<File> dirs,
                            TranskribusUtils.TranskribusUtilsCallback callback) {

        for (File dir : dirs) {
            // Get the image files contained in the directory:
            File[] imgFiles = Helper.getImageArray(dir);
            for (File file : imgFiles) {
                SyncInfo.getInstance().addFile(context, file);
            }
        }

        callback.onFilesPrepared();

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
                String remoteFolderPath = localFile.getParentFile().getName();

                // Note - this is not ensuring the name is a valid dropbox file name
                String remoteFileName = localFile.getName();

                try  {

                    InputStream inputStream = new FileInputStream(localFile);

                    return mClient.files().uploadBuilder("/" + remoteFolderPath + "/" + remoteFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                } catch (DbxException | IOException e) {
                    mException = e;
                    Log.d("DropboxUtils", "exception: " + e);
                }
            }

            return null;
        }
    }
}
