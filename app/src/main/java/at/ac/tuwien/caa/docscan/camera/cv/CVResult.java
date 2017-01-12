/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   22. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan.camera.cv;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;

/**
 * Class responsible for holding the results of the page segmentation and focus measurement tasks.
 * The coordinates of the resulting objects are mapped by this class from frame to screen coordinates.
 * Note that the drawing thread in the PaintView is waiting for updates of this class, so take care
 * to call notify if the class receives an update.
 */
public class CVResult {

    // TODO: use an enum instead:

    // Take care this must be ordered!
    public static final int DOCUMENT_STATE_EMPTY = 1;
    public static final int DOCUMENT_STATE_SMALL = 2;
    public static final int DOCUMENT_STATE_PERSPECTIVE = 3;
    public static final int DOCUMENT_STATE_ROTATION = 4;
    public static final int DOCUMENT_STATE_NO_FOCUS_MEASURED = 5;
    public static final int DOCUMENT_STATE_UNSHARP = 6;
    public static final int DOCUMENT_STATE_NO_ILLUMINATION_MEASURED = 7;
    public static final int DOCUMENT_STATE_BAD_ILLUMINATION = 8;
    public static final int DOCUMENT_STATE_NO_PAGE_CHANGES = 9;

    public static final int DOCUMENT_STATE_OK = 10;

    private static final int ORIENTATION_LANDSCAPE = 0;
    private static final int ORIENTATION_PORTRAIT = 90;
    private static final int ORIENTATION_FLIPPED_LANDSCAPE = 180;
    private static final int ORIENTATION_FLIPPED_PORTRAIT = 270;

    private static final int MAX_STATE_CORRECTION_CNT = 1;

    private static final String TAG = "CVResult";

    private DkPolyRect[] mDKPolyRects;
    private DkPolyRect mLastDKPolyRect;
    private Patch[] mPatches;
    private int mViewWidth, mViewHeight;
    private int mFrameHeight, mFrameWidth;
    private int mCameraOrientation;
    private CVResultCallback mCallback;
    private Context mContext;
    private int mRatioSharpUnsharp;
    private int mCorrectedStatesCnt = 0;

    private int mCVState = -1;
    private double mIllumination;
    private boolean mIsIlluminationComputed;
    private boolean mCheckPageChanges = false;

    public CVResult(Context context) {

        mCallback = (CVResultCallback) context;
        mContext = context;

    }

    public void storePageState() {

        if (mDKPolyRects.length == 0)
            return;

        mCheckPageChanges = true;
        mLastDKPolyRect = mDKPolyRects[0];

    }

    /**
     * Sets the DkPolyRect array, calculated by PageSegmentation.cpp
     * @param dkPolyRects array of page segmentation results
     */
    public void setDKPolyRects(DkPolyRect[] dkPolyRects) {

        synchronized (this) {

            mDKPolyRects = dkPolyRects;
            updateRects();
            stateUpdated();

            // notify is necessary, because the PaintView is waiting for updates on the CVResult
            // object. If notify is not called no update would be drawn.
            this.notify();

        }

    }

    public DkPolyRect[] getDKPolyRects() {

        return mDKPolyRects;

    }

    public void setPatches(Patch[] patches) {

        synchronized (this) {

            mPatches = patches;
            updatePatches();
//            stateUpdated();
            // notify is necessary, because the PaintView is waiting for updates on the CVResult
            // object. If notify is not called no update would be drawn.
            this.notify();
        }

    }

    /**
     * @return focus measurement results
     */
    public Patch[] getPatches() {

        return mPatches;

    }

    public void setIllumination(double illumination) {

        mIllumination = illumination;

    }

    public double getIllumination() {

        return mIllumination;

    }

    public void setIsIlluminationComputed(boolean isIlluminationComputed) {

        mIsIlluminationComputed = isIlluminationComputed;

    }



