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


import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;


public class ImageViewerFragment extends Fragment {

    private Page mPage;
//    private SubsamplingScaleImageView mImageView;
    private static final String CLASS_NAME = "ImageViewerFragment";
    private PageImageView mImageView;
    private String mFileName;
    private View mLoadingView;

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

        mImageView = rootView.findViewById(R.id.image_viewer_image_view);
        mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);

        mLoadingView = rootView.findViewById(R.id.image_viewer_progress_layout);

        refreshImageView();

        return rootView;

    }

    @Override
    public void onResume() {

        super.onResume();


    }

    public boolean isLoadingViewVisible() {

        return mLoadingView.getVisibility() == View.VISIBLE;

    }

    public void checkLoadingViewStatus() {

        if (mFileName == null)
            return;

        if (ImageProcessLogger.isAwaitingPageDetection(new File(mFileName))) {
            Log.d(CLASS_NAME, "checkLoadingViewStatus: cropping NOT done: " + mFileName);
            if (mLoadingView != null)
                mLoadingView.setVisibility(View.VISIBLE);
        }
        else {
            Log.d(CLASS_NAME, "checkLoadingViewStatus: cropping done: " + mFileName);
            if (mLoadingView != null)
            if (mLoadingView != null)
                mLoadingView.setVisibility(View.INVISIBLE);
        }

    }

    public PageImageView getImageView() {
        return mImageView;
    }

    public void refreshImageView() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mImageView.setTransitionName(new File(mFileName).getAbsolutePath());

        mImageView.setImage(ImageSource.uri(mFileName));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mFileName, options);

        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        File file = new File(mFileName);

        try {
            int orientation = Helper.getExifOrientation(file);
            if (orientation != -1) {
                int angle = Helper.getAngleFromExif(orientation);
                Log.d(CLASS_NAME, "refreshImageView: angle: " + angle);
                if (angle == 0 || angle == 180)  {
                    imageHeight = options.outWidth;
                    imageWidth = options.outHeight;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(CLASS_NAME, "refreshImageView: w: " + imageWidth + " h: " + imageHeight);

        if (ImageProcessLogger.isAwaitingPageDetection(file)) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        else {
            mLoadingView.setVisibility(View.INVISIBLE);

            if (!PageDetector.isCropped(mFileName)) {

                PageDetector.PageFocusResult result =
                        PageDetector.getScaledCropPoints(mFileName, imageHeight, imageWidth);
//                ArrayList<PointF> points =
//                        PageDetector.getScaledCropPoints(mFileName, imageHeight, imageWidth).getPoints();
                if (result != null)
                    mImageView.setPoints(result.getPoints(), result.isFocused());

//                ArrayList<PointF> outerPoints = PageDetector.getParallelPoints(points, mFileName);
//                mImageView.setPoints(points, outerPoints);
            }
            else
//            We do this because the image might have been cropped in the meantime:
                mImageView.resetPoints();
        }


    }


}
