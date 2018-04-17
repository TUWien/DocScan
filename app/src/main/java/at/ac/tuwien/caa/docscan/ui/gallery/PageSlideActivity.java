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

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.davemorrissey.labs.subscaleview.ImageSource;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.gallery.ImageViewerFragment;
import at.ac.tuwien.caa.docscan.gallery.TouchImageView;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.CropViewActivity;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

public class PageSlideActivity extends AppCompatActivity implements TouchImageView.SingleClickListener {

    private ViewPager mPager;
    private PageSlideAdapter mPagerAdapter;
    private Page mPage;
    private Toolbar mToolbar;
    private Document mDocument;
    private LinearLayout mButtonsLayout;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_slide);

        // Instantiate a ViewPager and a PagerAdapter.
        initPager();

        String fileName = getIntent().getStringExtra(getString(R.string.key_document_file_name));
        if (fileName == null)
            return;

        mDocument = getDummyDocument(fileName);

        mPagerAdapter = new PageSlideAdapter(getSupportFragmentManager(), mDocument);
        mPager.setAdapter(mPagerAdapter);

        initToolbar();
        initButtons();

        mButtonsLayout = findViewById(R.id.page_view_buttons_layout);

        mContext = this;

//        Which position was selected?
        int pos = getIntent().getIntExtra(getString(R.string.key_page_position), -1);
        if (pos != -1 && mDocument.getPages() != null && mDocument.getPages().size() > pos) {
            mPager.setCurrentItem(pos);
            setToolbarTitle(pos);
            mPage = mDocument.getPages().get(pos);
        }


    }

    private void initPager() {
        mPager = findViewById(R.id.slide_viewpager);


        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                mPage = mDocument.getPages().get(position);
                setToolbarTitle(position);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initToolbar() {

        mToolbar = findViewById(R.id.image_viewer_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        if (mToolbar != null) {
            getSupportActionBar().setTitle("#: " + Integer.toString(pos+1) + "/" +
                    Integer.toString(mDocument.getPages().size())+ " - " + mDocument.getTitle());
        }
    }

    // A method to find height of the status bar
    public int getStatusBarHeight() {

        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;

    }


    private void initButtons() {

//        initCropButton();
        initDeleteButton();
        initRotateButton();
        initShareButton();

    }

    private void initRotateButton() {

        ImageView rotateImageView = findViewById(R.id.page_view_buttons_layout_rotate_button);
        rotateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {
                    try {
//                        Rotate the image 90 degrees and set the image again (I did not find another way to force an update of the imageview)
                        if (Helper.rotateExif(mPage.getFile().getAbsoluteFile())) {
                            mPagerAdapter.getCurrentFragment().refreshImageView();
//                              Tell the gallery viewer that the file has rotated:
                            GalleryActivity.fileRotated();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    private void initShareButton() {

        ImageView shareView = findViewById(R.id.page_view_buttons_layout_share_button);
        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {

                    Uri uri = getImageContentUri(getApplicationContext(), mPage.getFile());

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                    shareIntent.setDataAndType(uri, getContentResolver().getType(uri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.setType("image/jpg");
                    startActivity(Intent.createChooser(shareIntent, "Choose an app"));

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
//    private void initCropButton() {
//
//        ImageView cropImageView = findViewById(R.id.page_view_buttons_layout_crop_button);
//        cropImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mPage != null) {
//                    Intent intent = new Intent(getApplicationContext(), CropViewActivity.class);
//                    CropInfo cropInfo = new CropInfo(mPage.getFile().getPath());
//                    intent.putExtra(CROP_INFO_NAME, cropInfo);
//                    startActivity(intent);
//                }
//            }
//        });
//
//    }

    private void initDeleteButton() {

        ImageView deleteImageView = findViewById(R.id.page_view_buttons_layout_delete_button);
        deleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {
                    showDeleteConfirmationDialog();
                }
            }
        });

    }

    private void showDeleteConfirmationDialog() {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.page_slide_fragment_confirm_delete_text)
                .setPositiveButton(R.string.page_slide_fragment_confirm_delete_confirm_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        mPage.getFile().delete();
//                            Tell the gallery viewer that the file was deleted:
                        GalleryActivity.fileDeleted();
                        finish();

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

    private Document getDummyDocument(String fileName) {

        Document document = Helper.getDocument(fileName);

        return document;

//        DocumentMetaData document = new DocumentMetaData();
//        ArrayList<File> fileList = getFileList(fileName);
//        ArrayList<Page> pages = filesToPages(fileList);
//        document.setPages(pages);
//        File file = new File(fileName);
//        document.setTitle(file.getName());
//
//        return document;

    }

    @Override
    public void onSingleClick() {

        if (mToolbar.getVisibility() == View.VISIBLE) {
            mToolbar.setVisibility(View.INVISIBLE);
            mButtonsLayout.setVisibility(View.INVISIBLE);

            View decorView = getWindow().getDecorView();
// Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.
//            ActionBar actionBar = getActionBar();
//            actionBar.hide();

        }
        else {

            View decorView = getWindow().getDecorView();
// Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);


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

//            Bundle args = new Bundle();
//            args.putInt("num", num);
//            f.setArguments(args);

//            ImageViewerFragment fragment = ImageViewerFragment.create(page, mContext);
            ImageViewerFragment fragment = ImageViewerFragment.create();
            Bundle args = new Bundle();
                args.putString(getString(R.string.key_fragment_image_viewer_file_name),
                        page.getFile().getAbsolutePath());
            fragment.setArguments(args);

//            return ImageViewerFragment.create(page, mContext);
            return fragment;

        }

        @Override
        public int getCount() {

            if (mDocument == null)
                return -1;
            else
                return mDocument.getPages().size();

        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = ((ImageViewerFragment) object);
            }
            super.setPrimaryItem(container, position, object);
        }



        public ImageViewerFragment getCurrentFragment() {
            return mCurrentFragment;
        }


    }




}
