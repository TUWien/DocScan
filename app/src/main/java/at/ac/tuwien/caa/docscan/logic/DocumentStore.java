package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.rest.User;

public class DocumentStore implements Serializable {

    private static final String DOCUMENT_STORE_FILE_NAME = "documentstore.txt";
    private static final String CLASS_NAME = "DocumentStore";
    private static DocumentStore mInstance;

    private ArrayList<Document> mDocuments;
    private Document mActiveDocument;

    public DocumentStore() {

        mDocuments = new ArrayList<>();

    }

    public ArrayList<Document> getDocuments() {

        return mDocuments;

    }

    public void setActiveDocument(Document activeDocument) {

        mActiveDocument = activeDocument;

    }

    public void createNewDocument(String title) {

        Document document = new Document(title);

        if (mDocuments == null)
            mDocuments = new ArrayList<>();

        mDocuments.add(document);

        mActiveDocument = document;

    }

    public void addToActiveDocument(File file) {

//        TODO: temporary until the new document structure is done:
        if (mActiveDocument == null)
            mActiveDocument = new Document(User.getInstance().getDocumentName());

        if (mDocuments.isEmpty())
            mDocuments.add(mActiveDocument);

        if (mActiveDocument.getPages() != null)
            mActiveDocument.getPages().add(new Page(file));

    }


    public static DocumentStore getInstance() {

        if (mInstance == null) {
            mInstance = new DocumentStore();
        }

        return mInstance;

    }

    public static void readFromDisk(Context context) {

        File path = context.getFilesDir();
        File storeFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        if (!storeFile.exists())
            mInstance = new DocumentStore();
        else {
            Log.d(CLASS_NAME, "readFromDisk");

            try {
                FileInputStream fis = new FileInputStream(storeFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                mInstance = (DocumentStore) ois.readObject();
                ois.close();
            } catch (Exception e) {
                Log.d(CLASS_NAME, e.toString());
            }

        }

    }

    public static void saveToDisk(Context context) {

        File path = context.getFilesDir();
        File syncFile = new File(path, DOCUMENT_STORE_FILE_NAME);

        try {
            FileOutputStream fos = new FileOutputStream(syncFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mInstance);
            oos.close();
        }
        catch(Exception e) {
            int b = 0;

        }

    }



}
