package at.ac.tuwien.caa.docscan.logic;

import android.os.Environment;

import java.io.File;

import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 26.09.2017.
 */

public class Helper {

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
            if (User.getInstance().getDocumentName() != null)
                subDir = new File(mediaStorageDir, User.getInstance().getDocumentName());

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
}
