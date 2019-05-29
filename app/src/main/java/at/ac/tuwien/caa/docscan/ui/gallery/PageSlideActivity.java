package at.ac.tuwien.caa.docscan.ui.gallery;

/* Based on the example provided in:
 * https://developer.android.com/training/animation/screen-slide.html
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.ImageViewerFragment;
import at.ac.tuwien.caa.docscan.gallery.PageImageView;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;
import at.ac.tuwien.caa.docscan.ui.CropViewActivity;

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_FILE_NAME;
import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.INTENT_IMAGE_PROCESS_ACTION;

public class PageSlideActivity extends AppCompatActivity implements PageImageView.SingleClickListener {

    private HackyViewPager mPager;
    private PageSlideAdapter mPagerAdapter;
    private Page mPage;
    private Toolbar mToolbar;
    private Document mDocument;
    private LinearLayout mButtonsLayout;
    private Context mContext;
    private BroadcastReceiver mMessageReceiver;

    public static final String KEY_RETAKE_IMAGE = "KEY_RETAKE_IMAGE";
    public static final String KEY_RETAKE_IDX = "KEY_RETAKE_IDX";

    private static final int PERMISSION_ROTATE = 0;
    private static final int PERMISSION_DELETE = 1;
    private static final String CLASS_NAME = "PageSlideActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_slide);

        // Instantiate a ViewPager and a PagerAdapter.
        initPager();

        String fileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));
        if (fileName == null)
            return;

//        mDocument = getDummyDocument(fileName);
        mDocument = DocumentStorage.getInstance(this).getDocument(fileName);

        mPagerAdapter = new PageSlideAdapter(getSupportFragmentManager(), mDocument);
        mPager.setAdapter(mPagerAdapter);

        initToolbar();
        initButtons();

        mContext = this;

//        Which position was selected?
        int pos = getIntent().getIntExtra(getString(R.string.key_page_position), -1);
        if (pos != -1 && mDocument.getPages() != null && mDocument.getPages().size() > pos) {
            mPager.setCurrentItem(pos);
            setToolbarTitle(pos);
            mPage = mDocument.getPages().get(pos);
        }

    }

    @Override
    public void onResume() {

        super.onResume();

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "PROGRESS_INTENT_NAME".
        mMessageReceiver = getReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_IMAGE_PROCESS_ACTION));


        if ((mPagerAdapter != null) && (mPagerAdapter.getCurrentFragment() != null))
            mPagerAdapter.getCurrentFragment().refreshImageView();

        showHideCropButton();

    }

    @Override
    public void onPause() {

        super.onPause();

        DocumentStorage.saveJSON(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        mMessageReceiver = null;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.page_slide_menu, menu);

        return true;

    }


    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode Request code
     * @param permissions Permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        boolean isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);

        switch (requestCode) {

            case PERMISSION_ROTATE:
                if (isPermissionGiven)
                    rotatePage();
                break;
            case PERMISSION_DELETE:
                if (isPermissionGiven)
                    deletePage();
                break;

        }
    }

    private BroadcastReceiver getReceiver() {

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (mPagerAdapter != null)
                    mPagerAdapter.notifyDataSetChanged();

                Log.d(CLASS_NAME, "onReceive:");
                Log.d(CLASS_NAME, "onReceive: file: " + intent.getStringExtra(INTENT_FILE_NAME));

                if (mPage != null && mPage.getFile() != null) {
                    if (mPage.getFile().getAbsolutePath().equals(intent.getStringExtra(INTENT_FILE_NAME))) {
                        Log.d(CLASS_NAME, "onReceive: passing to mPagerAdapter");
                        if (mPagerAdapter != null)
                            mPagerAdapter.getCurrentFragment().refreshImageView();
                    }
                }
            }
        };

        return receiver;

    }

//    /**
//     * Handles broadcast intents which inform about the upload progress:
//     */
//    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            Log.d(CLASS_NAME, "onReceive:");
//            if (mPage.getFile().getAbsolutePath().equals(intent.getStringExtra(INTENT_FILE_NAME))) {
//                Log.d(CLASS_NAME, "onReceive: passing to mPagerAdapter");
//                mPagerAdapter.getCurrentFragment().refreshImageView();
//            }
//
//        }
//    };

    private void initPager() {

        mPager = findViewById(R.id.slide_viewpager);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                Log.d(CLASS_NAME, "onPageSelected: " + position);
                mPage = mDocument.getPages().get(position);
                setToolbarTitle(position);

//                if ((mPagerAdapter != null) && (mPagerAdapter.getCurrentFragment() != null)) {
//                    mPagerAdapter.getCurrentFragment().checkLoadingViewStatus();
//                }

//                if ((mPagerAdapter != null) && (mPagerAdapter.getCurrentFragment() != null)) {
//                    if (mPagerAdapter.getCurrentFragment().isLoadingViewVisible() &&
//                            !ImageProcessLogger.isAwaitingPageDetection(mPage.getFile())) {
//                        mPagerAdapter.getCurrentFragment().refreshImageView();
//                        Log.d(CLASS_NAME, "onPageSelected: " + )
//                    }
//                }

                showHideCropButton();


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void showHideCropButton() {

////                Check if the file is already cropped.
        RelativeLayout l = findViewById(R.id.page_view_buttons_layout_crop_layout);
//        if (PageDetector.isCropped(mPage.getFile().getAbsolutePath()))
//            l.setVisibility(View.GONE);
//        else
            l.setVisibility(View.VISIBLE);

    }

    private void initToolbar() {

        mToolbar = findViewById(R.id.image_viewer_toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //       Take care that the mToolbar is not overlaid by the status bar:
        mToolbar.setPadding(0, getStatusBarHeight(), 0, 0);

        // Close the fragment if the user hits the back button in the toolbar:
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void setToolbarTitle(int pos) {

        if (mToolbar != null && getSupportActionBar() != null)
            getSupportActionBar().setTitle("#: " + Integer.toString(pos+1) + "/" +
                    Integer.toString(mDocument.getPages().size())+ " - " + mDocument.getTitle());
    }

    // A method to find height of the status bar
//    Based on: https://gist.github.com/hamakn/8939eb68a920a6d7a498
    private int getStatusBarHeight() {

        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;

    }

    public void retakeImage(MenuItem item) {

        final int idx;
        if (mDocument != null && mDocument.getPages() != null && mPage != null)
            idx = mDocument.getPages().indexOf(mPage);
        else
//        Nothing to do here. TODO: add an error message
            return;


        final Context context = this;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        // set dialog message
        alertDialogBuilder
                .setTitle(getString(R.string.page_slide_fragment_retake_image_dialog_title))
                .setNegativeButton(getString(R.string.page_slide_fragment_retake_image_dialog_cancel_text), null)
                .setPositiveButton(getString(R.string.page_slide_fragment_retake_image_dialog_confirm_text),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {

                        Intent intent = new Intent(context, CameraActivity.class);
//                        Define which image / page should be replaced:
                        intent.putExtra(KEY_RETAKE_IMAGE, true);
                        intent.putExtra(KEY_RETAKE_IDX, idx);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
//                        finish();

                    }
                })
                .setCancelable(true)
                .setMessage(getString(R.string.page_slide_fragment_retake_image_dialog_text));

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    private void initButtons() {

       mButtonsLayout = findViewById(R.id.page_view_buttons_layout);
    //       Take care that the mButtonsLayout is not overlaid by the navigation bar:
    //       mButtonsLayout.setPadding(0, 0, 0, getNavigationBarHeight());

    //        initGalleryButton();
        initCropButton();
        initDeleteButton();
        initRotateButton();
        initShareButton();

    }

    public void showGallery(MenuItem item) {

        startGalleryActivity();

    }

//    private void initGalleryButton() {
//
//        ImageView galleryImageView = findViewById(R.id.page_view_buttons_layout_gallery_button);
//        galleryImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startGalleryActivity();
//            }
//        });
//    }

    private void startGalleryActivity() {

        if (mDocument == null)
            return;

        String documentTitle = mDocument.getTitle();
        if (documentTitle != null) {
            Intent intent = new Intent(mContext, GalleryActivity.class);
//            This is used to prevent cycling between the GalleryActivity and the PageSlideActivity.
//            Without this flag the activities would all be added to the back stack.
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(mContext.getString(R.string.key_document_file_name), documentTitle);
            mContext.startActivity(intent);
//            Finish is necessary, because the current PageSlideActivity can be started from the
//            CameraActivity, but once we are in the GalleryActivity, we do not want to get back to
//            PageSlideActivity:
            finish();
        }

    }

    private void initRotateButton() {

        ImageView rotateImageView = findViewById(R.id.page_view_buttons_layout_rotate_button);
        rotateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if we have the permission to rotate images:
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // ask for permission:
                    ActivityCompat.requestPermissions((AppCompatActivity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_ROTATE);
                } else
                    rotatePage();
            }
        });

    }

    private void rotatePage() {
        if (mPage != null) {
//            Rotate the image 90 degrees and set the image again (I did not find another way to force an update of the imageview)
            if (Helper.rotateExif(mPage.getFile().getAbsoluteFile())) {
                mPagerAdapter.getCurrentFragment().refreshImageView();
//                Tell the gallery viewer that the file has rotated:
                GalleryActivity.fileRotated();
            }

        }
    }


    private void initShareButton() {

        ImageView shareView = findViewById(R.id.page_view_buttons_layout_share_button);
        final Context context = this;
        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {

                    Uri uri = FileProvider.getUriForFile(context,
                            "at.ac.tuwien.caa.fileprovider", mPage.getFile());
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
//                    check here if the content resolver is null
                    shareIntent.setDataAndType(uri, getContentResolver().getType(uri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.setType("image/jpg");
                    startActivity(Intent.createChooser(shareIntent, mContext.getResources().getString(R.string.page_slide_fragment_share_choose_app_text)));

                }
            }
        });

    }


    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
