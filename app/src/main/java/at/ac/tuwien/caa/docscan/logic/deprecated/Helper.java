package at.ac.tuwien.caa.docscan.logic.deprecated;

import android.app.Activity;
import android.os.Environment;
import android.text.InputFilter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation;
import at.ac.tuwien.caa.docscan.extensions.DateExtensionKt;
import timber.log.Timber;


/**
 * Created by fabian on 26.09.2017.
 */

public class Helper {

    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";
    private static final String CLASS_NAME = "Helper";
    public static final String START_SAVE_JSON_CALLER = "START_SAVE_JSON_CALLER";
    public static final String END_SAVE_JSON_CALLER = "END_SAVE_JSON_CALLER";

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


    public static int getDPI(double cameraDistance, float horizontalViewAngle, int imgW) {

        double thetaH = Math.toRadians(horizontalViewAngle);

        //Size in inches
        double printWidth = 2 * cameraDistance * Math.tan(thetaH / 2);

        return (int) Math.round((double) imgW / printWidth);

    }

    /**
     * Returns the root path to the directory in which the pdfs are saved.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    public static File getPDFStorageDir(String appName) {

        File pdfStorageDir =
                new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), appName);

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
     *
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
                if (orientation == null)
                    return -1;
                return Integer.valueOf(orientation);
            }
        } else
            Timber.d("getExifOrientation: not existing: " + outFile);


        return -1;

    }

    public static int getSafeExifOrientation(File outFile) {
        try {
            return getExifOrientation(outFile);
        } catch (Exception e) {
            Timber.e(e, "Exif orientation couldn't be determined!");
            return -1;
        }
    }

    public static Rotation getNewSafeExifOrientation(File outFile) {
        try {
            return Rotation.Companion.getRotationByExif(getExifOrientation(outFile));
        } catch (Exception e) {
            Timber.e(e, "Exif orientation couldn't be determined!");
            return Rotation.Companion.getRotationByExif(-1);
        }
    }

    public static int getExifOrientation(String fileName) throws IOException {

        final ExifInterface exif = new ExifInterface(fileName);
        if (exif != null) {
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (orientation == null)
                return -1;
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


    public static void saveExif(ExifInterface exif, String fileName) throws IOException {

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

        for (int i = 0; i < attributes.length; i++) {
            String value = exif.getAttribute(attributes[i]);
            if (value != null)
                newExif.setAttribute(attributes[i], value);
        }

        newExif.resetOrientation();

        newExif.saveAttributes();

    }


    public static boolean rotateExif(File outFile) {

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
            Timber.e(e);
        }

        return false;

    }

    /**
     * @param appName
     * @param fileName
     * @return
     */
    public static File getFile(String appName, String fileName) {

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

    /**
     * Replaces an image with a new image. Saves the image first temporary in order to not destroy
     * the original image in case of an interruption.
     *
     * @param fileName
     * @param mat
     * @return
     */
    public static boolean replaceImage(String fileName, Mat mat) {

//            First get a temporary file name:
//        String tempFileName = fileName;
////                String fileExt = MimeTypeMap.getFileExtensionFromUrl(fileTmp.getAbsolutePath());
//        int dotPos = tempFileName.lastIndexOf(".");
//        tempFileName = tempFileName.substring(0, dotPos) + "-temp" + tempFileName.substring(dotPos);

        File originalFile = new File(fileName);
        if (originalFile == null)
            return false;

        File tempFile;
        try {
            tempFile = File.createTempFile("img", ".jpg", originalFile.getParentFile());
        } catch (IOException e) {
            Timber.e(e, "Helper.saveMat");
            return false;
        }

        if (tempFile == null)
            return false;

        String tempFileName = tempFile.getAbsolutePath();

        boolean isSaved = false;

        try {
//            First copy the exif data, because we do not want to loose this data:
            ExifInterface exif = new ExifInterface(fileName);
            boolean fileSaved = Imgcodecs.imwrite(tempFileName, mat);
            if (fileSaved) {
                saveExif(exif, tempFileName);
//                Rename the file:
                isSaved = tempFile.renameTo(new File(fileName));
            }
        } catch (IOException e) {
            Timber.e(e, "Helper.saveMat");
        } finally {
//            Delete the temporary file, if it still exists:
            if (tempFile != null && tempFile.exists())
                tempFile.delete();
        }

        return isSaved;

    }


    /**
     * Returns an input filter for edit text that prevents that the user enters non valid characters
     * for directories (under Windows and Unix).
     *
     * @return
     */
    public static InputFilter getDocumentInputFilter() {

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (source.length() < 1)
                return null;
            char last = source.charAt(source.length() - 1);
            if (RESERVED_CHARS.indexOf(last) > -1)
                return source.subSequence(0, source.length() - 1);

            return null;
        };

        return filter;

    }

    public static InputFilter[] getDocumentInputFilters() {

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (source.length() < 1)
                return null;
            char last = source.charAt(source.length() - 1);
            if (RESERVED_CHARS.indexOf(last) > -1)
                return source.subSequence(0, source.length() - 1);

            return null;
        };

        return new InputFilter[]{new InputFilter.LengthFilter(100), filter};

    }

    /**
     * Returns a file name prefix for a given timestamp document name and page num.
     *
     * @param timeStamp
     * @param docName
     * @param pageNum
     * @return
     */
    @NonNull
    public static String getFileNamePrefix(String timeStamp, String docName, int pageNum) {
        String leadingZeros = "00000";
        String page = Integer.toString(pageNum);
        int shift = page.length();
        if (shift < leadingZeros.length())
            page = leadingZeros.substring(0, leadingZeros.length() - shift) + page;

        return docName + "_" + page + '-' + timeStamp;
    }

    /**
     * Returns a time stamp that is used for the file name generation.
     *
     * @return
     */
    public static String getFileTimeStamp() {
        return DateExtensionKt.getTimeStamp(new Date());
    }

    private static ArrayList<File> getImageList(File file) {

        File[] files = getImageArray(file);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    public static File[] getImageArray(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return (file.getPath().endsWith(".jpg") || file.getPath().endsWith(".jpeg"));
//                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        if (files != null && files.length > 0)
            Arrays.sort(files);

        return files;
    }

    public static void hideKeyboard(Activity activity) {

        if (activity == null)
            return;

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
