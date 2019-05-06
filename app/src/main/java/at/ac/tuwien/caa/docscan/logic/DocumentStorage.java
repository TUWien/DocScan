package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.sync.SyncStorage;

public class DocumentStorage {

    public static final String DOCUMENT_STORE_FILE_NAME = "documentstorage.json";
    private static final String CLASS_NAME = "DocumentStorage";
    private static DocumentStorage sInstance;

    private ArrayList<Document> mDocuments;
    private String mTitle = null;

    public DocumentStorage() {

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

            boolean isDocumentCropped = Helper.areFilesCropped(document);
            document.setIsCropped(isDocumentCropped);

        }

    }

    public static void loadJSON(Context context) {

        Log.d(CLASS_NAME, "loadJSON");

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        if (!storeFile.exists()) {
            sInstance = new DocumentStorage();
            sInstance.setTitle(Helper.getActiveDocumentTitle(context));
        }

        else {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(storeFile));
                Gson gson = new Gson();
                sInstance = gson.fromJson(bufferedReader, DocumentStorage.class);
//                I do not know why this is sometimes happening...
                if (sInstance == null) {
                    Crashlytics.log("could not read json");
                    Crashlytics.logException(new Throwable());
                    sInstance = new DocumentStorage();
                }

                if (sInstance.getTitle() == null)
                    sInstance.setTitle(Helper.getActiveDocumentTitle(context));

            } catch (FileNotFoundException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                sInstance = new DocumentStorage();
            }
        }

    }

    public static void saveJSON(Context context) {

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(storeFile));
            String documentStorage = new Gson().toJson(sInstance);
            Log.d(CLASS_NAME, "json: " + documentStorage);
            writer.write(documentStorage);
            writer.close();

        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

    }


}



