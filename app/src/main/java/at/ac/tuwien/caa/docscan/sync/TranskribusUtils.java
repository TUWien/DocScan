package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
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
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.CreateCollectionRequest;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.UploadStatusRequest;
import at.ac.tuwien.caa.docscan.rest.User;

import static android.content.ContentValues.TAG;

/**
 * A class responsible for handling functionality that is connected to Transkribus. Other (more
 * general) functionality should be included in SyncService.
 */

public class TranskribusUtils  {

    public static final String TRANSKRIBUS_UPLOAD_COLLECTION_NAME = "DocScan - Uploads";
    private static final String CLASS_NAME = "TranskribusUtils";

    // Singleton:
    private static TranskribusUtils mInstance;

    private Context mContext;
    private ArrayList<String> mSelectedDirs;
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

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startUpload");

        SyncStorage.getInstance().clearSyncList();

        if (SyncStorage.getInstance().getUploadDocumentTitles() != null &&
                !SyncStorage.getInstance().getUploadDocumentTitles().isEmpty()) {
            mAreDocumentsPrepared = false;
            //        Start the upload of user selected dirs:
            TranskribusUtils.getInstance().uploadDocuments(SyncStorage.getInstance().getUploadDocumentTitles());
        }
        else
            mAreDocumentsPrepared = true;

