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


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Page;

public class PageSlideFragment extends Fragment {

    private Page mPage;

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

        // Set the title view to show the page number.
        ((TextView) rootView.findViewById(android.R.id.text1)).setText("bla");

        return rootView;
    }



}
