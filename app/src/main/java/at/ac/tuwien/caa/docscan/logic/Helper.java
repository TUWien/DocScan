package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.exifinterface.media.ExifInterface;
import androidx.appcompat.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.rest.RestRequest;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;


/**
 * Created by fabian on 26.09.2017.
 */

public class Helper {

    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";
    private static final String CLASS_NAME = "Helper";


    /**
     * Start the CameraActivity and remove everything from the back stack.
     * @param context
     */
    public static void startCameraActivity(Context context) {

        Intent intent = new Intent(context, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);

    }


    public static void crashlyticsLog(String className, String methodName, String msg) {

        String log = className + ": " + methodName + ": " + msg;
        Crashlytics.log(log);

    }

    /**
     * Returns the root path to the directory in which the images are saved.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    public static File getMediaStorageDir(String appName) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return mediaStorageDir;
    }

    /**
     * Returns the root path to the directory in which the pdfs are saved.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    public static File getPDFStorageDir(String appName) {

        File pdfStorageDir;

        if (Build.VERSION.SDK_INT >= 19) {
            pdfStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), appName);
        }else{
            pdfStorageDir = new File(Environment.getExternalStorageDirectory(),  "Documents");
            pdfStorageDir = new File(pdfStorageDir.getAbsolutePath(), appName);
        }

        // Create the storage directory if it does not exist
        if (!pdfStorageDir.exists()) {
            if (!pdfStorageDir.mkdirs()) {
                return null;
            }
        }

        return pdfStorageDir;
    }


    /**
     * Returns the angle from an exif orientation
     * @param orientation
     * @return angle (in degrees)
     */
    public static int getAngleFromExif(int orientation) {

        switch (orientation) {

            case 1:
                return 0;
            case 6:
                return 90;
            case 3:
                return 180;
            case 8:
                return 270;

        }

        return -1;

    }

    public static int getExifOrientation(File outFile) throws IOException {
        if (outFile.exists()) {
            final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
            if (exif != null) {
                String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                return Integer.valueOf(orientation);
            }
        }
        else
            Log.d(CLASS_NAME, "getExifOrientation: not existing: " + outFile);


        return -1;

    }

    public static int getExifOrientation(String fileName) throws IOException {

        final ExifInterface exif = new ExifInterface(fileName);
        if (exif != null) {
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            return Integer.valueOf(orientation);
        }

        return -1;

    }

