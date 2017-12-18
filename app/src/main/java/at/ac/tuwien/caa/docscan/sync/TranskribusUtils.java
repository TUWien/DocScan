package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.User;

import static android.content.ContentValues.TAG;
import static at.ac.tuwien.caa.docscan.rest.RestRequest.BASE_URL;

/**
 * Created by fabian on 05.09.2017.
 */

public class TranskribusUtils  {

    public static final String TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "DocScan";

    // Singleton:
    private static TranskribusUtils mInstance;

    private Context mContext;
    private ArrayList<File> mSelectedDirs;
    private int mNumUploadJobs;
    private TranskribusUtilsCallback mCallback;

//    private int mUploadId;

    public static TranskribusUtils getInstance() {

        if (mInstance == null)
            mInstance = new TranskribusUtils();

        return mInstance;

    }

    private TranskribusUtils() {

    }


    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    public void startUpload(Context context, ArrayList<File> selectedDirs) {

        mContext = context;
        mSelectedDirs = selectedDirs;
        mCallback = (TranskribusUtilsCallback) context;

        new CollectionsRequest(mContext);

    }

    public void onCollections(List<Collection> collections) {

        for (Collection collection : collections) {
            if (collection.getName().compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
                docScanCollectionFound(collection);
                return;
            }
        }
        createDocScanCollection();

    }

    public void onError() {

        //

    }


    private void createDocScanCollection() {

        if (mContext != null)
            new CreateCollectionRequest(mContext, TRANSKRIBUS_UPLOAD_COLLECTION_NAME);

    }

//    @Override
    public void onCollectionCreated(String collName) {
        if (collName.compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0)
            new CollectionsRequest(mContext);

    }

    private void docScanCollectionFound(Collection collection) {

        uploadDirs(mSelectedDirs, collection.getID());

    }

    private void uploadDirs(ArrayList<File> dirs, int uploadId) {

        mSelectedDirs = dirs;
        mNumUploadJobs = 0;

        Log.d(TAG, "preparing file: " + dirs + " for upload");

        for (File dir : mSelectedDirs) {

            // Get the image files contained in the directory:
            File[] imgFiles = TranskribusUtils.getFiles(dir);
            if (imgFiles == null)
                return;
            else if (imgFiles.length == 0)
                return;

//            Create the JSON object for the directory:
            JSONObject jsonObject = TranskribusUtils.getJSONObject(dir.getName(), imgFiles);
//            Start the upload request:
            if (jsonObject != null) {
                new StartUploadRequest(mContext, jsonObject, uploadId);
            }
        }
    }

    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     * @param uploadId
     */
    public void onUploadStart(int uploadId, String title) {

        File selectedDir = getMatchingDir(title);

        if (selectedDir != null) {
            for (File file : TranskribusUtils.getFiles(selectedDir))
                SyncInfo.getInstance().addTranskribusFile(mContext, file, uploadId);
        }

        mNumUploadJobs++;

        // For each directory the upload request is finished and all files are added to the sync list:
        if (mNumUploadJobs == mSelectedDirs.size())
            mCallback.onFilesPrepared();
//            SyncInfo.startSyncJob(mContext);

//        TranskribusUtils.getInstance().setUploadId(uploadId);
//        SyncInfo.startSyncJob(this);

    }


    /**
     * Find the directory assigned to its title:
     * @param title
     * @return
     */
    @Nullable
    private File getMatchingDir(String title) {
        File selectedDir = null;

        // Find the
        for (File dir : mSelectedDirs) {
            if (dir.getName().compareTo(title) == 0) {
                selectedDir = dir;
                break;
            }
        }
        return selectedDir;
    }


    public void uploadFile(final SyncInfo.Callback callback, Context context, final SyncInfo.TranskribusFileSync fileSync) {

        final File file = fileSync.getFile();

        Ion.with(context)
                .load("PUT", BASE_URL + "uploads/" + Integer.toString(fileSync.getUploadId()))
                .setHeader("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
                .setMultipartContentType("multipart/form-data")
                .setMultipartFile("img", "application/octet-stream", file)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        if (e == null)
                            callback.onUploadComplete(fileSync);
                        else
                            callback.onError(e);

                    }
                });
    }