//
    private void initCropButton() {

        ImageView cropImageView = findViewById(R.id.page_view_buttons_layout_crop_button);
        cropImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {
                    Intent intent = new Intent(getApplicationContext(), CropViewActivity.class);
                    intent.putExtra(getString(R.string.key_crop_view_activity_file_name),
                            mPage.getFile().getAbsolutePath());
                    startActivity(intent);
                }
            }
        });

    }

    private void initDeleteButton() {

        ImageView deleteImageView = findViewById(R.id.page_view_buttons_layout_delete_button);
        deleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // ask for permission:
                        ActivityCompat.requestPermissions((AppCompatActivity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_DELETE);
                    } else
                        deletePage();
                }
//            }
        });

    }

    private void deletePage() {
        if (mPage != null)
            showDeleteConfirmationDialog();
    }

    private void showDeleteConfirmationDialog() {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.page_slide_fragment_confirm_delete_text)
                .setPositiveButton(R.string.page_slide_fragment_confirm_delete_confirm_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {

                        deleteCurrentPage();

                    }
                })
                .setNegativeButton(R.string.page_slide_fragment_confirm_delete_cancel_text, null)
                .setCancelable(true);
//                .setMessage(deleteText);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void deleteCurrentPage() {

        mDocument.getPages().remove(mPage);
        mPage.getFile().delete();
        mPage = null;

