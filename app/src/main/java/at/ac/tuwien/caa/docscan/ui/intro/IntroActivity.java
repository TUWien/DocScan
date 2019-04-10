package at.ac.tuwien.caa.docscan.ui.intro;

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import at.ac.tuwien.caa.docscan.R;

//import com.google.android.material.tabs.TabLayout;
//import android.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//import androidx.viewpager.widget.ViewPager;
//import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    private static int[] mLayouts = new int[] {R.layout.fragment_intro_1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        final PageSlideAdapter pagerAdapter = new PageSlideAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.intro_viewpager);
//        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//
//                if (position == 3) {
//                    pagerAdapter.getCurrentFragment().animateImageView();
//                }
//
//
//
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });

        pager.setAdapter(pagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(pager, true);

    }

    private class PageSlideAdapter extends FragmentPagerAdapter {

//        private IntroFragment mCurrentFragment;

        public PageSlideAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return IntroFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 4;
        }

//        @Override
//        public void setPrimaryItem(ViewGroup container, int position, Object object) {
//            if (getCurrentFragment() != object)
//                mCurrentFragment = ((IntroFragment) object);
//
//            super.setPrimaryItem(container, position, object);
//
//        }
//        public IntroFragment getCurrentFragment() {
//
//            return mCurrentFragment;
//        }

    }

}
