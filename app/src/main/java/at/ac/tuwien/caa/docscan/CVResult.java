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

package at.ac.tuwien.caa.docscan;

import android.graphics.PointF;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Class responsible for holding the results of the page segmentation and focus measurement tasks.
 * The coordinates of the resulting objects are mapped by this class from frame to screen coordinates.
 * Note that the drawing thread in the PaintView is waiting for updates of this class, so take care
 * to call notify if the class receives an update.
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
    private int mCameraOrientation;

    public CVResult() {


    }

    /**
     * Sets the DkPolyRect array, calculated by PageSegmentation.cpp
     * @param dkPolyRects array of page segmentation results
     */
    public void setDKPolyRects(DkPolyRect[] dkPolyRects) {

        synchronized (this) {

            mDKPolyRects = dkPolyRects;
            updateRects();
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

    /**
     * Converts the coordinates of the patches to screen coordinates.
     */
    private void updatePatches() {


        Patch patch;
        PointF screenPoint, framePoint;

        for (int i = 0; i < mPatches.length; i++) {

            patch = mPatches[i];
            framePoint = new PointF(patch.getPX(), patch.getPY());
            screenPoint = getScreenCoordinates(framePoint, mFrameWidth, mFrameHeight, mViewWidth, mViewHeight, mCameraOrientation);

            patch.setDrawViewPX(screenPoint.x);
            patch.setDrawViewPY(screenPoint.y);


        }


    }

    /**
     * Converts the coordinates of the rects to screen coordinates.
     */
    private void updateRects() {

        DkPolyRect polyRect;
        PointF screenPoint, framePoint;

        for (int i = 0; i < mDKPolyRects.length; i++) {

            polyRect = mDKPolyRects[i];

            if (polyRect == null)
                continue;

            ArrayList<PointF> points = polyRect.getPoints();
//            if (points == null)
//                continue;

            ArrayList<PointF> screenPoints = new ArrayList<PointF>();

            for (int j = 0; j < points.size(); j++) {
                framePoint = new PointF(points.get(j).x, points.get(j).y);
                screenPoint = getScreenCoordinates(framePoint, mFrameWidth, mFrameHeight, mViewWidth, mViewHeight, mCameraOrientation);
                screenPoints.add(screenPoint);
            }

            polyRect.setScreenPoints(screenPoints);


        }


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

}
