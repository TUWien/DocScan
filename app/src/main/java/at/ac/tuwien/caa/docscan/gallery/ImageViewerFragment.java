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


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ContentFrameLayout;
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
    private String mFileName;

    public static ImageViewerFragment create() {

        ImageViewerFragment fragment = new ImageViewerFragment();

        return fragment;

    }

    public ImageViewerFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mFileName = getArguments() != null ?
                getArguments().getString(getString(R.string.key_fragment_image_viewer_file_name)) : null;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_image_viewer, container, false);

        initImageView(rootView);

        return rootView;

    }

    private void initImageView(ViewGroup rootView) {

        if (mFileName != null) {

            mImageView = rootView.findViewById(R.id.image_viewer_image_view);
            mImageView.setImage(ImageSource.uri(mFileName));
            mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);

        }
//      PhotoView by Chris Bane:
//        PhotoView photoView = rootView.findViewById(R.id.page_slide_photo_view);
//        photoView.setImageURI(Uri.fromFile(mPage.getFile()));
//        photoView.setRotationBy();

    }


    public void refreshImageView() {

        mImageView.setImage(ImageSource.uri(mFileName));

    }


}
