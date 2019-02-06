package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.ui.AccountActivity;

/**
 * Class used to access Dropbox. The Dropbox API key is not provided in repository and should never
 * be provided. Instead a not working dummy key is provided in gradle.properties. You can get the
 * API key if you send a mail to docscan@cvl.tuwien.ac.at. Before you replace the dummy key, assure
 * that you do not commit the key with the following command:
 * git update-index --assume-unchanged gradle.properties
 * Then you just have to replace the dummy key in gradle.properties.
 */

public class DropboxUtils {

    private static final String CLASS_NAME = "DropboxUtils";

    // Singleton:
    private static DropboxUtils mInstance;

    private Context mContext;
    private DbxClientV2 mClient;
    private TranskribusUtils.TranskribusUtilsCallback mCallback;

    public static DropboxUtils getInstance() {

        if (mInstance == null)
            mInstance = new DropboxUtils();

        return mInstance;
    }

    private DropboxUtils() {

    }

    public void startUpload(Context context) {

        mCallback = (TranskribusUtils.TranskribusUtilsCallback) context;
        mContext = context;

        SyncStorage.getInstance(context).clearSyncList();

        ArrayList<String> titles = SyncStorage.getInstance(context).getUploadDocumentTitles();

        if (titles != null && !titles.isEmpty()) {
            for (String title : titles) {
                if (title != null)
                    addDocument(title);
            }
        }



    }

    private void addDocument(String documentTitle) {

        Document document = DocumentStorage.getInstance(mContext).getDocument(documentTitle);
        if (document != null) {
//            ArrayList<File> files = document.getFiles();
//            if (files != null && !files.isEmpty()) {
//                File[] imageList = files.toArray(new File[files.size()]);
//                for (File file : imageList)
//                    SyncStorage.getInstance().addDropboxFile(file, documentTitle);
//            }
            new CleanFilesTask(document).execute();

        }

    }


    public void loginToDropbox(LoginRequest.LoginCallback callback, String token) {

        new DropboxLogin(callback).execute(token);

    }

    public void uploadFile(final SyncStorage.Callback callback, DropboxSyncFile file) {
        new UploadFileTask(callback, file).execute();

    }

    /**
     * Clears the token, which is similar to logging a user out. See here:
     * https://github.com/dropbox/dropbox-sdk-java/issues/92
     * @param callback
     */
    public void clearAccessToken(DropboxCallback callback) {

        new ClearAccessToken(callback).execute();

    }

