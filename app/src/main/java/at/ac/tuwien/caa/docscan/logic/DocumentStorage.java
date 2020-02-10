package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.sync.SyncStorage;

public class DocumentStorage {

    public static final String DOCUMENT_STORE_FILE_NAME = "documentstorage.json";
//    public static final String DOCUMENT_BACKUP_FILE_NAME = "documentbackup.json";
    private static final String CLASS_NAME = "DocumentStorage";
    private static DocumentStorage sInstance;

    private ArrayList<Document> mDocuments;
    private String mTitle = null;

    public DocumentStorage() {

        Log.d(CLASS_NAME, "creating new document");

        mDocuments = new ArrayList<>();

    }

    public ArrayList<Document> getDocuments() {

        return mDocuments;

    }

    public void setDocuments(ArrayList<Document> documents) {

        mDocuments = documents;

    }

    public void setPageFocused(String fileName, boolean isFocused) {

        for (Document document : mDocuments) {
            if (document != null) {
                int docIdx = document.getFileNames().indexOf(fileName);
                if (docIdx != -1)
                    document.getPages().get(docIdx).setIsFocused(isFocused);
            }
        }

    }

//    public void setPageAsUnsharp(String fileName) {
//
//        for (Document document : mDocuments) {
//            if (document != null) {
//                int docIdx = document.getFileNames().indexOf(fileName);
//                if (docIdx != -1)
//                    document.getPages().get(docIdx).setIsFocused(false);
//            }
//        }
//
//    }

    public void setTitle(String title) {

        Log.d(CLASS_NAME, "setTitle: " + title);

        mTitle = title;

    }

    /**
     * Creates a new document inf the title is not already assigned to another document.
     * @param title
     * @return
     */
    public boolean createNewDocument(String title) {

        if (title == null || title.isEmpty())
            return false;

        if (isTitleAlreadyAssigned(title))
            return false;

        Document document = new Document(title);

        if (mDocuments == null)
            mDocuments = new ArrayList<>();

        mDocuments.add(document);

        mTitle = title;

//        The document has been created:
        return true;

    }

    public boolean isTitleAlreadyAssigned(String title) {

        for (Document document : mDocuments) {

            if (document.getTitle() != null && title.compareToIgnoreCase(document.getTitle()) == 0)
                return true;
        }

        return false;

    }

    /**
     * Returns the last file name of the pages of the active document
     * @return
     */
    public String getLastPageFileInActiveDocument() {

        if (mTitle != null) {
            //        Retrieve the active document:
            Document document = getDocument(mTitle);
            if (document != null) {
                ArrayList<Page> pages = document.getPages();
                if (pages != null && !pages.isEmpty())
                    return pages.get(pages.size()-1).getFile().getAbsolutePath();
            }
        }
        return null;

    }

    public Document getActiveDocument() {

        return getDocument(mTitle);

    }

    public void setActiveDocumentTitle(String title) {

        mTitle = title;

    }

    public void generateDocument(File file, Context context) {

        if (mTitle == null)
            mTitle = Helper.getActiveDocumentTitle(context);

        Document document = getDocument(mTitle);
        if (document == null || document.getPages() == null) {
            createNewDocument(mTitle);
            document = getDocument(mTitle);
        }

        document.getPages().add(new Page(file));


    }

    /**
     * Returns true if a document with the active title (mTitle) is existing and the file is added
     * to it.
     * @param file
     * @return
     */
    public boolean addToActiveDocument(File file) {

        if (mDocuments == null || mDocuments.isEmpty() || mTitle == null)
            return false;

        Document document = getDocument(mTitle);
        if (document != null) {
            document.getPages().add(new Page(file));
            return true;
        }
        else
            return false;

    }

    public String getTitle() {

        return mTitle;

    }


    public Document getDocument(String title) {

        if (title == null)
            return null;

        if (mDocuments != null) {
            for (Document document : mDocuments) {
                if (document == null) {
                    Crashlytics.logException(new Throwable(CLASS_NAME + ": document is null"));
                    continue;
                }
                if (document != null && document.getTitle() != null && document.getTitle().compareToIgnoreCase(title) == 0)
                    return document;
            }
        }

        return null;

    }

    public static boolean isInstanceNull() {

        return sInstance == null;

    }

