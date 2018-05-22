package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.UploadStatusRequest;
import at.ac.tuwien.caa.docscan.rest.User;

import static android.content.ContentValues.TAG;
import static at.ac.tuwien.caa.docscan.rest.RestRequest.BASE_URL;

/**
 * A class responsible for handling functionality that is connected to Transkribus. Other (more
 * general) functionality should be included in SyncService.
 */

public class TranskribusUtils  {

//    public static final String TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "DocScan - Uploads";
    public static final String TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "upload_continue_test_2";

    // Singleton:
    private static TranskribusUtils mInstance;

    private Context mContext;
    private ArrayList<File> mSelectedDirs;
    private int mNumUploadJobs;
    private TranskribusUtilsCallback mCallback;
    private boolean mIsCollectionCreated;
    private ArrayList<Integer> mUnfinishedUploadIDsProcessed;
    private boolean mAreUnfinishedFilesPrepared, mAreDocumentsPrepared;

//    private int mUploadId;

    public static TranskribusUtils getInstance() {

        if (mInstance == null)
            mInstance = new TranskribusUtils();

        return mInstance;

    }

    private TranskribusUtils() {

    }

    public void startUpload(Context context) {

        mContext = context;

        if (SyncInfo.getInstance().getUploadDirs() != null && !SyncInfo.getInstance().getUploadDirs().isEmpty()) {
            mAreDocumentsPrepared = false;
            //        Start the upload of user selected dirs:
            TranskribusUtils.getInstance().uploadDocuments(mContext, SyncInfo.getInstance().getUploadDirs());
        }
        else
            mAreDocumentsPrepared = true;

        if (SyncInfo.getInstance().getUnfinishedUploadIDs() != null &&
                !SyncInfo.getInstance().getUnfinishedUploadIDs().isEmpty()) {
            mAreUnfinishedFilesPrepared = false;
            startFindingUnfinishedUploads(SyncInfo.getInstance().getUnfinishedUploadIDs());
        }
        else
            mAreUnfinishedFilesPrepared = true;


    }

    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    public void uploadDocuments(Context context, ArrayList<File> selectedDirs) {


        mContext = context;
        mSelectedDirs = selectedDirs;
        mCallback = (TranskribusUtilsCallback) context;

        mIsCollectionCreated = false;

        if (isCollectionIDSaved())
            new CollectionsRequest(mContext);
        else
            createDocScanCollection();

//        new CollectionsRequest(mContext);

    }

    /**
     * Sends requests to the server asking in order to collect the unfinished files.
     * @param unfinishedIDs
     */
    public void startFindingUnfinishedUploads(ArrayList<Integer> unfinishedIDs) {

        mAreUnfinishedFilesPrepared = false;
        mUnfinishedUploadIDsProcessed = new ArrayList<>();

        for (Integer id : unfinishedIDs)
            new UploadStatusRequest(mContext, id);

    }

    private boolean isCollectionIDSaved() {

        int savedCollectionID =
                Settings.getInstance().loadIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY);

        return (savedCollectionID != Settings.NO_ENTRY);

    }

    public void onCollections(List<Collection> collections) {

        int savedCollectionID =
                Settings.getInstance().loadIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY);

        int maxId = -1;

        for (Collection collection : collections) {
            if (collection.getName().compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
//                docScanCollectionFound(collection);
//                return;

//                Is the collection id not saved yet?
                if ((savedCollectionID == Settings.NO_ENTRY) || mIsCollectionCreated) {
                    int id = collection.getID();
                    if (id > maxId)
                        maxId = id;
//                    mIsCollectionCreated = false;
//                    Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY, collection.getID());
//                    docScanCollectionFound(collection);
//                    return;
                }
                else if ((savedCollectionID == collection.getID())) {
                    docScanCollectionFound(collection.getID());
                    return;
                }

            }
        }

        if (maxId > -1) {

            mIsCollectionCreated = false;
            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY, maxId);
            docScanCollectionFound(maxId);
            return;

        }

        createDocScanCollection();

    }


    private void createDocScanCollection() {

        if (mContext != null)
            new CreateCollectionRequest(mContext, TRANSKRIBUS_UPLOAD_COLLECTION_NAME);

    }

    //    @Override
    public void onCollectionCreated(String collName) {
        if (collName.compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
            mIsCollectionCreated = true;
            new CollectionsRequest(mContext);
        }

    }


    private void docScanCollectionFound(int id) {

        uploadDirs(mSelectedDirs, id);

    }

    private void uploadDirs(ArrayList<File> dirs, int uploadId) {

        mSelectedDirs = dirs;
        mNumUploadJobs = 0;

        Log.d(TAG, "preparing file: " + dirs + " for upload");

        for (File dir : mSelectedDirs) {

            // Get the image files contained in the directory:
            File[] imgFiles = Helper.getImageArray(dir);
            if (imgFiles == null)
                return;
            else if (imgFiles.length == 0)
                return;

//            Create the JSON object for the directory:
            JSONObject jsonObject = TranskribusUtils.getJSONObject(dir.getName(), imgFiles);
//            Start the upload request:
            if (jsonObject != null) {
                DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", jsonObject.toString());
                new StartUploadRequest(mContext, jsonObject, uploadId);
            }
        }
    }

    public void saveUnfinishedUploadIDs(){

        SyncInfo.getInstance().saveUnprocessedUploadIDs();


    }

    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     * @param uploadId
     */
    public void onUploadIDReceived(int uploadId, String title) {

        SyncInfo.getInstance().getUnprocessedUploadIDs().add(new Integer(uploadId));

        File selectedDir = getMatchingDir(title);

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "onuploadStart - info:");
        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "id: " + uploadId);
        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "title: " + title);

        if (selectedDir != null) {
            File[] imageList = Helper.getImageArray(selectedDir);
            for (File file : imageList)
                SyncInfo.getInstance().addTranskribusFile(mContext, file, uploadId);
        }

        mNumUploadJobs++;

        Iterator<File> iter = mSelectedDirs.iterator();
        while (iter.hasNext()) {
            if (iter.next() == selectedDir)
                iter.remove();
        }

