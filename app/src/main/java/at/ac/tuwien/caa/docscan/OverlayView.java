package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by fabian on 04.07.2016.
 */
public class OverlayView extends FrameLayout {


    // This interface is used to call the setMeasuredDimension methods of the child views. The
    // setMeasured methods is private.
    interface SizeUpdate {
        void setMeasuredSize(int width, int height);
    }

    private CameraView mCameraView;
    private DrawView mDrawView;

    public OverlayView(Context context, AttributeSet attrs) {

        super(context, attrs);

    }

    public void setCameraView(CameraView cameraView) {

        mCameraView = cameraView;

    }

    public void setDrawView(DrawView drawView) {

        mDrawView = drawView;

    }

    // Scales the camera view so that the preview has original width to height ratio:
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int frameWidth = mCameraView.getFrameWidth();
        int frameHeight = mCameraView.getFrameHeight();

        if (0 == frameHeight || 0 == frameWidth) {
            setChildMeasuredDimension(width, height);
//            setMeasuredDimension(width, height);
        } else {


            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!
            // Portrait mode:
            if (width < height) {
                double resizeFac = (double) width / frameHeight;
                int scaledHeight = (int) Math.round(frameWidth * resizeFac);
                if (scaledHeight > height)
                    scaledHeight = height;
//                setMeasuredDimension(width, scaledHeight);
                setMeasuredDimension(width, scaledHeight);
            }
            // Landscape mode:
            else {
                double resizeFac = (double) height / frameHeight;
                int scaledWidth = (int) Math.round(frameWidth * resizeFac);
                if (scaledWidth > width)
                    scaledWidth = width;
//                setMeasuredDimension(scaledWidth, height);
                setChildMeasuredDimension(scaledWidth, height);
            }

        }

    }

    private void setChildMeasuredDimension(int width, int height) {

        setMeasuredDimension(width, height);

        // The size of the children should be the same as the size of their parent:
        if (mCameraView != null)
            mCameraView.setMeasuredSize(width, height);
        if (mDrawView != null)
            mDrawView.setMeasuredSize(width, height);


    }



    }
