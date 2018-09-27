package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.CropLogger;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;

/**
 * A class that is used for a migration of how the old documents (with serialization) are saved to
 * the way new documents are saved (via DocumentStorage and json). Note that new is always better.
 */
public class DocumentMigrator {


    public static void migrate(Context context) {

//        TODO: acquire the permission to read the data

//        Get a list of 'old' documents:
        String appName = context.getResources().getString(R.string.app_name);
        ArrayList<Document> documents = getDocuments(appName);
        File imgDir = Helper.getMediaStorageDir(appName);

        for (Document document : documents)
            movePages(document, imgDir);

        String activeDocumentTitle = getActiveDocumentTitle(context);

//        Save the new documents:
        DocumentStorage.getInstance().setDocuments(documents);
        DocumentStorage.getInstance().setTitle(activeDocumentTitle);
        DocumentStorage.saveJSON(context);

    }

    private static String getActiveDocumentTitle(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String seriesName = sharedPref.getString(
                context.getResources().getString(R.string.series_name_key),
                context.getResources().getString(R.string.series_name_default));

        return seriesName;

    }

    private static void movePages(Document document, File imgDir) {


//        Move the files:
        for (Page page : document.getPages()) {
            File oldFile = page.getFile();
            File newFile = new File(imgDir, oldFile.getName());
            oldFile.renameTo(newFile);
            page.setFile(newFile);
        }

//        Delete the directory:
        File dir = new File(imgDir, document.getTitle());
        if (dir.exists())
            dir.delete();

    }

    public static ArrayList<Document> getDocuments(String appName){

        ArrayList<Document> documents = new ArrayList<>();

        File mediaStorageDir = Helper.getMediaStorageDir(appName);

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] folders = mediaStorageDir.listFiles(directoryFilter);
        if (folders == null)
            return documents;

        ArrayList<File> dirs = new ArrayList<>(Arrays.asList(folders));

        for (File dir : dirs) {
            Document document = getDocument(dir.getAbsolutePath());
            documents.add(document);
        }

        return documents;

    }

    public static Document getDocument(String dirName) {

        Document document = new Document();
        ArrayList<File> fileList = getImageList(dirName);
        ArrayList<Page> pages = filesToPages(fileList);
        document.setPages(pages);
        File file = new File(dirName);
        document.setTitle(file.getName());

        boolean isDocumentUploaded = areFilesUploaded(fileList);
        document.setIsUploaded(isDocumentUploaded);

        boolean isDocumentCropped = areFilesCropped(fileList);
        document.setIsCropped(isDocumentCropped);

        if (!isDocumentUploaded) {
            boolean isAwaitingUpload = isDirAwaitingUpload(new File(dirName), fileList);
            document.setIsAwaitingUpload(isAwaitingUpload);
        }

        return document;

    }

    private static ArrayList<File> getImageList(String dir) {

        return getImageList(new File(dir));

    }

    private static ArrayList<File> getImageList(File file) {

        File[] files = Helper.getImageArray(file);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private static boolean areFilesUploaded(ArrayList<File> fileList) {

        if (fileList == null)
            return false;

        if (fileList.size() == 0)
            return false;

        File[] files = fileList.toArray(new File[fileList.size()]);

        return SyncInfo.getInstance().areFilesUploaded(files);

//        if (files.length == 0)
//            return false;
//
//        // Check if every file contained in the folder is already uploaded:
//        for (File file : files) {
//            if (!isFileUploaded(file))
//                return false;
//        }
//
//        return true;

    }


    private static boolean isDirAwaitingUpload(File dir, ArrayList<File> fileList) {

        if (dir == null)
            return false;

        if (fileList == null)
            return false;

        if (fileList.size() == 0)
            return false;

        File[] files = fileList.toArray(new File[fileList.size()]);

        return SyncInfo.getInstance().isDirAwaitingUpload(dir, files);

    }



    private static ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    public static boolean areFilesCropped(ArrayList<File> fileList) {

        if (fileList == null)
            return false;

        if (fileList.size() == 0)
            return false;


        for (File file : fileList) {
            if (CropLogger.isAwaitingCropping(file))
                return true;
        }

        return false;

    }


}
