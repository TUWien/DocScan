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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.CropViewActivity;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

public class PageSlideFragment extends Fragment {

    private Page mPage;
    private SubsamplingScaleImageView mImageView;

    /** TODO: remove this, we just need it here for debugging if the CameraActivitiy is not started before PageSlideFragment
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

//         We need this for Android 4:
        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Error while initializing OpenCV.");
        } else {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("docscan-native");
//            Log.d(TAG, "OpenCV initialized");
        }

    }


    public static PageSlideFragment create(Page page) {

        PageSlideFragment fragment = new PageSlideFragment();
//        Bundle args = new Bundle();
//        args.putInt("arg_int", pageNumber);
//        fragment.setArguments(args);
        fragment.setPage(page);

        return fragment;

    }

    public PageSlideFragment() {

    }

    public void testClick2() {

    }

    public void rotateImg90Degrees() {

        if (mPage != null && mImageView != null) {
            try {
                if (Helper.rotateExif(mPage.getFile().getAbsoluteFile())) {
                    mImageView.invalidate();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private void setPage(Page page) {
        mPage = page;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mPageNumber = getArguments().getInt("arg_int");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_page_slide, container, false);

// Use Glide for image loading:
//        GlideApp.with(this)
//                .load((mPage.getFile()).getPath())
//                .into(imageView);
// Use SubsamplingScaleImageView for image loading:

        mImageView = rootView.findViewById(R.id.page_slide_image_view);
        mImageView.setImage(ImageSource.uri(mPage.getFile().getPath()));
        mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);

//        Toolbar toolbar = rootView.findViewById(R.id.page_slide_toolbar);
//        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
//        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("ein test");
//        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        initRotateButton(rootView);
//        initCropButton(rootView);
//        initDeleteButton(rootView);


//        // status bar height
//        int statusBarHeight = 0;
//        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
//        }





        return rootView;
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



}
