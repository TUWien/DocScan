package at.ac.tuwien.caa.docscan.sync;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public void connectToDropbox(DropboxConnectorCallback callback) {

        new DropboxConnector(callback).execute();

    }

    public void uploadFile(Callback callback, File file) {
        new UploadFileTask(callback).execute(file);

    }


    public interface DropboxConnectorCallback {
        void onDropboxConnected(boolean success);
    }

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    /**
     * Simply connects to the dropbox account. Note this must be done in an own thread cause
     * otherwise an android.os.NetworkOnMainThreadException exception is thrown.
     */
    private class DropboxConnector extends AsyncTask<Void, Void, Boolean> {

        private DropboxConnectorCallback mCallback;

        private DropboxConnector(DropboxConnectorCallback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            Log.d(this.getClass().getName(), "connecting to dropbox");

            DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
            mClient = new DbxClientV2(config, "QydvcYqUqSAAAAAAAAAACj6YMU2jyXUtUG5Rseh101maUnoOIDZQS8oH6BiP3uus");

            if (mClient == null)
                return false;

            // Get current account info
            FullAccount account = null;
            try {
                account = mClient.users().getCurrentAccount();
                Log.d(this.getClass().getName(), "dropbox account name: " + account.getName().getDisplayName());

                // Get files and folder metadata from Dropbox root directory
                ListFolderResult result = mClient.files().listFolder("");
                while (true) {
                    for (Metadata metadata : result.getEntries()) {
                        Log.d(this.getClass().getName(), "path: " + metadata.getPathLower());
                    }

                    if (!result.getHasMore()) {
                        break;
                    }

                    result = mClient.files().listFolderContinue(result.getCursor());
                }


            } catch (DbxException e) {
                e.printStackTrace();
            }

            return true;
        }


        protected void onPostExecute(Boolean result){
            mCallback.onDropboxConnected(result);
        }
    }

    /**
     * Async task to upload a file to a directory
     * Taken from: @see <a href="https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/UploadFileTask.java"/>
     */
    private class UploadFileTask extends AsyncTask<File, Void, FileMetadata> {

        private final Callback mCallback;
        private Exception mException;


        UploadFileTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(FileMetadata result) {
            super.onPostExecute(result);
            if (mException != null) {
                mCallback.onError(mException);
            } else if (result == null) {
                mCallback.onError(null);
            } else {
                mCallback.onUploadComplete(result);
            }
        }

        @Override
        protected FileMetadata doInBackground(File... params) {


            File localFile = params[0];

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
                }
            }

            return null;
        }
    }
}
