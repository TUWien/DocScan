package at.ac.tuwien.caa.docscan.ui.intro;

import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;

public class IntroFragment extends Fragment {

    protected final static String INTRO_FRAGMENT_NUM_KEY = "INTRO_FRAGMENT_NUM_KEY";

    private Drawable mDrawable;
    private int mNum;

    public static IntroFragment newInstance(int num) {

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

            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_3, container, false);

            ImageView imageView = rootView.findViewById(R.id.intro_imageview);
            GlideApp.with(this)
                    .load(R.drawable.auto_mode)
                    .into(imageView);


        }
//        else if (mNum == 4) {
//
//            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_5, container, false);
//            final ImageView transkribusImageView = rootView.findViewById(R.id.transkribus_imageview);
//            GlideApp.with(this)
//                    .load(R.drawable.transkribus)
//                    .fitCenter()
//                    .into(transkribusImageView);
//
//            final ImageView dropboxImageView = rootView.findViewById(R.id.dropbox_imageview);
//            GlideApp.with(this)
//                    .load(R.drawable.dropbox_glyph_blue)
//                    .fitCenter()
//                    .into(dropboxImageView);
//
//        }
        else if (mNum == 4) {

            rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_5, container, false);
            final ImageView uploadImageView = rootView.findViewById(R.id.upload_imageview);
            GlideApp.with(this)
                    .load(R.drawable.upload_screenshot)
                    .fitCenter()
                    .into(uploadImageView);
            final ImageView documentImageView = rootView.findViewById(R.id.document_imageview);
            GlideApp.with(this)
                    .load(R.drawable.document_screenshot)
                    .fitCenter()
                    .into(documentImageView);
        }
        else {

            switch (mNum) {
                case 0:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_1, container, false);
//                    Make the link clickable, found this here: https://stackoverflow.com/a/2746708
                    TextView textView = rootView.findViewById(R.id.intro_1_textview);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                    break;
                case 1:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_2, container, false);
                    break;
                case 3:
                    rootView = (ViewGroup) inflater.inflate(R.layout.fragment_intro_4, container, false);
//                    Initialize the trigger flash checkbox. Note that the button is checkbox can be
//                    checked in case the user starts the intro from the AboutActivity

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                            getContext());
                    boolean check = sharedPref.getBoolean(getString(R.string.key_flash_series_mode),
                            false);
                    CheckBox checkBox = rootView.findViewById(R.id.intro_4_trigger_flash_checkbox);
                    checkBox.setChecked(check);
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
