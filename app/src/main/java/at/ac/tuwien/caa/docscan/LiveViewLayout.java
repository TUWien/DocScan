package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by fabian on 14.10.2016.
 */
public class LiveViewLayout extends FrameLayout{

    private int mFrameWidth, mFrameHeight;
    private CameraPreview.CameraPreviewCallback mCameraPreviewCallback;

//    public LiveViewLayout(Context context) {
//        super(context);
//        mCameraPreviewCallback = (CameraPreview.CameraPreviewCallback) context;
//    }

    public LiveViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraPreviewCallback = (CameraPreview.CameraPreviewCallback) context;
    }



    public void setFrameDimension(int frameWidth, int frameHeight) {

        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;

    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//
//        if (mFrameHeight == 0|| mFrameWidth == 0) {
////            setChildMeasuredDimension(width, height);
//            setMeasuredDimension(width, height);
//        } else {
//
//
//            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!
//            // Portrait mode:
//            if (width < height) {
//
//                double resizeFac = (double) width / mFrameHeight;
//                int scaledHeight = (int) Math.round(mFrameWidth * resizeFac);
//                if (scaledHeight > height)
//                    scaledHeight = height;
//                setMeasuredDimension(width, scaledHeight);
//
//            }
//            // Landscape mode:
//            else {
//                double resizeFac = (double) height / mFrameHeight;
//                int scaledWidth = (int) Math.round(mFrameWidth * resizeFac);
//                if (scaledWidth > width)
//                    scaledWidth = width;
//                setMeasuredDimension(scaledWidth, height);
//
//            }
//
//        }
//
//        // Finally tell the dependent Activity the dimension has changed:
//        mCameraPreviewCallback.onMeasuredDimensionChange(getMeasuredWidth(), getMeasuredHeight());
//    }
}
