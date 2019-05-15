package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.Mapper;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;

/**
 * Created by fabian on 24.11.2017.
 */

public class MapViewActivity  extends BaseNoNavigationActivity {

    public static final String KEY_MAP_VIEW_ACTIVITY_FINISHED = "KEY_MAP_VIEW_ACTIVITY_FINISHED";

    private static final String TEMP_IMG_PREFIX = "TEMP_";

    private SubsamplingScaleImageView mImageView;
    private MenuItem mSaveMenuItem;
    private String mFileName;
    private boolean mIsMapTaskRunning;

//    /**
//     * Static initialization of the OpenCV and docscan-native modules.
//     */
//    static {
//
////        Log.d(CLASS_NAME, "initializing OpenCV");
//
////         We need this for Android 4:
//        if (!OpenCVLoader.initDebug()) {
////            Log.d(CLASS_NAME, "Error while initializing OpenCV.");
//        } else {
//
//            System.loadLibrary("opencv_java3");
//            System.loadLibrary("docscan-native");
//
////            Log.d(CLASS_NAME, "OpenCV initialized");
//        }
//
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_crop_view);

        super.initToolbarTitle(R.string.map_crop_view_title);

        mImageView = findViewById(R.id.map_view);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mFileName = b.getString(getString(R.string.key_crop_view_activity_file_name), null);

            if (mFileName != null)
                new MapTask().execute(mFileName);

        }

    }


    @Override
    public void onDestroy() {

        super.onDestroy();

//        Delete the temp file:
        if (mFileName != null) {
            File file = getTempFileName(mFileName);
            file.delete();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);

        mSaveMenuItem = menu.findItem(R.id.map_menu_save_item);
        if (mIsMapTaskRunning)
            mSaveMenuItem.setVisible(false);

        return true;

    }

    public void saveImage(MenuItem item) {

//        showOverwriteImageAlert();
        replaceImage();

    }

    private void replaceImage() {
        //        Overwrite the original file with the temp file:
        if (mFileName != null) {
            File tempFile = getTempFileName(mFileName);
            File newFile = new File(mFileName);
            try {

//                    Store the exif data of the original file:
                ExifInterface exif = new ExifInterface(mFileName);
//                    Save the temporary file on the external storage:
                copyFile(tempFile, newFile);
//                    Save the exif data of the original image to the new image:
                Helper.saveExif(exif, newFile.getAbsolutePath());
//                    Save the new image as being cropped:
                PageDetector.saveAsCropped(newFile.getAbsolutePath());
                sendNewImageBroadcast(newFile);

                GalleryActivity.fileCropped();

//                    Start the CropViewActivity and tell it to immediately close itself:
                Intent intent = new Intent(this, CropViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(KEY_MAP_VIEW_ACTIVITY_FINISHED, true);
                startActivity(intent);

//                    Close the activity:
                finish();

            } catch (IOException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }


        }
    }

    /**
     * Moves the temporary file from the internal to the external storage and overwrites the
     * original image.
     * @param src
     * @param dst
     * @throws IOException
     */
    private static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
        }
    }

//    private void showOverwriteImageAlert() {
//
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//
//        // set dialog message
//        alertDialogBuilder
//                .setTitle(R.string.map_crop_view_overwrite_title)
//                .setPositiveButton(R.string.dialog_ok_text, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        replaceImage();
//                    }
//                })
//                .setNegativeButton(R.string.dialog_cancel_text, null)
//                .setCancelable(true)
//                .setMessage(R.string.map_crop_view_overwrite_text);
//
//        AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
//
//    }



    private void showNoTransformationAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.map_crop_view_no_transformation_title)
                .setPositiveButton("OK", null)
                .setMessage(R.string.map_crop_view_no_transformation_text);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    /**
     * Constructs a new temporary file the in the internal storage directory of the app.
     * @param fileName
     * @return
     */
    @NonNull
    private File getTempFileName(String fileName) {

        File file = new File(fileName);
        String newFileName = TEMP_IMG_PREFIX + file.getName();
        return new File(getFilesDir(), newFileName);

    }

    /**
     * Informs DocScan and other (system) apps that the image has been changed.
     * @param file
     */
    private void sendNewImageBroadcast(File file) {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);

//                Send the broadcast:
        sendBroadcast(mediaScanIntent);

    }

    private class MapTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

            mIsMapTaskRunning = true;

            if (mSaveMenuItem != null)
                mSaveMenuItem.setVisible(false);

        }

        @Override
        protected Boolean doInBackground(String... strings) {

            String fileName = strings[0];

            ArrayList<PointF> points = PageDetector.getNormedCropPoints(fileName).getPoints();

//            Save the file: We still need the original image at this point, so save it as a temp file:
            File newFile = getTempFileName(fileName);
            return Mapper.mapImage(fileName, newFile.getAbsolutePath(), points);

        }

        @Override
        protected void onPostExecute(Boolean isSaved) {

            if (isSaved) {
                if ((mImageView != null) && (mFileName != null)) {
                    File file = getTempFileName(mFileName);
                    mImageView.setImage(ImageSource.uri(file.getAbsolutePath()));
                    sendNewImageBroadcast(file);
                }
            }
            else {
                showNoTransformationAlert();
            }

            hideLoadingLayout();

            mIsMapTaskRunning = false;

        }


        private void hideLoadingLayout() {

            if (mImageView != null)
                mImageView.setVisibility(View.VISIBLE);
            if (mSaveMenuItem != null)
                mSaveMenuItem.setVisible(true);

            View loadingLayout = findViewById(R.id.map_loading_layout);
            loadingLayout.setVisibility(View.INVISIBLE);

        }
    }

}