//    public void setUploadId(int uploadId) {
//        mUploadId = uploadId;
//    }
//
//    public int getUploadId() {
//        return mUploadId;
//    }

//    ================================ static methods start here: ================================

    public static JSONObject getJSONObject(String dirName, File[] imgFiles) {

//        File[] imgFiles = getFiles(dir);
        String metaData =   "\"md\": {" +
                            "\"title\": " + "\"" + dirName + "\"" +
                            "},";

        String jsonStart =  "{" +
                            metaData +
                            "    \"pageList\": {\"pages\": [";
        String jsonEnd =    "    ]}" +
                            "}";
        String jsonMiddle = "";

        int idx = 0;
        for (File file : imgFiles) {
            jsonMiddle += getFileString(file, idx+1); // Take care the JSON idx starts with 1.
            if (idx < imgFiles.length - 1)
                jsonMiddle += ", ";
            idx++;
        }

        JSONObject o = null;

        try {
            o = new JSONObject(jsonStart + jsonMiddle + jsonEnd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return o;

    }

    private static String getFileString(File file, int pageNr) {

        String result =
                            "        {" +
                                    "            \"fileName\": \"" +
                                    file.getName() +
                                    "\"," +
                                    "            \"pageNr\": " + Integer.toString(pageNr) +
                                    "        }";

        return result;
    }

    public static File[] getFiles(File dir) {

//TODO: filter JPGS only
        
        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };

        File[] files = dir.listFiles(filesFilter);

        return files;
    }


    public interface TranskribusUtilsCallback {

        void onFilesPrepared();

    }




//    // Singleton:
//    private static TranskribusUtils mInstance;
//
//    public static TranskribusUtils getInstance() {
//
//        if (mInstance == null)
//            mInstance = new TranskribusUtils();
//
//        return mInstance;
//
//    }
//
//    private TranskribusUtils() {
//
//    }
//
//    public void uploadFile(SyncInfo.Callback callback, SyncInfo.FileSync file) {
//        new UploadFileTask(callback, file).execute();
//    }
//
//    /**
//     * Async task to upload a file to a directory
//     * Taken from: @see <a href="https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/UploadFileTask.java"/>
//     */
//    private class UploadFileTask extends AsyncTask<Void, Void, FileMetadata> {
//
//        private final SyncInfo.Callback mCallback;
//        private Exception mException;
//        private SyncInfo.FileSync mFileSync;
//
//
//        UploadFileTask(SyncInfo.Callback callback, SyncInfo.FileSync fileSync) {
//            mCallback = callback;
//            mFileSync = fileSync;
//        }
//
//        @Override
//        protected void onPostExecute(FileMetadata result) {
//            super.onPostExecute(result);
//            if (mException != null) {
//                mCallback.onError(mException);
//            } else if (result == null) {
//                mCallback.onError(null);
//            } else {
//                mCallback.onUploadComplete(mFileSync);
//            }
//        }
//
//        @Override
//        protected FileMetadata doInBackground(Void... params) {
//
//
//            File localFile = mFileSync.getFile();
//
//            if (localFile != null) {
////                String remoteFolderPath = params[1];
//
//                // Note - this is not ensuring the name is a valid dropbox file name
//                String remoteFileName = localFile.getName();
//
//
////                try  {
////
////                    InputStream inputStream = new FileInputStream(localFile);
////
////                    return mClient.files().uploadBuilder("/" + remoteFileName)
////                            .withMode(WriteMode.OVERWRITE)
////                            .uploadAndFinish(inputStream);
////
//////                    return mClient.files().uploadBuilder(remoteFolderPath + "/" + remoteFileName)
//////                            .withMode(WriteMode.OVERWRITE)
//////                            .uploadAndFinish(inputStream);
////                } catch (DbxException | IOException e) {
////                    mException = e;
////                    Log.d("DropboxUtils", "exception: " + e);
////                }
//            }
//
//            return null;
//        }
//    }

}
