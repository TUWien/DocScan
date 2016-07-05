package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import at.ac.tuwien.caa.docscan.cv.Patch;

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

    private static final int ORIENTATION_LANDSCAPE = 0;
    private static final int ORIENTATION_PORTRAIT = 90;
    private static final int ORIENTATION_FLIPPED_LANDSCAPE = 180;

    public OverlayView(Context context, AttributeSet attrs) {

        super(context, attrs);

    }

    public void setCameraView(CameraView cameraView) {

        mCameraView = cameraView;

    }

    public void setDrawView(DrawView drawView) {

        mDrawView = drawView;

    }

    // Converts the coordinates of the patches and passes it to the DrawView:
    public void setFocusPatches(Patch[] patches) {

        Patch patch;
        int drawViewWidth = mDrawView.getWidth();
        int drawViewHeight = mDrawView.getHeight();

        int frameWidth = mCameraView.getFrameWidth();
        int frameHeight = mCameraView.getFrameHeight();

        float drawViewPX = -1;
        float drawViewPY = -1;

        int cameraDisplayOrientation = MainActivity.getCameraDisplayOrientation();

        for (int i = 0; i < patches.length; i++) {

            patch = patches[i];

            // Check here for the orientation:
            // Note: frameWidth is always greater than frameHeight - regardless of the orientation.
            switch (cameraDisplayOrientation) {

                case ORIENTATION_LANDSCAPE:
                    drawViewPX = patch.getPX() / frameWidth * drawViewWidth;
                    drawViewPY = patch.getPY() / frameHeight * drawViewHeight;
                    break;

                case ORIENTATION_PORTRAIT:
                    drawViewPX = (frameHeight- patch.getPY()) / frameHeight * drawViewWidth;
                    drawViewPY = patch.getPX() / frameWidth * drawViewHeight;
                    break;

                case ORIENTATION_FLIPPED_LANDSCAPE:
                    drawViewPX = (frameWidth - patch.getPX()) / frameWidth * drawViewWidth;
                    drawViewPY = (frameHeight - patch.getPY()) / frameHeight * drawViewHeight;
                    break;

            }
//            drawViewPX = patch.getPX() / frameWidth * drawViewWidth;
            if (drawViewPX > drawViewWidth)
                drawViewPX = drawViewWidth;

//            drawViewPY = patch.getPY() / frameHeight * drawViewHeight;
            if (drawViewPY > drawViewHeight)
                drawViewPY = drawViewHeight;

            patch.setDrawViewPX(drawViewPX);
            patch.setDrawViewPY(drawViewPY);

        }

        mDrawView.setFocusPatches(patches);
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