//        Tell the gallery viewer that the file was deleted:
        GalleryActivity.fileDeleted();

        int newPos = mPager.getCurrentItem()-1;

        if ((newPos < 0) && (mDocument.getPages().size() > 0))
            newPos = 0;
        else if (mDocument.getPages().size() == 0) {
            finish();
            return;
        }

        mPagerAdapter.notifyDataSetChanged();

        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(newPos);

//            onPageSelected is not called if newPos == 0. This is a known issue, see here:
//            https://stackoverflow.com/questions/11794269/onpageselected-isnt-triggered-when-calling-setcurrentitem0
        mPage = mDocument.getPages().get(newPos);
        setToolbarTitle(newPos);

    }

//    private Document getDummyDocument(String fileName) {
//
//        Document document = Helper.getDocument(fileName);
//
//        return document;
//
////        TranskribusDocumentMetaDataRequest document = new TranskribusDocumentMetaDataRequest();
////        ArrayList<File> fileList = getFileList(fileName);
////        ArrayList<Page> pages = filesToPages(fileList);
////        document.setPages(pages);
////        File file = new File(fileName);
////        document.setTitle(file.getName());
////
////        return document;
//
//    }


    @Override
    public void onSingleClick() {


//        Hide the status bar and the navigation bar:
        if (mToolbar.getVisibility() == View.VISIBLE) {
            mToolbar.setVisibility(View.INVISIBLE);
            mButtonsLayout.setVisibility(View.INVISIBLE);
        }
        else {

            mToolbar.setVisibility(View.VISIBLE);
            mButtonsLayout.setVisibility(View.VISIBLE);
        }


    }



    private class PageSlideAdapter extends FragmentStatePagerAdapter {

        private Document mDocument;
        private ImageViewerFragment mCurrentFragment;

        public PageSlideAdapter(FragmentManager fm, Document document) {

            super(fm);
            mDocument = document;

        }


        @Override
        public Fragment getItem(int position) {

            Page page = mDocument.getPages().get(position);

            Log.d(CLASS_NAME, "getItem: position: " + position);
            Log.d(CLASS_NAME, "getItem: file: " + page.getFile().toString());

            ImageViewerFragment fragment = ImageViewerFragment.create();
            Bundle args = new Bundle();
                args.putString(getString(R.string.key_fragment_image_viewer_file_name),
                        page.getFile().getAbsolutePath());
            fragment.setArguments(args);

            return fragment;

        }

        @Override
        public int getCount() {

            if (mDocument == null || mDocument.getPages() == null)
                return -1;
            else
                return mDocument.getPages().size();

        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (getCurrentFragment() != object)
                mCurrentFragment = ((ImageViewerFragment) object);

            super.setPrimaryItem(container, position, object);

        }



        public ImageViewerFragment getCurrentFragment() {

            return mCurrentFragment;
        }


    }




}
