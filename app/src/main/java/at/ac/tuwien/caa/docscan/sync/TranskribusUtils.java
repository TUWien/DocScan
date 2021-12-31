package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
//import com.koushikdutta.async.future.FutureCallback;
//import com.koushikdutta.ion.Ion;

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
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.logic.DocumentJSONParser;

/**
 * A class responsible for handling functionality that is connected to Transkribus. Other (more
 * general) functionality should be included in SyncService.
 */
@Deprecated
public class TranskribusUtils {

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
        SyncStorage.getInstance(mContext).clearSyncList();

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startUpload: " +
                SyncStorage.getInstance(mContext).getUploadDocumentTitles());

        if (SyncStorage.getInstance(mContext).getUploadDocumentTitles() != null &&
                !SyncStorage.getInstance(mContext).getUploadDocumentTitles().isEmpty()) {
            Log.d(CLASS_NAME, "startUpload: the following titles are selected for upload: " +
                    SyncStorage.getInstance(mContext).getUploadDocumentTitles());
            mAreDocumentsPrepared = false;
            //        Start the upload of user selected dirs:
            TranskribusUtils.getInstance().uploadDocuments(SyncStorage.getInstance(mContext).getUploadDocumentTitles());
        } else {
            mAreDocumentsPrepared = true;
            Log.d(CLASS_NAME, "startUpload: no titles are selected for upload");
        }

        if (SyncStorage.getInstance(mContext).getUnfinishedUploadIDs() != null &&
                !SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().isEmpty()) {

            Log.d(CLASS_NAME, "startUpload: we have some unfinished work here");
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startUpload: we have some unfinished work here");
            SyncStorage.getInstance(mContext).printUnfinishedUploadIDs();
            mAreUnfinishedFilesPrepared = false;
            startFindingUnfinishedUploads(SyncStorage.getInstance(mContext).getUnfinishedUploadIDs());

        } else {

            SyncStorage.getInstance(mContext).printUnfinishedUploadIDs();
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

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "uploadDocuments");

        mSelectedDirs = selectedDirs;
        mIsCollectionCreated = false;
//
//        if (isCollectionIDSaved())
//            new CollectionsRequest(mContext);
//        else
//            createDocScanCollection();

//        new CollectionsRequest(mContext);

    }

    /**
     * Sends requests to the server asking in order to collect the unfinished files.
     *
     * @param unfinishedIDs
     */
    public void startFindingUnfinishedUploads(ArrayList<Integer> unfinishedIDs) {

        Log.d(CLASS_NAME, "startFindingUnfinishedUploads1");
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startFindingUnfinishedUploads1");

        mAreUnfinishedFilesPrepared = false;

//        We need here a deep copy, because we want to manipulate the member but not the
//        corresponding SyncStorage member:
        mUnfinishedUploadIDsProcessed = new ArrayList<>(unfinishedIDs);

//        for (Integer id : unfinishedIDs)
//            new UploadStatusRequest(mContext, id);

        Log.d(CLASS_NAME, "startFindingUnfinishedUploads2");
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "startFindingUnfinishedUploads2");

    }

    public void onUnfinishedUploadStatusReceived(Context context, int uploadID, String title,
                                                 ArrayList<String> unfinishedFileNames) {

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived1: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived1: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());

        boolean isFileDeleted = false;
        File dir = Helper.getMediaStorageDir(context.getResources().getString(R.string.app_name));

        for (String fileName : unfinishedFileNames) {

            File file = new File(dir.getAbsolutePath(), fileName);

            if (file.exists()) {
                Log.d(getClass().getName(), "onUnfinishedUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived: added unfinished file - id: " + uploadID + " file: " + file);
                //        Add the files to the sync list:
                SyncStorage.getInstance(mContext).addTranskribusFile(file, uploadID);
            } else {
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


        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived2: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived2: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        if (mUnfinishedUploadIDsProcessed.isEmpty()) {
            mAreUnfinishedFilesPrepared = true;
            checkFilesPrepared();
        }

        Log.d(CLASS_NAME, "onUnfinishedUploadStatusReceived3: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onUnfinishedUploadStatusReceived3: unfinished upload ids size" +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());

        SyncStorage.saveJSON(mContext);

//        if (areUnfinishedFilesProcessed()) {
//            mAreUnfinishedFilesPrepared = true;
//            checkFilesPrepared();
//        }

    }

    /**
     * Determines if there is a DocScan collection ID saved in SharedPreferences.
     *
     * @return
     */
    private boolean isCollectionIDSaved() {

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "isCollectionIDSaved");

        int savedCollectionID = getDocScanCollectionID();

        return (savedCollectionID != Settings.NO_ENTRY);

    }

    private int getDocScanCollectionID() {

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "getDocScanCollectionID");

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

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "saveDocScanCollectionID");

        boolean useTestServer = Helper.useTranskribusTestServer(mContext);

        if (useTestServer)
            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.TEST_COLLECTION_ID_KEY,
                    collectionID);
        else
            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY,
                    collectionID);


    }

