package at.ac.tuwien.caa.docscan.sync;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by fabian on 05.09.2017.
 */

public class TranskribusUtils {

    // Singleton:
    private static TranskribusUtils mInstance;

    public static TranskribusUtils getInstance() {

        if (mInstance == null)
            mInstance = new TranskribusUtils();

        return mInstance;

    }

    private TranskribusUtils() {

    }

    public void uploadFile(SyncInfo.Callback callback, SyncInfo.FileSync file) {
        new UploadFileTask(callback, file).execute();
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


//                try  {
//
//                    InputStream inputStream = new FileInputStream(localFile);
//
//                    return mClient.files().uploadBuilder("/" + remoteFileName)
//                            .withMode(WriteMode.OVERWRITE)
//                            .uploadAndFinish(inputStream);
//
////                    return mClient.files().uploadBuilder(remoteFolderPath + "/" + remoteFileName)
////                            .withMode(WriteMode.OVERWRITE)
////                            .uploadAndFinish(inputStream);
//                } catch (DbxException | IOException e) {
//                    mException = e;
//                    Log.d("DropboxUtils", "exception: " + e);
//                }
            }

            return null;
        }
    }

}
