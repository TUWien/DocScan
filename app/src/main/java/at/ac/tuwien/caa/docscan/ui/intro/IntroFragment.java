package at.ac.tuwien.caa.docscan.ui.intro;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;

public class IntroFragment extends Fragment {

    protected final static String INTRO_FRAGMENT_NUM_KEY = "INTRO_FRAGMENT_NUM_KEY";

    private Drawable mDrawable;
    private int mNum;

    protected static IntroFragment newInstance(int num) {

        IntroFragment fragment = new IntroFragment();

        Bundle args = new Bundle();
        args.putInt(INTRO_FRAGMENT_NUM_KEY, num);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mNum = getArguments() != null ?
                getArguments().getInt(INTRO_FRAGMENT_NUM_KEY) : null;

    }


    public Drawable getDrawable() {

        return mDrawable;

    }

    @Override
    public void onResume() {

        super.onResume();

        if (mDrawable != null && mDrawable instanceof Animatable)
            ((Animatable) mDrawable).start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = null;

        if (mNum == 2) {

            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_4, container, false);

            ImageView imageView = rootView.findViewById(R.id.intro_imageview);

            GlideApp.with(this)
                    .load(R.drawable.animated_series)
                    .into(imageView);



            

//            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_4, container, false);

//            RelativeLayout imageViewContainer = rootView.findViewById(R.id.container_layout);
//            ViewGroup.LayoutParams params = imageViewContainer.getLayoutParams();
//            float scaleFactor = 400.f / 755.f; // 755 is the sum of the widths of both imageviews
//            params.height = Math.round(params.width * scaleFactor);
//            imageViewContainer.setLayoutParams(params);

//            ImageView imageView = rootView.findViewById(R.id.intro_imageview_left);
//            GlideApp.with(this)
//                    .load(R.drawable.left)
//                    .into(imageView);
//
////            ImageView imageViewRight = rootView.findViewById(R.id.intro_imageview_right);
////            imageViewRight.setImageResource(R.drawable.series_animation);
////            // Get the background, which has been compiled to an AnimationDrawable object.
////            AnimationDrawable frameAnimation = (AnimationDrawable) imageViewRight.getDrawable();
//////            imageViewRight.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//////            imageViewRight.setAdjustViewBounds(true);
////
////            // Start the animation (looped playback by default).
////            frameAnimation.start();
//
//            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//            imageView.setAdjustViewBounds(true);
        }
        else {

            switch (mNum) {
                case 0:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_1, container, false);
                    break;
                case 1:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_2, container, false);
                    break;
                case 3:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_3, container, false);
                    break;
            }

            if (rootView != null) {

                ImageView imageView = rootView.findViewById(R.id.intro_imageview);
                mDrawable = imageView.getDrawable();
                //            if (drawable != null && drawable instanceof Animatable)
                //                ((Animatable) drawable).start();

            }
        }

        return rootView;

    }

}