//    public void onCollections(List<Collection> collections) {
//
//        Log.d(CLASS_NAME, "onCollections");
//        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onCollections");
//
//        int savedCollectionID = getDocScanCollectionID();
//
//        int maxId = -1;
//
//        for (Collection collection : collections) {
//            if (collection.getName().compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
////                docScanCollectionFound(collection);
////                return;
//
////                Is the collection id not saved yet?
//                if ((savedCollectionID == Settings.NO_ENTRY) || mIsCollectionCreated) {
//                    int id = collection.getID();
//                    if (id > maxId)
//                        maxId = id;
////                    mIsCollectionCreated = false;
////                    Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY, collection.getID());
////                    docScanCollectionFound(collection);
////                    return;
//                } else if ((savedCollectionID == collection.getID())) {
//                    Log.d(CLASS_NAME, "onCollections: docScanCollectionFound");
//                    docScanCollectionFound(collection.getID());
//                    return;
//                }
//
//            }
//        }
//
//        if (maxId > -1) {
//
//            Log.d(CLASS_NAME, "onCollections: saveDocScanCollectionID");
//            mIsCollectionCreated = false;
//            saveDocScanCollectionID(maxId);
////            Settings.getInstance().saveIntKey(mContext, Settings.SettingEnum.COLLECTION_ID_KEY, maxId);
//            docScanCollectionFound(maxId);
//            return;
//
//        }
//
//        createDocScanCollection();
//
//    }


    private void createDocScanCollection() {

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "createDocScanCollection");
//
//        if (mContext != null)
//            new CreateCollectionRequest(mContext, TRANSKRIBUS_UPLOAD_COLLECTION_NAME);

    }

    //    @Override
    public void onCollectionCreated(String collName) {

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onCollectionCreated");

        if (collName.compareTo(TRANSKRIBUS_UPLOAD_COLLECTION_NAME) == 0) {
            mIsCollectionCreated = true;
//            new CollectionsRequest(mContext);
        }

    }


    private void docScanCollectionFound(int id) {

        Log.d(CLASS_NAME, "docScanCollectionFound: id: " + id);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "docScanCollectionFound: id: " + id);
        uploadDirs(mSelectedDirs, id);

    }

    private void uploadDirs(ArrayList<String> dirs, int collectionId) {

//        Clean the documents, because some files might be deleted:
        Helper.cleanDocuments(mContext);

        mSelectedDirs = dirs;
        mNumUploadJobs = 0;

        Log.d(CLASS_NAME, "uploadDirs: preparing file: " + dirs + " for upload");
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "uploadDirs: preparing file: " + dirs + " for upload");

//        for (String dir : mSelectedDirs) {
        for (Iterator<String> it = mSelectedDirs.iterator(); it.hasNext(); ) {

            String dir = it.next();
            Log.d(CLASS_NAME, "uploadDirs: processing dir: " + dir);
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "uploadDirs: processing dir: " + dir);

            // Get the corresponding document:

            Document document = DocumentStorage.getInstance(mContext).getDocument(dir);

            if (document != null && document.getPages() != null && !document.getPages().isEmpty()) {

                Log.d(CLASS_NAME, "uploadDirs: processed dir: " + dir);

                //                Create the JSON object:
                String jsonString = DocumentJSONParser.toJSONString(document);
                Log.d(CLASS_NAME, "uploadDirs: json: " + jsonString);
                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "uploadDirs: json: " + jsonString);
//
//                try {
////                    JSONObject jsonObject = new JSONObject(jsonString);
////                    new StartUploadRequest(mContext, jsonObject, collectionId);
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
////                    TODO: tell the user that an error happened:
//                    FirebaseCrashlytics.getInstance().recordException(e);
//                    DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,
//                            "error while parsing json string: " + jsonString);
//                }
            } else {
                it.remove();
            }


        }

    }


    /**
     * Receives the uploadId for a document/directory and starts the upload job. Note that multiple
     * directories can be selected and we have to take assign each directory to its correct
     * uploadId (this is done by comparing the title).
     *
     * @param uploadId
     */
    public void onNewUploadIDReceived(int uploadId, String title) {

        Log.d(CLASS_NAME, "onNewUploadIDReceived: id: " + uploadId + " title: " + title);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, +uploadId + " title: " + title);

        SyncStorage.getInstance(mContext).getUnprocessedUploadIDs().add(new Integer(uploadId));

//        File selectedDir = getMatchingDir(title);
        Document document = DocumentStorage.getInstance(mContext).getDocument(title);
        if (document != null) {
            ArrayList<File> files = document.getFiles();
            if (files != null && !files.isEmpty()) {
                File[] imageList = files.toArray(new File[files.size()]);
                for (File file : imageList)
                    SyncStorage.getInstance(mContext).addTranskribusFile(file, uploadId);
            }
        }

        mNumUploadJobs++;

        mSelectedDirs.remove(title);

        SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().add(uploadId);
        Log.d(CLASS_NAME, "added ID to SyncStorage.unfinishedUploadIDs: " + uploadId);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "added ID to SyncStorage.unfinishedUploadIDs: " + uploadId);