    /**
     * Sets the dimension of the view.
     * @param width
     * @param height
     */
    public void setViewDimensions(int width, int height) {

        mViewWidth = width;
        mViewHeight = height;

    }

    /**
     * Sets the dimensions of the camera preview frame.
     * @param width
     * @param height
     * @param cameraOrientation
     */
    public void setFrameDimensions(int width, int height, int cameraOrientation) {

        mFrameWidth = width;
        mFrameHeight = height;
        mCameraOrientation = cameraOrientation;

    }

    public int getViewHeight() {

        return mViewHeight;

    }

    /**
     * Converts the coordinates of the patches to screen coordinates.
     */
    private void updatePatches() {

        if (mPatches == null)
            return;

//        Patch patch;
        PointF screenPoint, framePoint;

        int sharpCnt = 0;
        int unsharpCnt = 0;

        for (Patch patch : mPatches) {

            framePoint = new PointF(patch.getPX(), patch.getPY());
            screenPoint = getScreenCoordinates(framePoint, mFrameWidth, mFrameHeight, mViewWidth, mViewHeight, mCameraOrientation);
            patch.setDrawViewPX(screenPoint.x);
            patch.setDrawViewPY(screenPoint.y);

            boolean isInsidePolyRect = false;

            if (mDKPolyRects != null) {

                for (DkPolyRect polyRect : mDKPolyRects) {

                    if (patch.getIsForeGround() && polyRect.isInside(patch.getPoint())) {
                        isInsidePolyRect = true;

                        if (patch.getIsSharp())
                            sharpCnt++;
                        else
                            unsharpCnt++;
                    }
                }
            }

            patch.setIsForeGround(isInsidePolyRect);
        }

        int foreGroundCnt = sharpCnt + unsharpCnt;
        if (foreGroundCnt > 0)
            mRatioSharpUnsharp = (int) Math.round(((float) sharpCnt / foreGroundCnt) * 100);
        else
            mRatioSharpUnsharp = 0;

    }

    /**
     * Converts the coordinates of the rects to screen coordinates.
     */
    private void updateRects() {

        PointF screenPoint, framePoint;

        for (DkPolyRect rect : mDKPolyRects) {

            rect.getPoints();

        }


        for (DkPolyRect polyRect : mDKPolyRects) {

            if (polyRect == null)
                continue;

            ArrayList<PointF> points = polyRect.getPoints();
//            if (points == null)
//                continue;s

            ArrayList<PointF> screenPoints = new ArrayList<PointF>();

            for (int j = 0; j < points.size(); j++) {

                framePoint = new PointF(points.get(j).x, points.get(j).y);
                screenPoint = getScreenCoordinates(framePoint, mFrameWidth, mFrameHeight, mViewWidth, mViewHeight, mCameraOrientation);
//                screenPoint = getScreenCoordinates(framePoint, 500, 500, mViewWidth, mViewHeight, mCameraOrientation);
                screenPoints.add(screenPoint);


            }

            polyRect.setScreenPoints(screenPoints);


        }


    }

    private void stateUpdated() {

        int currentCVState = getCVState();

        // check if the current CV state should be corrected - this is mainly used to remove small outliers:
        if (currentCVState < mCVState) {
            mCorrectedStatesCnt++;
            if (mCorrectedStatesCnt > MAX_STATE_CORRECTION_CNT) {
                mCVState = currentCVState;
                mCorrectedStatesCnt = 0;
            }
        }
        else {
            mCVState = currentCVState;
            mCorrectedStatesCnt = 0;
        }


        mCallback.onStatusChange(mCVState);

    }



