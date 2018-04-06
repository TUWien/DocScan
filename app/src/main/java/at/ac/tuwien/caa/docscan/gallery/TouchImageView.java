package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

/**
 * Created by fabian on 4/4/2018.
 */

public class TouchImageView extends SubsamplingScaleImageView {

    private SingleClickListener mClickCallBack;

    public TouchImageView(Context context, AttributeSet attr) {
        super(context, attr);

        mClickCallBack = (SingleClickListener) context;

    }

    public void setClickCallBack(SingleClickListener listener) {

        mClickCallBack = listener;

    }

    public TouchImageView(Context context) {
        super(context);

        mClickCallBack = (SingleClickListener) context;
    }

    @Override
    public boolean performClick () {

        if (mClickCallBack != null) {
            mClickCallBack.onSingleClick();
        }

        return mClickCallBack != null;

    }

    public interface SingleClickListener {

        void onSingleClick();

    }

}
