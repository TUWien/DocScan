package at.ac.tuwien.caa.docscan.gallery;

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


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.CropViewActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;


public class ImageViewerFragment extends Fragment {

    private Page mPage;
//    private SubsamplingScaleImageView mImageView;
    private TouchImageView mImageView;
    private Toolbar mToolbar;
    private LinearLayout mButtonsLayout;

//    /** TODO: remove this, we just need it here for debugging if the CameraActivitiy is not started before ImageViewerFragment
//     * Static initialization of the OpenCV and docscan-native modules.
//     */
//    static {
//
////         We need this for Android 4:
//        if (!OpenCVLoader.initDebug()) {
////            Log.d(TAG, "Error while initializing OpenCV.");
//        } else {
//            System.loadLibrary("opencv_java3");
//            System.loadLibrary("docscan-native");
////            Log.d(TAG, "OpenCV initialized");
//        }
//
//    }


    public static ImageViewerFragment create(Page page) {

        ImageViewerFragment fragment = new ImageViewerFragment();
        fragment.setPage(page);

        return fragment;

    }

    public ImageViewerFragment() {

    }

    private void setPage(Page page) {
        mPage = page;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_image_viewer, container, false);

        initImageView(rootView);

//        initToolbar(rootView);

        mButtonsLayout = rootView.findViewById(R.id.page_view_buttons_layout);

//        initRotateButton(rootView);
//        initDeleteButton(rootView);
//        initCropButton(rootView);

        return rootView;

    }

    private void initImageView(ViewGroup rootView) {

        mImageView = rootView.findViewById(R.id.image_viewer_image_view);
        mImageView.setImage(ImageSource.uri(mPage.getFile().getPath()).tilingDisabled());
        mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);

//        mImageView.setClickCallBack(this);

//        mImageView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//
//
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        onDown(event);
//                        return true;
//
//                    case MotionEvent.ACTION_MOVE:
//                        if (mIsPointDragged)
//                            onMove(event);
//                        return true;
//
//                    case MotionEvent.ACTION_UP:
//                        onUp(event);
//                        return true;
//                }
//
//
//            }
//
//
//        });

//      PhotoView by Chris Bane:
//        PhotoView photoView = rootView.findViewById(R.id.page_slide_photo_view);
//        photoView.setImageURI(Uri.fromFile(mPage.getFile()));
//        photoView.setRotationBy();

    }

    private void initToolbar(ViewGroup rootView) {

        mToolbar = rootView.findViewById(R.id.image_viewer_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        String title = mPage.getFile().getName();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(title);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setPadding(0, getStatusBarHeight(), 0, 0);

        // Close the fragment if the user hits the back button in the toolbar:
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
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

    public void refreshImageView() {

        mImageView.setImage(ImageSource.uri(mPage.getFile().getPath()));

    }


    private void initRotateButton(ViewGroup rootView) {

        ImageView rotateImageView = rootView.findViewById(R.id.page_view_buttons_layout_rotate_button);
        rotateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null && mImageView != null) {
                    try {
//                        Rotate the image 90 degrees and set the image again (I did not find another way to force an update of the imageview)
                        if (Helper.rotateExif(mPage.getFile().getAbsoluteFile())) {
                            mImageView.setImage(ImageSource.uri(mPage.getFile().getPath()));
//                            Tell the gallery viewer that the file has rotated:
                            GalleryActivity.fileRotated();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    private void initCropButton(ViewGroup rootView) {

        ImageView cropImageView = rootView.findViewById(R.id.page_view_buttons_layout_crop_button);
        cropImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPage != null && mImageView != null) {
                    Intent intent = new Intent(getContext(), CropViewActivity.class);
                    CropInfo cropInfo = new CropInfo(mPage.getFile().getPath());
                    intent.putExtra(CROP_INFO_NAME, cropInfo);
                    startActivity(intent);
                }
            }
        });

    }

    private void initDeleteButton(ViewGroup rootView) {

        ImageView deleteImageView = rootView.findViewById(R.id.page_view_buttons_layout_delete_button);
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

        if (getContext() == null)
            return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.page_slide_fragment_confirm_delete_text)
                .setPositiveButton(R.string.page_slide_fragment_confirm_delete_confirm_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        mPage.getFile().delete();
//                            Tell the gallery viewer that the file was deleted:
                        GalleryActivity.fileDeleted();
                        getActivity().finish();

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


//    @Override
//    public void onSingleClick() {
//
//        if (mToolbar.getVisibility() == View.VISIBLE) {
//            mToolbar.setVisibility(View.INVISIBLE);
//            mButtonsLayout.setVisibility(View.INVISIBLE);
//        }
//        else {
//            mToolbar.setVisibility(View.VISIBLE);
//            mButtonsLayout.setVisibility(View.VISIBLE);
//        }
//
//    }

}
