package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;
import at.ac.tuwien.caa.docscan.ui.document.DocumentJSONParser;

public class DocumentStorage {

    private static final String DOCUMENT_STORE_FILE_NAME = "documentstorage.json";
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

    public void setTitle(String title) {

        mTitle = title;

    }

    public boolean openDocument(String title) {

        for (Document document : mDocuments) {
            if (document.getTitle().compareToIgnoreCase(title) == 0) {
                mTitle = title;
                return true;
            }
        }

        return false;

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

            if (title.compareToIgnoreCase(document.getTitle()) == 0)
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

    public void addToActiveDocument(File file) {

        if (mDocuments.isEmpty())
            mDocuments.add(new Document(mTitle));

        Document document = getDocument(mTitle);
        if (document != null)
            document.getPages().add(new Page(file));

//        if (mActiveDocument.getPages() != null)
//            mActiveDocument.getPages().add(new Page(file));

    }

    public String getTitle() {

        return mTitle;

    }

    public Document getDocument(String title) {

        for (Document document : mDocuments) {
            if (document.getTitle().compareToIgnoreCase(title) == 0)
                return document;
        }

        return null;

    }

    public static boolean isInstanceNull() {

        return sInstance == null;

    }

    public static DocumentStorage getInstance() {

//        if (sInstance == null)
//            loadJSON(context);

        if (sInstance == null)
            sInstance = new DocumentStorage();

        return sInstance;

    }

    public void updateStatus() {

        for (Document document : mDocuments) {

            boolean areFilesUploaded = SyncStorage.getInstance().areFilesUploaded(document.getFiles());
            document.setIsUploaded(areFilesUploaded);

            if (!areFilesUploaded) {
                boolean isAwaitingUpload = SyncStorage.getInstance().
                        isDocumentAwaitingUpload(document);
                document.setIsAwaitingUpload(isAwaitingUpload);
            }

            boolean isDocumentCropped = Helper.areFilesCropped(document);
            document.setIsCropped(isDocumentCropped);

        }



    }

    public static void loadJSON(Context context) {

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        if (!storeFile.exists()) {
            sInstance = new DocumentStorage();
            DocumentStorage.getInstance().setTitle(Helper.getActiveDocumentTitle(context));
        }


        else {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(storeFile));

                Gson gson = new Gson();
                sInstance = gson.fromJson(bufferedReader, DocumentStorage.class);

            } catch (FileNotFoundException e) {
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
            e.printStackTrace();
        }

    }

}