    public static DocumentStorage getInstance(Context context) {

//        At first try to read the JSON file: Note that the system might close the app and restart if
//        without opening StartActivity, where the JSON file is read. So we have to take care that
//        the JSON file is read if the singleton is null. See also:
//        https://medium.com/inloopx/android-process-kill-and-the-big-implications-for-your-app-1ecbed4921cb

        if (sInstance == null)
            loadJSON(context);

        if (sInstance == null)
            sInstance = new DocumentStorage();

        return sInstance;

    }

    public void updateStatus(Context context) {

//        Check if some files are removed from outside:
        Helper.cleanDocuments(context);

        for (Document document : mDocuments) {

            boolean areFilesUploaded = SyncStorage.getInstance(context).areFilesUploaded(document.getFiles());
            document.setIsUploaded(areFilesUploaded);

            if (!areFilesUploaded) {
                boolean isAwaitingUpload = SyncStorage.getInstance(context).
                        isDocumentAwaitingUpload(document);
                document.setIsAwaitingUpload(isAwaitingUpload);
            }

            boolean currentlyProcessed = Helper.isCurrentlyProcessed(document);
            document.setIsCurrentlyProcessed(currentlyProcessed);

        }

    }

    public static String convertStreamToString(InputStream is) throws IOException {
        // http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        Boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if(firstLine){
                sb.append(line);
                firstLine = false;
            } else {
                sb.append("\n").append(line);
            }
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath){
        File fl = new File(filePath);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "nothing";
    }

    public static void loadJSON(Context context) {

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        if (!storeFile.exists()) {
            sInstance = new DocumentStorage();
            sInstance.setTitle(Helper.getActiveDocumentTitle(context));
        }

        else {

//            File tempFile = null;

            try {

                BufferedReader bufferedReader = new BufferedReader(new FileReader(storeFile));
                Gson gson = new Gson();
                sInstance = gson.fromJson(bufferedReader, DocumentStorage.class);
                bufferedReader.close();

                if (sInstance == null) {
                    Crashlytics.log("could not read json test: " + getStringFromFile(storeFile.getAbsolutePath()));
                    Crashlytics.logException(new Throwable());
                    sInstance = new DocumentStorage();
                }

////                    Try to recover the backup file:
//                    if (backupStoreFile != null && backupStoreFile.exists()) {
//                        Log.d(CLASS_NAME, "recovering backup");
////                        Copy the backup file:
//                        Utility.Companion.copyFile(backupStoreFile, storeFile);
//                        backupStoreFile.renameTo(storeFile);
//                        bufferedReader = new BufferedReader(new FileReader(storeFile));
//                        gson = new Gson();
//                        sInstance = gson.fromJson(bufferedReader, DocumentStorage.class);
//                        bufferedReader.close();
//                        if (sInstance == null)
//                            sInstance = new DocumentStorage();
//                    }
//                    else
//                        sInstance = new DocumentStorage();
//                }
//                The storage file is valid so create a backup:
//                else {
////                    First copy the storage to a temporary file:
////                    Unfortunately, I do not know if Kotlin copyTo is atomic, so stay on the safe
////                    side...
//
//                    tempFile = File.createTempFile("document_bp", ".json", path);
////                    Replace the current backup file:
//                    if (Utility.Companion.copyFile(storeFile, tempFile))
//                        tempFile.renameTo(backupStoreFile);
//
//                }

                if (sInstance.getTitle() == null)
                    sInstance.setTitle(Helper.getActiveDocumentTitle(context));

            } catch (Exception e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                if (sInstance == null)
                    sInstance = new DocumentStorage();
            }
//            finally {
//                if (tempFile != null && tempFile.exists())
//                    tempFile.delete();
//            }
        }

    }

    /**
     * Saves the current sInstance to a JSON file.
     * @param context
     */
    public static void saveJSON(Context context) {

//        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

//        Save it first as a temp file, because the saving might be interrupted.
        File tempFile = null;

        try {
            File path = context.getFilesDir();
            tempFile = File.createTempFile("docstore", ".json", path);

            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            String documentStorage = new Gson().toJson(sInstance);
            writer.write(documentStorage);
            writer.close();

            boolean isSaved = tempFile.exists();
            if (isSaved) {
                File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);
//                Rename the temp file:
                tempFile.renameTo(storeFile);
            }

        } catch (Exception e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        } finally {
//            Delete the temporary file, if it still exists:
            if (tempFile != null && tempFile.exists())
                tempFile.delete();
        }

    }

    public static void saveJSONOld(Context context) {

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(storeFile));
            String documentStorage = new Gson().toJson(sInstance);
            writer.write(documentStorage);
            writer.close();

        } catch (Exception e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

    }


}



