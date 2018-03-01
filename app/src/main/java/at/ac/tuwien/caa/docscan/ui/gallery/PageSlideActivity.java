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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.ImageViewerFragment;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Page;

public class PageSlideActivity extends AppCompatActivity {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_slide);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.slide_viewpager);

//        Create a dummy document based on the intent: TODO: use a real document here
        String fileName = getIntent().getStringExtra("DOCUMENT_FILE_NAME");
        if (fileName == null)
            fileName = "/storage/emulated/0/Pictures/DocScan/default";
        Document document = getDummyDocument(fileName);

        mPagerAdapter = new PageSlideAdapter(getSupportFragmentManager(), document);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                invalidateOptionsMenu();
            }
        });

        int position = getIntent().getIntExtra("PAGE_POSITION", -1);
        if (position != -1)
            mPager.setCurrentItem(position);
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


//    TODO: temporary helper methods copied from BaseDocumentAdapter. Replace them.

    private ArrayList<File> getFileList(String dir) {

        File[] files = getFiles(new File(dir));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    private File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }


    private Document getDummyDocument(String fileName) {

        Document document = new Document();
        ArrayList<File> fileList = getFileList(fileName);
        ArrayList<Page> pages = filesToPages(fileList);
        document.setPages(pages);
        File file = new File(fileName);
        document.setTitle(file.getName());

        return document;

    }

    private class PageSlideAdapter extends FragmentStatePagerAdapter {

        private Document mDocument;


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
    }




}
