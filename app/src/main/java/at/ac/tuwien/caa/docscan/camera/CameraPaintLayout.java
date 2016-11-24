package at.ac.tuwien.caa.docscan.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by fabian on 20.10.2016.
 */
public class CameraPaintLayout extends FrameLayout {

    private int mFrameWidth, mFrameHeight;
    private CameraPreview.CameraPreviewCallback mCameraPreviewCallback;

    public CameraPaintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraPreviewCallback = (CameraPreview.CameraPreviewCallback) context;
    }

    public void setFrameDimensions(int frameWidth, int frameHeight) {

        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mFrameHeight == 0|| mFrameWidth == 0) {
//            setChildMeasuredDimension(width, height);
            setMeasuredDimension(width, height);
        } else {
            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!

            // Portrait mode:
            if (width < height) {

                double resizeFac = (double) width / mFrameHeight;
                int scaledHeight = (int) Math.round(mFrameWidth * resizeFac);
                if (scaledHeight > height)
                    scaledHeight = height;
                setMeasuredDimension(width, scaledHeight);
            }

            // Landscape mode:
            else {

                double resizeFac = (double) height / mFrameHeight;
                int scaledWidth = (int) Math.round(mFrameWidth * resizeFac);
                if (scaledWidth > width)
                    scaledWidth = width;
                setMeasuredDimension(scaledWidth, height);
            }

        }

//        final int count = getChildCount();
//        for (int i = 0; i < count; i++) {
//            final View v = getChildAt(i);
//
//            v.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
//                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
//                    getMeasuredHeight(), MeasureSpec.EXACTLY));
//        }

        mCameraPreviewCallback.onMeasuredDimensionChange(getMeasuredWidth(), getMeasuredHeight());

    }


}
