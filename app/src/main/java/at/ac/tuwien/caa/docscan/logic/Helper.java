package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;

/**
 * Created by fabian on 26.09.2017.
 */

public class Helper {


    /**
     * Start the CameraActivity and remove everything from the back stack.
     * @param context
     */
    public static void startCameraActivity(Context context) {

        Intent intent = new Intent(context, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);

    }

    /**
     * Returns the root path to the directory in which the images are saved.
     *
     * @param appName name of the app, this is used for gathering the directory string.
     * @return the path where the images are stored.
     */
    public static File getMediaStorageUserSubDir(String appName) {

        File mediaStorageDir = getMediaStorageDir(appName);
        File subDir = mediaStorageDir;
        if (mediaStorageDir != null)
            if (User.getInstance().getDocumentName() != null) {
                subDir = new File(mediaStorageDir, User.getInstance().getDocumentName());
                // Check if the directory is existing:
                if (!subDir.exists())
                    subDir.mkdir();
            }

        return subDir;

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


    public static boolean rotateExif(File outFile) throws IOException {

        String newOrientation = null;

        final ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
        if (exif != null) {

            // Save the orientation of the image:
//            int orientation = getExifOrientation();
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            switch (orientation) {
                case "1":
                    newOrientation = "6"; // 90 degrees
                    break;
                case "6":
                    newOrientation = "3"; // 180 degrees
                    break;
                case "3":
                    newOrientation = "8"; // 270 degrees
                    break;
                case "8":
                    newOrientation = "1"; // 0 degrees
                    break;
                default:
            }

            if (newOrientation != null)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation);

            exif.saveAttributes();
        }

        return newOrientation != null;
    }



}