//        // For each directory the upload request is finished and all files are added to the sync list:
//        if (mNumUploadJobs == mSelectedDirs.size())
//            mCallback.onSelectedFilesPrepared();

        // For each directory the upload request is finished and all files are added to the sync list:
        if (mSelectedDirs.size() == 0) {
            mAreDocumentsPrepared = true;
            checkFilesPrepared();
//            mCallback.onSelectedFilesPrepared();
        }

    }

    public void onUploadStatusReceived(Context context, int uploadID, ArrayList<String> unfinishedFileNames) {

        Log.d(getClass().getName(), "onUploadStatusReceived");

        for (String fileName : unfinishedFileNames) {

//      We have to find here the file for the corresponding fileName. Unfortunately, we have to rely
//        here on the timestamp in the fileName to find correspondences...
            File file = Helper.getFile(context.getResources().getString(R.string.app_name), fileName);

            if (file != null) {
//                SyncInfo.getInstance().addToUnfinishedSyncList(file, uploadID);
                Log.d(getClass().getName(), "onUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
                SyncInfo.getInstance().addTranskribusFile(context, file, uploadID);
            }
            else {
                Log.d(getClass().getName(), "onUploadStatusReceived: file not existing: " + fileName);
                //            TODO: error handling here!
            }
        }

        mUnfinishedUploadIDsProcessed.add(uploadID);

        if (areUnfinishedFilesProcessed()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

    }

    /**
     * Checks if the unfinished files and the user selected documents are ready for upload.
     */
    private void checkFilesPrepared() {

        if (mAreDocumentsPrepared && mAreUnfinishedFilesPrepared)
            mCallback.onFilesPrepared();
    }


    private boolean areUnfinishedFilesProcessed() {

        //        The unfinished files are now ready for upload:
        if (mUnfinishedUploadIDsProcessed.size() ==
                SyncInfo.getInstance().getUnfinishedUploadIDs().size()) {

            for (int i : SyncInfo.getInstance().getUnfinishedUploadIDs()) {
                if (!mUnfinishedUploadIDsProcessed.contains(i))
                    return false;

            }

            return true;
        }

        return false;

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

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "uploading file: " + fileSync.toString());

        Ion.with(context)
                .load("PUT", BASE_URL + "uploads/" + Integer.toString(fileSync.getUploadId()))
                .setHeader("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
                .setMultipartContentType("multipart/form-data")
                .setMultipartFile("img", "application/octet-stream", file)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        if (e == null) {
                            Log.d(getClass().getName(), "uploaded file: " + fileSync.toString());
                            callback.onUploadComplete(fileSync);
                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "uploaded file: " + fileSync.toString());

                            if (result.contains("<finished>")) {
//                                TODO: remove the document from the unfinished documents list.

                                removeFromUnprocessedList(fileSync.getUploadId());
                                Log.d(getClass().getName(), "finished upload with ID: " + fileSync.getUploadId());
                                DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "finished upload with ID: " + fileSync.getUploadId());
                            }

                        }
                        else {
                            callback.onError(e);
                            Log.d(getClass().getName(), "error uploading file with upload ID: " + fileSync.getUploadId() + " fileSync: " + fileSync.toString());
                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "error uploading file: " + e);
                        }

                    }
                });
    }


    private void removeFromUnprocessedList(int uploadID) {

        SyncInfo.getInstance().getUnprocessedUploadIDs().remove(new Integer(uploadID));

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

    public interface TranskribusUtilsCallback {

        void onFilesPrepared();
//        void onSelectedFilesPrepared();
//        void onUnfinishedFilesPrepared();

    }


}