        if (SyncStorage.getInstance().getUnfinishedUploadIDs() != null &&
                !SyncStorage.getInstance().getUnfinishedUploadIDs().isEmpty()) {

            Log.d(CLASS_NAME, "startUpload: we have some unfinished work here");
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startUpload: we have some unfinished work here");
            SyncStorage.getInstance().printUnfinishedUploadIDs();
            mAreUnfinishedFilesPrepared = false;
            startFindingUnfinishedUploads(SyncStorage.getInstance().getUnfinishedUploadIDs());

        }
        else {

            SyncStorage.getInstance().printUnfinishedUploadIDs();
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startUpload: no unfinished work here");
            Log.d(CLASS_NAME, "startUpload: no unfinished work here");
            mAreUnfinishedFilesPrepared = true;

        }


    }


    /**
     * This method creates simply a CollectionsRequest in order to find the ID of the DocScan Transkribus
     * upload folder.
     */
    public void uploadDocuments(ArrayList<String> selectedDirs) {

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
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startFindingUnfinishedUploads1");

        mAreUnfinishedFilesPrepared = false;

//        We need here a deep copy, because we want to manipulate the member but not the
//        corresponding SyncStorage member:
        mUnfinishedUploadIDsProcessed = new ArrayList<>(unfinishedIDs);

        for (Integer id : unfinishedIDs)
            new UploadStatusRequest(mContext, id);

        Log.d(CLASS_NAME, "startFindingUnfinishedUploads2");
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startFindingUnfinishedUploads2");

    }

    public void onUnfinishedUploadStatusReceived(Context context, int uploadID, String title,
                                                 ArrayList<String> unfinishedFileNames) {


        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived1: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,"onUnfinishedUploadStatusReceived1: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());

        boolean isFileDeleted = false;
        File dir = Helper.getMediaStorageDir(context.getResources().getString(R.string.app_name));

        for (String fileName : unfinishedFileNames) {

            File file = new File(dir.getAbsolutePath(), fileName);

            if (file.exists()) {
                Log.d(getClass().getName(), "onUnfinishedUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
        //        Add the files to the sync list:
                SyncStorage.getInstance().addTranskribusFile(file, uploadID);
            }
            else {
                Log.d(getClass().getName(), "onUnfinishedUploadStatusReceived: file not existing: " + fileName);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived: file not existing: " + fileName);
//                First remove the upload ID from the unfinished list:
                removeFromSyncStorageUnfinishedList(uploadID);
                removeFromSyncStorageUploadList(title);
                isFileDeleted = true;
                break;
            }
        }

//        Tell the user that one or more files have been deleted:
        if (isFileDeleted) {
            mCallback.onFilesDeleted();
        }


//        mUnfinishedUploadIDsProcessed.add(uploadID);

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived2: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,"onUnfinishedUploadStatusReceived2: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        if (mUnfinishedUploadIDsProcessed.isEmpty()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived3: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived3: unfinished upload ids size" +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());

//        TODO: save here
//        SyncStorage.saveToDisk(mContext);

//        if (areUnfinishedFilesProcessed()) {
//            mAreUnfinishedFilesPrepared = true;
//            checkFilesPrepared();
//        }

    }

    /**
     * Determines if there is a DocScan collection ID saved in SharedPreferences.
     * @return
     */
    private boolean isCollectionIDSaved() {

        int savedCollectionID = getDocScanCollectionID();

        return (savedCollectionID != Settings.NO_ENTRY);

    }

    private int getDocScanCollectionID() {

        int savedCollectionID;

        //        Should we use the Transkribus test server?
        boolean useTestServer = Helper.useTranskribusTestServer(mContext);

//        Get the stored value, if one is existing otherwise Settings.NO_ENTRY is returned.
        if (useTestServer)
            savedCollectionID = Settings.getInstance().loadIntKey(mContext,
                    Settings.SettingEnum.TEST_COLLECTION_ID_KEY);
        else
            savedCollectionID = Settings.getInstance().loadIntKey(mContext,
                    Settings.SettingEnum.COLLECTION_ID_KEY);

        return savedCollectionID;

    }


    private void saveDocScanCollectionID(int collectionID) {

        boolean useTestServer = Helper.useTranskribusTestServer(mContext);

        if (useTestServer)
            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.TEST_COLLECTION_ID_KEY,
                    collectionID);
        else
            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY,
                    collectionID);



    }

    public void onCollections(List<Collection> collections) {

        int savedCollectionID = getDocScanCollectionID();

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
            saveDocScanCollectionID(maxId);
//            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY, maxId);
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

    private void uploadDirs(ArrayList<String> dirs, int collectionId) {

        mSelectedDirs = dirs;
        mNumUploadJobs = 0;

        Log.d(TAG, "preparing file: " + dirs + " for upload");
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,"preparing file: " + dirs + " for upload");

        for (String dir : mSelectedDirs) {

            // Get the image files contained in the directory:
            Document document = DocumentStorage.getInstance().getDocument(dir);

            if (document != null) {
                ArrayList<File> files = document.getFiles();
                if (files != null && !files.isEmpty()) {
                    File[] imgFiles = files.toArray(new File[files.size()]);
//            Create the JSON object for the directory:
                    JSONObject jsonObject = TranskribusUtils.getJSONObject(dir, imgFiles);
//            Start the upload request:
                    if (jsonObject != null) {
                        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", jsonObject.toString());
                        new StartUploadRequest(mContext, jsonObject, collectionId);
                    }
                }
            }
        }


    }


    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     * @param uploadId
     */
    public void onNewUploadIDReceived(int uploadId, String title) {

        Log.d(CLASS_NAME, "onNewUploadIDReceived: id: " + uploadId + " title: " + title);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,  + uploadId + " title: " + title);

        SyncStorage.getInstance().getUnprocessedUploadIDs().add(new Integer(uploadId));