    public static void saveExifOrientation(File outFile, int orientation) throws IOException {
        final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
        if (exif != null) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(orientation));
            exif.saveAttributes();
        }

    }


    public static void saveExif(ExifInterface exif, String fileName) throws IOException
    {

//        TODO: check whcih tags should be deleted!
        String[] attributes = new String[]
                {
                        ExifInterface.TAG_ARTIST,
                        ExifInterface.TAG_BITS_PER_SAMPLE,
                        ExifInterface.TAG_BRIGHTNESS_VALUE,
                        ExifInterface.TAG_CFA_PATTERN,
                        ExifInterface.TAG_COLOR_SPACE,
                        ExifInterface.TAG_COMPONENTS_CONFIGURATION,
                        ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
                        ExifInterface.TAG_COMPRESSION,
                        ExifInterface.TAG_CONTRAST,
                        ExifInterface.TAG_COPYRIGHT,
                        ExifInterface.TAG_CUSTOM_RENDERED,
                        ExifInterface.TAG_DATETIME,
                        ExifInterface.TAG_DATETIME_DIGITIZED,
                        ExifInterface.TAG_DATETIME_ORIGINAL,
                        ExifInterface.TAG_DEFAULT_CROP_SIZE,
                        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
                        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
                        ExifInterface.TAG_DNG_VERSION,
                        ExifInterface.TAG_EXIF_VERSION,
                        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                        ExifInterface.TAG_EXPOSURE_INDEX,
                        ExifInterface.TAG_EXPOSURE_MODE,
                        ExifInterface.TAG_EXPOSURE_PROGRAM,
                        ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_FILE_SOURCE,
                        ExifInterface.TAG_FLASH,
                        ExifInterface.TAG_FLASHPIX_VERSION,
                        ExifInterface.TAG_FLASH_ENERGY,
                        ExifInterface.TAG_FOCAL_LENGTH,
                        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
                        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
                        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
                        ExifInterface.TAG_F_NUMBER,
                        ExifInterface.TAG_GAIN_CONTROL,
                        ExifInterface.TAG_GPS_ALTITUDE,
                        ExifInterface.TAG_GPS_ALTITUDE_REF,
                        ExifInterface.TAG_GPS_AREA_INFORMATION,
                        ExifInterface.TAG_GPS_DATESTAMP,
                        ExifInterface.TAG_GPS_DEST_BEARING,
                        ExifInterface.TAG_GPS_DEST_BEARING_REF,
                        ExifInterface.TAG_GPS_DEST_DISTANCE,
                        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
                        ExifInterface.TAG_GPS_DEST_LATITUDE,
                        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
                        ExifInterface.TAG_GPS_DEST_LONGITUDE,
                        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_DIFFERENTIAL,
                        ExifInterface.TAG_GPS_DOP,
                        ExifInterface.TAG_GPS_IMG_DIRECTION,
                        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF,
                        ExifInterface.TAG_GPS_MAP_DATUM,
                        ExifInterface.TAG_GPS_MEASURE_MODE,
                        ExifInterface.TAG_GPS_PROCESSING_METHOD,
                        ExifInterface.TAG_GPS_SATELLITES,
                        ExifInterface.TAG_GPS_SPEED,
                        ExifInterface.TAG_GPS_SPEED_REF,
                        ExifInterface.TAG_GPS_STATUS,
                        ExifInterface.TAG_GPS_TIMESTAMP,
                        ExifInterface.TAG_GPS_TRACK,
                        ExifInterface.TAG_GPS_TRACK_REF,
                        ExifInterface.TAG_GPS_VERSION_ID,
                        ExifInterface.TAG_IMAGE_DESCRIPTION,
//                        ExifInterface.TAG_IMAGE_LENGTH,
                        ExifInterface.TAG_IMAGE_UNIQUE_ID,
//                        ExifInterface.TAG_IMAGE_WIDTH,
                        ExifInterface.TAG_INTEROPERABILITY_INDEX,
                        ExifInterface.TAG_ISO_SPEED_RATINGS,
                        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
                        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                        ExifInterface.TAG_LIGHT_SOURCE,
                        ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MAKER_NOTE,
                        ExifInterface.TAG_MAX_APERTURE_VALUE,
                        ExifInterface.TAG_METERING_MODE,
                        ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_NEW_SUBFILE_TYPE,
                        ExifInterface.TAG_OECF,
                        ExifInterface.TAG_ORF_ASPECT_FRAME,
                        ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
                        ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
                        ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
//                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
//                        ExifInterface.TAG_PIXEL_X_DIMENSION,
//                        ExifInterface.TAG_PIXEL_Y_DIMENSION,
                        ExifInterface.TAG_PLANAR_CONFIGURATION,
                        ExifInterface.TAG_PRIMARY_CHROMATICITIES,
                        ExifInterface.TAG_REFERENCE_BLACK_WHITE,
                        ExifInterface.TAG_RELATED_SOUND_FILE,
                        ExifInterface.TAG_RESOLUTION_UNIT,
//                        ExifInterface.TAG_ROWS_PER_STRIP,
                        ExifInterface.TAG_RW2_ISO,
                        ExifInterface.TAG_RW2_JPG_FROM_RAW,
                        ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
                        ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
                        ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
                        ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
                        ExifInterface.TAG_SAMPLES_PER_PIXEL,
                        ExifInterface.TAG_SATURATION,
                        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
                        ExifInterface.TAG_SCENE_TYPE,
                        ExifInterface.TAG_SENSING_METHOD,
                        ExifInterface.TAG_SHARPNESS,
                        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                        ExifInterface.TAG_SOFTWARE,
                        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
                        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
//                        ExifInterface.TAG_STRIP_BYTE_COUNTS,
//                        ExifInterface.TAG_STRIP_OFFSETS,
                        ExifInterface.TAG_SUBFILE_TYPE,
                        ExifInterface.TAG_SUBJECT_AREA,
                        ExifInterface.TAG_SUBJECT_DISTANCE,
                        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
                        ExifInterface.TAG_SUBJECT_LOCATION,
                        ExifInterface.TAG_SUBSEC_TIME,
                        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                        ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
                        ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
                        ExifInterface.TAG_TRANSFER_FUNCTION,
                        ExifInterface.TAG_USER_COMMENT,
                        ExifInterface.TAG_WHITE_BALANCE,
                        ExifInterface.TAG_WHITE_POINT,
//                        ExifInterface.TAG_X_RESOLUTION,
                        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
                        ExifInterface.TAG_Y_CB_CR_POSITIONING,
                        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
//                        ExifInterface.TAG_Y_RESOLUTION,

                };

        ExifInterface newExif = new ExifInterface(fileName);

        for (int i = 0; i < attributes.length; i++)
        {
            String value = exif.getAttribute(attributes[i]);
            if (value != null)
                newExif.setAttribute(attributes[i], value);
        }

        newExif.resetOrientation();

        newExif.saveAttributes();

    }

    public static boolean isDocumentFocused(Document document) {

        if (document == null)
            return true;

        for (Page page : document.getPages()) {
            if (!page.isFocused())
                return false;
        }

        return true;

    }

    public static boolean rotateExif(File outFile)  {

        final ExifInterface exif;
        try {
            exif = new ExifInterface(outFile.getAbsolutePath());
            if (exif != null) {

//            Note the regular android.media.ExifInterface has no method for rotation, but the
//            android.support.media.ExifInterface does.

                exif.rotate(90);
                exif.saveAttributes();

                if (!PageDetector.isCropped(outFile.getAbsolutePath()))
                    PageDetector.rotate90Degrees(outFile.getAbsolutePath());

                return true;
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        return false;

    }

//    public static List<Document> getDocuments(String appName){
//
//        List<Document> documents = new ArrayList<>();
//
//        File mediaStorageDir = getMediaStorageDir(appName);
//
//        FileFilter directoryFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return file.isDirectory();
//            }
//        };
//
//        File[] folders = mediaStorageDir.listFiles(directoryFilter);
//        if (folders == null)
//            return documents;
//
//        ArrayList<File> dirs = new ArrayList<>(Arrays.asList(folders));
//
//        for (File dir : dirs) {
//            Document document = getDocument(dir.getAbsolutePath());
//            documents.add(document);
//        }
//
//        return documents;
//
//    }

    /**
     *
     * @param appName
     * @param fileName
     * @return
     */
    public static File getFile(String appName, String fileName){

        File mediaStorageDir = getMediaStorageDir(appName);

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] folders = mediaStorageDir.listFiles(directoryFilter);
        if (folders == null)
            return null;

        ArrayList<File> dirs = new ArrayList<>(Arrays.asList(folders));

//        Iterate over the sub directories to find the image file:
        for (File dir : dirs) {

            ArrayList<File> files = getImageList(dir);
            for (File file : files) {
                if (file.getName().equals(fileName))
                    return file;

            }
        }

        return null;

    }

    public static ArrayList<Document> getValidDocuments(List<Document> documents) {

        ArrayList<Document> validDocuments = new ArrayList<>();

        if (documents == null || documents.size() == 0)
            return validDocuments;

        for (Document document : documents) {
            if (document.getPages() != null && document.getTitle() != null)
                validDocuments.add(document);
        }

        // Sort it based on the upload status:
        java.util.Collections.sort(validDocuments, new DocumentComparator());

        return validDocuments;

    }

    public static List<Document> getNonEmptyDocuments(List<Document> documents) {

        List<Document> nonEmptyDocuments = new ArrayList<>();

        if (documents == null || documents.size() == 0)
            return nonEmptyDocuments;

        for (Document document : documents) {
            if (document.getPages() != null && document.getPages().size() > 0)
                nonEmptyDocuments.add(document);
        }

        // Sort it based on the upload status:
        java.util.Collections.sort(nonEmptyDocuments, new DocumentComparator());

        return nonEmptyDocuments;

    }

    static class DocumentComparator implements Comparator<Document>
    {
        @Override public int compare(Document doc1, Document doc2)
        {
            int value;
            if (doc1.isUploaded() && !doc2.isUploaded())
                value = 1;
            else if (!doc1.isUploaded() && doc2.isUploaded())
                value = -1;
            else {
                value = doc1.getTitle().compareToIgnoreCase(doc2.getTitle());
            }

            return value;

        }
    }



//    public static Document getDocument(String dirName) {
//
//        Document document = new Document();
//        ArrayList<File> fileList = getImageList(dirName);
//        ArrayList<Page> pages = filesToPages(fileList);
//        document.setPages(pages);
//        File file = new File(dirName);
//        document.setTitle(file.getName());
//
//        boolean isDocumentUploaded = areFilesUploaded(fileList);
//        document.setIsUploaded(isDocumentUploaded);
//
//        boolean isDocumentCropped = areFilesCropped(fileList);
//        document.setIsCropped(isDocumentCropped);
//
//        if (!isDocumentUploaded) {
//            boolean isAwaitingUpload = isDirAwaitingUpload(new File(dirName), fileList);
//            document.setIsAwaitingUpload(isAwaitingUpload);
//        }
//
//        return document;
//
//    }

    public static String getActiveDocumentTitle(Context context) {

        SharedPreferences sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        String seriesName = sharedPref.getString(
                context.getResources().getString(R.string.series_name_key),
                context.getResources().getString(R.string.series_name_default));

        return seriesName;

    }

    public static boolean areFilesCropped(Document document) {

        if (document != null) {
            ArrayList<File> files = document.getFiles();
            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    if (ImageProcessLogger.isAwaitingCropping(file))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Iterates over documents and checks if the files are still existing.
     * @param context
     */
    public static void cleanDocuments(Context context) {


        ArrayList<Document> documents = DocumentStorage.getInstance(context).getDocuments();
        for (Document document : documents) {

            Iterator<Page> iter = document.getPages().iterator();

            while (iter.hasNext()) {
                Page page = iter.next();
                File file = page.getFile();
                if (!file.exists()) {
                    iter.remove();
//                    Removes the document from the uploaded list and the awaiting upload list:
//                    SyncStorage.getInstance(context).removeDocumentFromUploadList(document);
                    SyncStorage.getInstance(context).removeDocument(document.getTitle(), context);
                }
            }

        }

    }

    public static boolean areFilesCropped(ArrayList<File> fileList) {

        if (fileList == null)
            return false;

        if (fileList.size() == 0)
            return false;


        for (File file : fileList) {
            if (ImageProcessLogger.isAwaitingCropping(file))
                return true;
        }

        return false;

    }

    /**
     * Returns an input filter for edit text that prevents that the user enters non valid characters
     * for directories (under Windows and Unix).
     * @return
     */
    public static InputFilter getDocumentInputFilter() {

        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.length() < 1)
                    return null;
                char last = source.charAt(source.length() - 1);
                if(RESERVED_CHARS.indexOf(last) > -1)
                    return source.subSequence(0, source.length() - 1);

                return null;
            }
        };

        return filter;

    }

