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
//    public static final String TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "qr_code_test";
    private static final String CLASS_NAME = "TranskribusUtils";

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
        mCallback = (TranskribusUtilsCallback) context;

        SyncInfo.getInstance().clearSyncList();

        if (SyncInfo.getInstance().getUploadDirs() != null && !SyncInfo.getInstance().getUploadDirs().isEmpty()) {
            mAreDocumentsPrepared = false;
            //        Start the upload of user selected dirs:
            TranskribusUtils.getInstance().uploadDocuments(SyncInfo.getInstance().getUploadDirs());
        }
        else
            mAreDocumentsPrepared = true;

        if (SyncInfo.getInstance().getUnfinishedUploadIDs() != null &&
                !SyncInfo.getInstance().getUnfinishedUploadIDs().isEmpty()) {

            Log.d(CLASS_NAME, "startUpload: we have some unfinished work here");
            SyncInfo.getInstance().printUnfinishedUploadIDs();
            mAreUnfinishedFilesPrepared = false;
            startFindingUnfinishedUploads(SyncInfo.getInstance().getUnfinishedUploadIDs());
        }
        else {
            SyncInfo.getInstance().printUnfinishedUploadIDs();
            Log.d(CLASS_NAME, "startUpload: no unfinished work here");
            mAreUnfinishedFilesPrepared = true;
        }


    }

    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    public void uploadDocuments(ArrayList<File> selectedDirs) {

        mSelectedDirs = selectedDirs;
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

        Log.d(CLASS_NAME, "startFindingUnfinishedUploads1");

        mAreUnfinishedFilesPrepared = false;

//        We need here a deep copy, because we want to manipulate the member but not the
//        corresponding SyncInfo member:
        mUnfinishedUploadIDsProcessed = new ArrayList<>(unfinishedIDs);

        for (Integer id : unfinishedIDs)
            new UploadStatusRequest(mContext, id);

        Log.d(CLASS_NAME, "startFindingUnfinishedUploads2");

    }

    public void onUnfinishedUploadStatusReceived(Context context, int uploadID, ArrayList<String> unfinishedFileNames) {


        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived1: unfinished upload ids size" +
                SyncInfo.getInstance().getUnfinishedUploadIDs().size());

//        Add the files to the sync list:

        for (String fileName : unfinishedFileNames) {

//      We have to find here the file for the corresponding fileName. Unfortunately, we have to rely
//        here on the timestamp in the fileName to find correspondences...
            File file = Helper.getFile(context.getResources().getString(R.string.app_name), fileName);

            if (file != null) {
//                SyncInfo.getInstance().addToUnfinishedSyncList(file, uploadID);
                Log.d(getClass().getName(), "onUnfinishedUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
                SyncInfo.getInstance().addTranskribusFile(context, file, uploadID);
            }
            else {
                Log.d(getClass().getName(), "onUnfinishedUploadStatusReceived: file not existing: " + fileName);
                //            TODO: error handling here!
            }
        }


//        mUnfinishedUploadIDsProcessed.add(uploadID);

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived2: unfinished upload ids size" +
                SyncInfo.getInstance().getUnfinishedUploadIDs().size());

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        if (mUnfinishedUploadIDsProcessed.isEmpty()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived3: unfinished upload ids size" +
                SyncInfo.getInstance().getUnfinishedUploadIDs().size());

        SyncInfo.saveToDisk(mContext);

//        if (areUnfinishedFilesProcessed()) {
//            mAreUnfinishedFilesPrepared = true;
//            checkFilesPrepared();
//        }

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

        Log.d(CLASS_NAME, "saveUnfinishedUploadIDs: saving unprocessed ids...");

//        SyncInfo.getInstance().saveUnprocessedUploadIDs();

        Log.d(CLASS_NAME, "saveUnfinishedUploadIDs: unfinished saved:");

        SyncInfo.getInstance().printUnfinishedUploadIDs();

    }

    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     * @param uploadId
     */
    public void onNewUploadIDReceived(int uploadId, String title) {

        Log.d(CLASS_NAME, "onNewUploadIDReceived: id: " + uploadId + " title: " + title);

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

        SyncInfo.getInstance().getUnfinishedUploadIDs().add(uploadId);
        Log.d(CLASS_NAME, "added ID to SyncInfo.unfinishedUploadIDs: " + uploadId);

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



    /**
     * Checks if the unfinished files and the user selected documents are ready for upload.
     */
    private void checkFilesPrepared() {

        Log.d(CLASS_NAME, "checkFilesPrepared: mAreDocumentsPrepared: " + mAreDocumentsPrepared
                + " mAreUnfinishedFilesPrepared: " + mAreUnfinishedFilesPrepared);

        if (mAreDocumentsPrepared && mAreUnfinishedFilesPrepared)
            mCallback.onFilesPrepared();

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
//                            Log.d(getClass().getName(), "uploaded file: " + fileSync.toString());
                            callback.onUploadComplete(fileSync);
                            Log.d(CLASS_NAME, "uploaded file to collectionID: " +
                                    fileSync.getUploadId() + " file: " + fileSync.toString());
                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "uploaded file: " + fileSync.toString());

                            if (result.contains("<finished>")) {
//                                TODO: remove the document from the unfinished documents list.

//                                removeFromUnprocessedList(fileSync.getUploadId());
//                                removeFromUnfinishedListAndCheckJob(fileSync.getUploadId());
                                removeFromUnfinishedList(fileSync.getUploadId());

                                Log.d(getClass().getName(), "finished upload with ID: " + fileSync.getUploadId());
//                                Log.d(getClass().getName(), "response: " + result);
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

    public void onError() {

        Log.d(CLASS_NAME, "unfinished size: " + SyncInfo.getInstance().getUnfinishedUploadIDs().size());
        SyncInfo.saveToDisk(mContext);

    }

    public void removeFromUnfinishedListAndCheckJob(int uploadID) {

        removeFromUnfinishedList(uploadID);

        if (mUnfinishedUploadIDsProcessed.isEmpty()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

    }

    private void removeFromUnfinishedList(int uploadID) {

        Log.d(CLASS_NAME, "removeFromUnfinishedList: uploadID: " + uploadID);
        Log.d(CLASS_NAME, "removeFromUnfinishedList: list size: " +
                SyncInfo.getInstance().getUnfinishedUploadIDs().size());

        SyncInfo.getInstance().getUnfinishedUploadIDs().remove(new Integer(uploadID));

        if (mUnfinishedUploadIDsProcessed == null)
            return;

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        Log.d(CLASS_NAME, "removeFromUnfinishedList: list size: " +
                SyncInfo.getInstance().getUnfinishedUploadIDs().size());

        SyncInfo.saveToDisk(mContext);


    }


//    public void removeFromUnprocessedList(int uploadID) {
//
//        SyncInfo.getInstance().getUnprocessedUploadIDs().remove(new Integer(uploadID));
//
//    }


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

// example for the QR code values:
//        String metaData =   "\"md\": {" +
//                            "\"title\": " + "\"" + dirName + "\"," +
//                            "\"externalId\": "+ "\"" + "test_signature" + "\"," +
//                            "\"authority\": "+ "\"" + "test_authority" + "\"," +
//                            "\"hierarchy\": "+ "\"" + "test_hierarchy" + "\"" +
//                            "},";

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