//        File selectedDir = getMatchingDir(title);
        Document document = DocumentStorage.getInstance().getDocument(title);
        if (document != null) {
            ArrayList<File> files = document.getFiles();
            if (files != null && !files.isEmpty()) {
                File[] imageList = files.toArray(new File[files.size()]);
                for (File file : imageList)
                    SyncStorage.getInstance().addTranskribusFile(file, uploadId);
            }
        }

        mNumUploadJobs++;

        mSelectedDirs.remove(title);

        SyncStorage.getInstance().getUnfinishedUploadIDs().add(uploadId);
        Log.d(CLASS_NAME, "added ID to SyncStorage.unfinishedUploadIDs: " + uploadId);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "added ID to SyncStorage.unfinishedUploadIDs: " + uploadId);

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
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "checkFilesPrepared: mAreDocumentsPrepared: " + mAreDocumentsPrepared
                + " mAreUnfinishedFilesPrepared: " + mAreUnfinishedFilesPrepared);

        if (mAreDocumentsPrepared && mAreUnfinishedFilesPrepared)
            mCallback.onFilesPrepared();

    }



    public void uploadFile(final SyncStorage.Callback callback, Context context, final TranskribusSyncFile syncFile) {

        final File file = syncFile.getFile();

        Log.d(CLASS_NAME, "uploading file: " + syncFile.toString());

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "uploading file: " + syncFile.toString());

        Ion.with(context)
                .load("PUT", Helper.getTranskribusBaseUrl(mContext) + "uploads/" +
                        Integer.toString(syncFile.getUploadId()))
                .setHeader("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
                .setMultipartContentType("multipart/form-data")
                .setMultipartFile("img", "application/octet-stream", file)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {

                        if (e == null) {
//                            Log.d(getClass().getName(), "uploaded file: " + fileSync.toString());
                            callback.onUploadComplete(syncFile);
                            Log.d(CLASS_NAME, "uploaded file to collectionID: " +
                                    syncFile.getUploadId() + " file: " + syncFile.toString());
                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils",
                                    "uploaded file: " + syncFile.toString());

                            if (result.contains("<finished>")) {
                                removeFromSyncStorageUnfinishedList(syncFile.getUploadId());

                                Log.d(getClass().getName(), "finished upload with ID: " + syncFile.getUploadId());
//                                Log.d(getClass().getName(), "response: " + result);
                                DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "finished upload with ID: " + syncFile.getUploadId());
                            }

                        }
                        else {
                            callback.onError(e);
                            Log.d(getClass().getName(), "error uploading file with upload ID: " + syncFile.getUploadId() + " fileSync: " + syncFile.toString());
                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "error uploading file: " + e);
                        }

                    }
                });
    }


    public void onError() {

        User.getInstance().setLoggedIn(false);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onError");
        Log.d(CLASS_NAME, "unfinished size: " + SyncStorage.getInstance().getUnfinishedUploadIDs().size());
//        TODO: save here
//        SyncStorage.saveToDisk(mContext);

    }

    public void removeFromUnfinishedListAndCheckJob(int uploadID) {

        removeFromSyncStorageUnfinishedList(uploadID);

        if (mUnfinishedUploadIDsProcessed.isEmpty()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

    }

    /**
     * Removes every entry from the sync info uploaded list. This is necessary to show the document
     * as not uploaded. Otherwise, if the directory contains soley uploaded files, but also contains
     * missing files, it would be shown as uploaded.
     * @param title
     */
    private void removeFromSyncStorageUploadList(String title) {

        SyncStorage.getInstance().removeDocument(title);
//        TODO: save here:
//        SyncStorage.saveToDisk(mContext);

    }

    private void removeFromSyncStorageUnfinishedList(int uploadID) {

        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: uploadID: " + uploadID);
        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "removeFromSyncStorageUnfinishedList: uploadID: " + uploadID);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                        SyncStorage.getInstance().getUnfinishedUploadIDs().size());


        SyncStorage.getInstance().getUnfinishedUploadIDs().remove(new Integer(uploadID));

        if (mUnfinishedUploadIDsProcessed == null)
            return;

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,  "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance().getUnfinishedUploadIDs().size());


        for (Integer i : SyncStorage.getInstance().getUnfinishedUploadIDs()) {
            Log.d(CLASS_NAME, "still in list: " + i);
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "still in list: " + i);
        }

//TODO: save here:
//        SyncStorage.saveToDisk(mContext);


    }


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
        void onFilesDeleted();

    }


}