    private int getCVState() {

        if (mDKPolyRects == null) {
            mCheckPageChanges = false;
            return DOCUMENT_STATE_EMPTY;
        }

        if (mDKPolyRects.length == 0) {
            mCheckPageChanges = false;
            return DOCUMENT_STATE_EMPTY;
        }

        DkPolyRect polyRect = mDKPolyRects[0];

        if (mCheckPageChanges) {
            if (!isPageChange(polyRect)) {
                return DOCUMENT_STATE_NO_PAGE_CHANGES;
            }
            else
                mCheckPageChanges = false;
        }


        if (!isAreaCorrect(polyRect))
            return DOCUMENT_STATE_SMALL;

        if (!isPerspectiveCorrect(polyRect))
            return DOCUMENT_STATE_PERSPECTIVE;

        if (!isRotationCorrect(polyRect))
            return DOCUMENT_STATE_ROTATION;

        if (mPatches == null)
            return DOCUMENT_STATE_NO_FOCUS_MEASURED;

        if (!isSharp())
            return DOCUMENT_STATE_UNSHARP;

        if (!mIsIlluminationComputed)
            return DOCUMENT_STATE_NO_ILLUMINATION_MEASURED;

        if (!isIlluminationOK())
            return DOCUMENT_STATE_BAD_ILLUMINATION;

        return DOCUMENT_STATE_OK;

    }

    private boolean isPageChange(DkPolyRect polyRect) {

        double largestDist = mLastDKPolyRect.getLargestDistance(polyRect);
        double diagLength = Math.sqrt(mFrameWidth*mFrameWidth + mFrameHeight*mFrameHeight);
        double distPerc = largestDist / diagLength * 100;

        Log.d(TAG, "dist percentage:  " + distPerc);
        return distPerc >= mContext.getResources().getInteger(R.integer.min_page_change_percentage);

    }

    private boolean isAreaCorrect(DkPolyRect polyRect) {

        double area = polyRect.getArea();
        double matArea = mFrameWidth * mFrameHeight;

        double areaPerc = area / matArea * 100;

        return areaPerc >= mContext.getResources().getInteger(R.integer.min_page_area_percentage);

    }

    private boolean isPerspectiveCorrect(DkPolyRect polyRect) {

        double largestAngle = polyRect.getLargestAngle();

        return largestAngle <= mContext.getResources().getInteger(R.integer.max_page_angle);

    }

    private boolean isRotationCorrect(DkPolyRect polyRect) {

        double minAngle = polyRect.getDocumentRotation();
        return minAngle <= mContext.getResources().getInteger(R.integer.max_rotation);

    }

    private boolean isSharp() {

        return mRatioSharpUnsharp >= mContext.getResources().getInteger(R.integer.min_focus_ratio);

    }


    private boolean isIlluminationOK() {

        return mIllumination <= Double.parseDouble(mContext.getResources().getString(R.string.illumination_threshold));

    }


    /**
     * Transforms frame coordinates to screen coordinates.
     * @param framePos
     * @param frameWidth
     * @param frameHeight
     * @param drawWidth
     * @param drawHeight
     * @param orientation
     * @return
     */
    private PointF getScreenCoordinates(PointF framePos, int frameWidth, int frameHeight, int drawWidth, int drawHeight, int orientation) {


        float drawViewPX = -1;
        float drawViewPY = -1;

        // TODO: check out coordination conversation with tablets

        switch (orientation) {

            case ORIENTATION_PORTRAIT: // Portrait mode
                drawViewPX = (frameHeight - framePos.y) / frameHeight * drawWidth;
                drawViewPY = framePos.x / frameWidth * drawHeight;
                break;

            case ORIENTATION_FLIPPED_PORTRAIT: // Portrait mode
                drawViewPX = framePos.y / frameHeight * drawWidth;
                drawViewPY = (frameWidth - framePos.x) / frameWidth * drawHeight;
                break;



            case ORIENTATION_LANDSCAPE: // Landscape mode
                drawViewPX = framePos.x / frameWidth * drawWidth;
                drawViewPY = framePos.y / frameHeight * drawHeight;
                break;

            case ORIENTATION_FLIPPED_LANDSCAPE: // Landscape mode flipped
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


    // Callbacks:
    public interface CVResultCallback {

        void onStatusChange(int status);

    }

}