//    TODO: this should later be changed:
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

    private static ArrayList<File> getImageList(String dir) {

        return getImageList(new File(dir));

    }

    private static ArrayList<File> getImageList(File file) {

        File[] files = getImageArray(file);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }


    private static ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    public static File[] getImageArray(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return (file.getPath().endsWith(".jpg")||file.getPath().endsWith(".jpeg"));
//                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        if (files != null && files.length > 0)
            Arrays.sort(files);

        return files;
    }

    public static boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        return isConnected;

    }

    public static String getNetworkResponse(VolleyError error) {

        try {
            if (error == null || error.networkResponse == null || error.networkResponse.data == null)
                return "error is null";
            String body = new String(error.networkResponse.data, "UTF-8");
            return body;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
//            This should not be null, because we should use the proper encoding:
            return null;
        }

    }

    /**
     * Shows a dialog in case of ActivityNotFoundExceptions. This exceptions happen if the user
     * wants to share a file with another app and no app is found for that purpose.
     * @param context
     */
    public static void showActivityNotFoundAlert(Context context) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.activity_not_found_title)
                .setPositiveButton(R.string.dialog_ok_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .setMessage(R.string.activity_not_found_text);

        final AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_cancel_text),
//                new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                alertDialog.cancel();
//            }
//        });
        alertDialog.show();

    }

    /**
     * Check the device to make sure it has the Google Play Services APK. Taken from:
     * https://stackoverflow.com/a/48224404/9827698
     */
    public static boolean checkPlayServices(Context context) {

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable
                (context);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.d(CLASS_NAME, "This device does not support Google Play Services. " +
                    "Push notifications are not supported");
            return false;
        }
        return true;

    }

    /**
     * Returns the url of the production or test server, depending on the user setting.
     * @param context
     * @return
     */
    public static String getTranskribusBaseUrl(Context context) {

        boolean useTestServer = useTranskribusTestServer(context);

        if (useTestServer)
            return RestRequest.BASE_TEST_URL;
        else
            return RestRequest.BASE_URL;

    }

    public static boolean useTranskribusTestServer(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(context.getResources().getString(
                R.string.key_use_test_server), false);

    }


    /**
     * Returns the correct singular or plural form of the word documents, regarding the number of
     * documents.
     * @param numDoc
     * @return
     */
    public static String getDocumentSingularPlural(Context context, int numDoc) {

        if (numDoc == 1)
            return context.getResources().getString(R.string.sync_selection_single_document_text);
        else
            return context.getResources().getString(R.string.sync_selection_many_documents_text);

    }



}
