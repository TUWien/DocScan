package at.ac.tuwien.caa.docscan;

import android.graphics.PointF;
import android.view.Surface;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 22.07.2016.
 */
public class CVResult {

    private static final int ORIENTATION_LANDSCAPE = 0;
    private static final int ORIENTATION_PORTRAIT = 90;
    private static final int ORIENTATION_FLIPPED_LANDSCAPE = 180;
    private static final int ORIENTATION_FLIPPED_PORTRAIT = 270;

    private DkPolyRect[] mDKPolyRects;
    private Patch[] mPatches;
    private int mViewWidth, mViewHeight;
    private int mFrameHeight, mFrameWidth;
    private int mDisplayRotation;

    public CVResult() {


    }

    public void setDKPolyRects(DkPolyRect[] dkPolyRects) {

        mDKPolyRects = dkPolyRects;

    }

    public DkPolyRect[] getDKPolyRects() {

        return mDKPolyRects;

    }

    public void setPatches(Patch[] patches) {

        mPatches = patches;

        updatePatches();

    }

    public Patch[] getPatches() {

        return mPatches;

    }

    public void setViewDimensions(int width, int height) {

        mViewWidth = width;
        mViewHeight = height;

    }


    public void setFrameDimensions(int width, int height) {

        mFrameWidth = width;
        mFrameHeight = height;

    }

    public void setDisplayRotation(int displayRotation) {

        mDisplayRotation = displayRotation;

    }

    private void updatePatches() {


        Patch patch;
        PointF screenPoint, framePoint;

        for (int i = 0; i < mPatches.length; i++) {

            patch = mPatches[i];
            framePoint = new PointF(patch.getPX(), patch.getPY());
            screenPoint = getScreenCoordinates(framePoint, mFrameWidth, mFrameHeight, mViewWidth, mViewHeight, mDisplayRotation);

            patch.setDrawViewPX(screenPoint.x);
            patch.setDrawViewPY(screenPoint.y);


        }


    }


    private PointF getScreenCoordinates(PointF framePos, int frameWidth, int frameHeight, int drawWidth, int drawHeight, int orientation) {


        float drawViewPX = -1;
        float drawViewPY = -1;

        // TODO: check out coordination conversation with tablets

        switch (orientation) {

            case Surface.ROTATION_0: // Portrait mode
                drawViewPX = (frameHeight - framePos.y) / frameHeight * drawWidth;
                drawViewPY = framePos.x / frameWidth * drawHeight;
                break;

//            drawViewPX = framePos.x / frameWidth * drawWidth;
//                drawViewPY = framePos.y / frameHeight * drawHeight;
//                break;

//                drawViewPX = (frameHeight - framePos.y) / frameHeight * drawWidth;
//                drawViewPY = framePos.x / frameWidth * drawHeight;
//                break;
            case Surface.ROTATION_90: // Landscape mode
                drawViewPX = framePos.x / frameWidth * drawWidth;
                drawViewPY = framePos.y / frameHeight * drawHeight;
                break;

            case Surface.ROTATION_270: // Landscape mode flipped
                drawViewPX = (frameWidth - framePos.x) / frameWidth * drawWidth;
                drawViewPY = (frameHeight - framePos.y) / frameHeight * drawHeight;
                break;


        }

        if (drawViewPX > drawWidth)
            drawViewPX = drawWidth;

        if (drawViewPY > drawHeight)
            drawViewPY = drawHeight;

        PointF point = new PointF(drawViewPX, drawViewPY);

        return point;

    }

}
