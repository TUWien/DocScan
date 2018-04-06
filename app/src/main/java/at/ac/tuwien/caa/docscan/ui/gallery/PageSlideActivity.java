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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_slide);

        // Instantiate a ViewPager and a PagerAdapter.
        initPager();

        String fileName = getIntent().getStringExtra("DOCUMENT_FILE_NAME");
        if (fileName == null)
            return;

        mDocument = getDummyDocument(fileName);

        mPagerAdapter = new PageSlideAdapter(getSupportFragmentManager(), mDocument);
        mPager.setAdapter(mPagerAdapter);

        initToolbar();
        initButtons();

        mButtonsLayout = findViewById(R.id.page_view_buttons_layout);


        int position = getIntent().getIntExtra("PAGE_POSITION", -1);
        if (position != -1)
            mPager.setCurrentItem(position);

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

//                mPager.setCurrentItem(mPager.getCurrentItem());
//          TODO: uncomment later:
//                String title = mPage.getFile().getName();
//                getSupportActionBar().setTitle(title);

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

        initCropButton();
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

//        TODO: not working

        ImageView shareView = findViewById(R.id.page_view_buttons_layout_share_button);
        shareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {

//                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), mPage.getFile());
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), "at.ac.tuwien.caa.fileprovider", mPage.getFile());

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, mPage.getFile().getAbsolutePath());
                    shareIntent.setType("image/jpeg");
                    startActivity(Intent.createChooser(shareIntent, "asdf"));

                }
            }
        });

    }


    private void initCropButton() {

        ImageView cropImageView = findViewById(R.id.page_view_buttons_layout_crop_button);
        cropImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null) {
                    Intent intent = new Intent(getApplicationContext(), CropViewActivity.class);
                    CropInfo cropInfo = new CropInfo(mPage.getFile().getPath());
                    intent.putExtra(CROP_INFO_NAME, cropInfo);
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


//
//    private ArrayList<File> getFileList(String dir) {
//
//        File[] files = getFiles(new File(dir));
//
//        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));
//
//        return fileList;
//
//    }
//
//    private ArrayList<Page> filesToPages(ArrayList<File> files) {
//
//        ArrayList<Page> pages = new ArrayList<>(files.size());
//
//        for (File file : files) {
//            pages.add(new Page(file));
//        }
//
//        return pages;
//
//    }
//
//    private File[] getFiles(File dir) {
//
//        FileFilter filesFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return !file.isDirectory();
//            }
//        };
//        File[] files = dir.listFiles(filesFilter);
//        Arrays.sort(files);
//
//        return files;
//    }


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

            return ImageViewerFragment.create(page);

        }

        @Override
        public int getCount() {

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