    public boolean startAuthentication(Context context) {

//        TODO: handle cases in which the user rejects the authentication

        try {
            Auth.startOAuth2Authentication(context, BuildConfig.DropboxApiKey);
        }
        catch (Exception e) {
            Crashlytics.logException(e);
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

            Log.d(CLASS_NAME, "DropboxLogin: doInBackground");
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "login...");

            DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
            mClient = new DbxClientV2(config, params[0]);

            if (mClient == null)
                return false;

            // Get current account info
            try {
                FullAccount account = mClient.users().getCurrentAccount();

                User.getInstance().setLoggedIn(true);
                User.getInstance().setFirstName(account.getName().getGivenName());
                User.getInstance().setLastName(account.getName().getSurname());
                User.getInstance().setConnection(User.SYNC_DROPBOX);
                User.getInstance().setPhotoUrl(account.getProfilePhotoUrl());

                Log.d(CLASS_NAME, "DropboxLogin: doInBackground: login success");
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,
                        "DropboxLogin: doInBackground: login success");

                return true;

            } catch (DbxException e) {
                Crashlytics.logException(e);
                Log.d(CLASS_NAME, "DropboxLogin: doInBackground: login error: " + e);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,
                        "DropboxLogin: doInBackground: login error: " + e);
                e.printStackTrace();
                return false;
            }


        }


        protected void onPostExecute(Boolean isConnected){
            if (isConnected)
                mCallback.onLogin(User.getInstance());
            else
                mCallback.onLoginError();
        }
    }

    private class CleanFilesTask extends AsyncTask<Void, Void, Void> {

        private Document mDocument;

        CleanFilesTask(Document document) {
            mDocument = document;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                ArrayList<String> remoteFileNames = processRemoteFiles();
                processLocalFiles(remoteFileNames);
            } catch (DbxException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            return null;

        }

        private void processLocalFiles(ArrayList<String> remoteFileNames) {
            if (mDocument.getFiles() != null) {
                for (File file : mDocument.getFiles()) {
//                        The file is not on the server:
                    if (!remoteFileNames.contains(file.getName()))
                        SyncStorage.getInstance(mContext).addDropboxFile(file, mDocument.getTitle());
                }
            }
        }

        private ArrayList<String> processRemoteFiles() throws DbxException {

            ArrayList<String> remoteFileNames = new ArrayList<>();


            ListFolderResult list;
//            I did not found another way to check the presence of a folder:
            try {
                list = mClient.files().listFolder("/" + mDocument.getTitle());
            }
            catch(ListFolderErrorException e) {
                Crashlytics.logException(e);
                return remoteFileNames;
            }

            for (int i = 0; i < list.getEntries().size(); i++) {
                Metadata metadata = list.getEntries().get(i);

                String remoteFileName = metadata.getName();
                remoteFileNames.add(remoteFileName);

//                    Delete the uploaded file if it is not contained in the document
                if (!isInDocument(remoteFileName))
                    mClient.files().delete("/" + mDocument.getTitle() + "/" + remoteFileName);
                else {
                    int fileIdx = mDocument.getFileNames().indexOf(remoteFileName);
                    File file = mDocument.getFiles().get(fileIdx);
                    if (SyncStorage.getInstance(mContext).isFileChanged(file) ||
                            !SyncStorage.getInstance(mContext).isInUploadedList(file))
                        SyncStorage.getInstance(mContext).addDropboxFile(file, mDocument.getTitle());

                }
            }

            return remoteFileNames;

        }


        private String hex(byte[] data) {

            char[] HEX_DIGITS = new char[]{
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f'};

            char[] buf = new char[2*data.length];
            int i = 0;
            for (byte b : data) {
                buf[i++] = HEX_DIGITS[(b & 0xf0) >>> 4];
                buf[i++] = HEX_DIGITS[b & 0x0f];
            }
            return new String(buf);

        }

        protected void onPostExecute(Void v){

            mCallback.onFilesPrepared();

        }

        private boolean isInDocument(String fileName) {

            if (mDocument.getPages() == null || mDocument.getPages().isEmpty())
                return false;

            return mDocument.getFileNames().contains(fileName);

        }

        private byte[] computeHash(String fileName) throws IOException {

            MessageDigest hasher = new DropboxContentHasher();
            byte[] buf = new byte[1024];
            InputStream in = new FileInputStream(fileName);
            try {
                while (true) {
                    int n = in.read(buf);
                    if (n < 0) break;  // EOF
                    hasher.update(buf, 0, n);
                }
            }
            finally {
                in.close();
            }

            return hasher.digest();

        }
    }

    private class ClearAccessToken extends AsyncTask<Void, Void, Boolean> {


        DropboxCallback mCallback;

        ClearAccessToken(DropboxCallback callback) {
            mCallback = callback;
        }

        /**
         * Returns true if the token is cleared:
         * @param voids
         * @return
         */
        @Override
        protected Boolean doInBackground(Void... voids) {

            try {

                AuthActivity.result = null;

                if (mClient != null)
                    mClient.auth().tokenRevoke();
                return true;

            } catch (DbxException e) {
                Crashlytics.logException(e);
                return true;
            }

        }

        @Override
        protected void onPostExecute(Boolean isCleared) {

            mCallback.onAccesTokenCleared(isCleared);

        }

    }

    /**
     * Async task to upload a file to a directory
     * Taken from: @see <a href="https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/UploadFileTask.java"/>
     */
    private class UploadFileTask extends AsyncTask<Void, Void, FileMetadata> {

        private final SyncStorage.Callback mCallback;
        private Exception mException;
        private DropboxSyncFile mSyncFile;


        UploadFileTask(SyncStorage.Callback callback, DropboxSyncFile syncFile) {
            mCallback = callback;
            this.mSyncFile = syncFile;
        }

        @Override
        protected void onPostExecute(FileMetadata result) {
            super.onPostExecute(result);
            if (mException != null) {
                Log.d(CLASS_NAME, "UploadFileTask: exception: " + mException + mSyncFile);
                mCallback.onError(mException);
            } else if (result == null) {
                Log.d(CLASS_NAME, "UploadFileTask: result null: " + mSyncFile);
                mCallback.onError(null);
            } else {
                Log.d(CLASS_NAME, "UploadFileTask: success: " + mSyncFile);
                mCallback.onUploadComplete(mSyncFile);
            }
        }

        @Override
        protected FileMetadata doInBackground(Void... params) {


            File localFile = mSyncFile.getFile();

            if (localFile != null) {
//                String remoteFolderPath = params[1];
                String remoteFolderPath = mSyncFile.getDocumentName();
                // Note - this is not ensuring the name is a valid dropbox file name
                String remoteFileName = localFile.getName();

                try {

                    InputStream inputStream = new FileInputStream(localFile);

                    return mClient.files().uploadBuilder("/" + remoteFolderPath + "/" + remoteFileName)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                } catch (DbxException | IOException e) {
                    Crashlytics.logException(e);
                    mException = e;
                    Log.d(CLASS_NAME, "UploadFileTask: doInBackground: exception: " + mException);
                }
            }

            return null;
        }
    }

    public interface DropboxCallback {
        void onAccesTokenCleared(boolean isCleared);
    }
}
