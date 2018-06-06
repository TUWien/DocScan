package at.ac.tuwien.caa.docscan.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.threads.crop.PageDetector;
import at.ac.tuwien.caa.docscan.camera.threads.crop.Mapper;

/**
 * Created by fabian on 24.11.2017.
 */

public class MapViewActivity  extends BaseNoNavigationActivity {

    private static final String TEMP_IMG_PREFIX = "TEMP_";

    private SubsamplingScaleImageView mImageView;
    private String mFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_crop_view);

        super.initToolbarTitle(R.string.map_crop_view_title);

        mImageView = findViewById(R.id.map_view);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            mFileName = b.getString(getString(R.string.key_crop_view_activity_file_name), null);

            if (mFileName != null) {
                new MapTask().execute(mFileName);
//                ArrayList<PointF> points = PageDetector.getNormedCropPoints(mFileName);
//                mapImage(points);
//                mCropView.setPoints(points);
            }
        }



//        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
//        if (cropInfo.getPoints() == null)
//            initCropInfo(cropInfo);
//        else
//            useCropInfo(cropInfo);

//        ImageButton okButton = findViewById(R.id.confirm_map_crop_view_button);
//        okButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), CropViewActivity.class);
////                CropInfo r = new CropInfo(cropPoints, mFileName);
//                intent.putExtra(CROP_INFO_NAME, mCropInfo);
//                startActivity(intent);
//            }
//        });
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

//        Delete the temp file:
        if (mFileName != null) {
            File file = getTempFileName(mFileName);
            if (file != null) {
                file.delete();
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);

        return true;

    }

    public void saveImage(MenuItem item) {

        showOverwriteImageAlert();

    }

    private void replaceImage() {
        //        Overwrite the original file with the temp file:
        if (mFileName != null) {
            File file = getTempFileName(mFileName);
            if (file != null) {
                file.renameTo(new File(mFileName));
            }
        }
    }

    private void showOverwriteImageAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.map_crop_view_overwrite_title)
                .setPositiveButton(R.string.dialog_ok_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        replaceImage();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel_text, null)
                .setCancelable(true)
                .setMessage(R.string.map_crop_view_overwrite_text);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }



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

    @NonNull
    private File getTempFileName(String fileName) {
        File file = new File(fileName);
        String newFileName = TEMP_IMG_PREFIX + file.getName();
        return new File(file.getParent(), newFileName);
    }

    private class MapTask extends AsyncTask<String, Void, Boolean> {


        @Override
        protected Boolean doInBackground(String... strings) {
            String fileName = strings[0];

            ArrayList<PointF> points = PageDetector.getNormedCropPoints(fileName);

//            Save the file: We still need the original image at this point, so save it as a temp file:
            File newFile = getTempFileName(fileName);
            return Mapper.mapImage(fileName, newFile.getAbsolutePath(), points);

        }


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

        protected void hideLoadingLayout() {

            mImageView.setVisibility(View.VISIBLE);
            View loadingLayout = findViewById(R.id.map_loading_layout);
            loadingLayout.setVisibility(View.INVISIBLE);

        }
    }

}