//        // For each directory the upload request is finished and all files are added to the sync list:
//        if (mNumUploadJobs == mSelectedDirs.size())
//            mCallback.onSelectedFilesPrepared();

//        TODO: find out why this is not the case:
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


    public void uploadFile(final SyncStorage.Callback callback, final Context context, final TranskribusSyncFile syncFile) {

        final File file = syncFile.getFile();

        Log.d(CLASS_NAME, "uploading file: " + syncFile.toString());

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "uploading file: " + syncFile.toString());

//        Ion.with(context)
//                .load("PUT", Helper.getTranskribusBaseUrl(mContext) + "uploads/" +
//                        Integer.toString(syncFile.getUploadId()))
//                .setHeader("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
//                .setMultipartContentType("multipart/form-data")
//                .setMultipartFile("img", "application/octet-stream", file)
//                .asString()
//                .setCallback(new FutureCallback<String>() {
//                    @Override
//                    public void onCompleted(Exception e, String result) {
//
//                        if (e == null) {
////                            Log.d(getClass().getName(), "uploaded file: " + fileSync.toString());
//                            callback.onUploadComplete(syncFile);
//                            Log.d(CLASS_NAME, "uploaded file to collectionID: " +
//                                    syncFile.getUploadId() + " file: " + syncFile.toString());
//                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils",
//                                    "uploaded file: " + syncFile.toString());
//
//                            if (result.contains("<finished>")) {
//                                removeFromSyncStorageUnfinishedList(syncFile.getUploadId());
//
//                                Log.d(getClass().getName(), "finished upload with ID: " + syncFile.getUploadId());
////                                Log.d(getClass().getName(), "response: " + result);
//                                DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "finished upload with ID: " + syncFile.getUploadId());
//                            }
//
//                        } else {
//                            if (e instanceof FileNotFoundException) {
//                                DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME,
//                                        "FileNotFoundException: " + syncFile.getFile().toString());
//                                Log.d(CLASS_NAME, "FileNotFoundException: " +
//                                        syncFile.getFile().toString());
//                                handleMissingFile(syncFile, context);
//                                removeFromSyncStorageUnfinishedList(syncFile.getUploadId());
//                            } else {
//                                callback.onError(e);
//                            }
//                            Log.d(getClass().getName(), "error uploading file with upload ID: " + syncFile.getUploadId() + " fileSync: " + syncFile.toString());
//                            DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "error uploading file: " + e);
//                        }
//
//                    }
//                });
    }

    //    TODO: test what we really need here!
    private void handleMissingFile(final TranskribusSyncFile syncFile, Context context) {

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "handleMissingFile");

        mCallback.onFilesDeleted();

        removeFromSyncStorageUnfinishedList(syncFile.getUploadId());

//        Reset the corresponding document(s) containing the file:
        for (Document document : DocumentStorage.getInstance(context).getDocuments()) {

            for (Page page : document.getPages()) {
                if (page.getFile().compareTo(syncFile.getFile()) == 0) {
                    document.setIsAwaitingUpload(false);
                    document.setIsUploaded(false);
                    continue;
                }
            }
        }

        SyncStorage.saveJSON(mContext);
        DocumentStorage.saveJSON(mContext);

    }


    public void onError() {

        User.getInstance().setLoggedIn(false);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "onError");
        Log.d(CLASS_NAME, "unfinished size: " + SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());

        SyncStorage.saveJSON(mContext);

    }

    public void removeFromUnfinishedListAndCheckJob(int uploadID) {

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "removeFromUnfinishedListAndCheckJob");

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
     *
     * @param title
     */
    private void removeFromSyncStorageUploadList(String title) {

        DataLog.getInstance().writeUploadLog(mContext, "TranskribusUtils", "removeFromSyncStorageUploadList");

        SyncStorage.getInstance(mContext).removeDocument(title, mContext);
        SyncStorage.saveJSON(mContext);

    }

    private void removeFromSyncStorageUnfinishedList(int uploadID) {

        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: uploadID: " + uploadID);
        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "removeFromSyncStorageUnfinishedList: uploadID: " + uploadID);
        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());


        SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().remove(new Integer(uploadID));

        if (mUnfinishedUploadIDsProcessed == null)
            return;

        int idx = mUnfinishedUploadIDsProcessed.indexOf(new Integer(uploadID));
        if (idx != -1)
            mUnfinishedUploadIDsProcessed.remove(idx);

        Log.d(CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());

        DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "removeFromSyncStorageUnfinishedList: list size: " +
                SyncStorage.getInstance(mContext).getUnfinishedUploadIDs().size());


        for (Integer i : SyncStorage.getInstance(mContext).getUnfinishedUploadIDs()) {
            Log.d(CLASS_NAME, "still in list: " + i);
            DataLog.getInstance().writeUploadLog(mContext, CLASS_NAME, "still in list: " + i);
        }

        SyncStorage.saveJSON(mContext);

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
