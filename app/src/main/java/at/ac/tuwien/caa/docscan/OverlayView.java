/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   04. July 2016
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

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;


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

    public void setDkPolyRects(DkPolyRect[] polyRects) {

        // TODO: Look out for orientation changes, so we do not need to determine these values at each drawing iteration.
        int drawViewWidth = mDrawView.getWidth();
        int drawViewHeight = mDrawView.getHeight();

        int frameWidth = mCameraView.getFrameWidth();
        int frameHeight = mCameraView.getFrameHeight();

        // TODO: Look out for orientation changes, so we do not need to determine cameraDisplayOrientation at each drawing iteration.
        int cameraDisplayOrientation = MainActivity.getCameraDisplayOrientation();

        PointF screenPoint, framePoint;

        DkPolyRect polyRect;

        for (int i = 0; i < polyRects.length; i++) {

            polyRect = polyRects[i];
            // TODO: this should not happen, find out why it still happens:
            if (polyRect == null)
                continue;

            ArrayList<PointF> points = polyRect.getPoints();
//            if (points == null)
//                continue;

            ArrayList<PointF> screenPoints = new ArrayList<PointF>();

            for (int j = 0; j < points.size(); j++) {
                framePoint = new PointF(points.get(j).x, points.get(j).y);
                screenPoint = getScreenCoordinates(framePoint, frameWidth, frameHeight, drawViewWidth, drawViewHeight, cameraDisplayOrientation);
                screenPoints.add(screenPoint);
            }

            polyRect.setScreenPoints(screenPoints);


        }

        mDrawView.setPolyRects(polyRects);

    }

    // Converts the coordinates of the patches and passes it to the DrawView:
    public void setFocusPatches(Patch[] patches) {

        Patch patch;
        PointF screenPoint, framePoint;
        // // TODO: Look out for orientation changes, so we do not need to determine these values at each drawing iteration.
        int drawViewWidth = mDrawView.getWidth();
        int drawViewHeight = mDrawView.getHeight();

        int frameWidth = mCameraView.getFrameWidth();
        int frameHeight = mCameraView.getFrameHeight();

        // TODO: Look out for orientation changes, so we do not need to determine cameraDisplayOrientation at each drawing iteration.
        int cameraDisplayOrientation = MainActivity.getCameraDisplayOrientation();

        for (int i = 0; i < patches.length; i++) {

            patch = patches[i];
            framePoint = new PointF(patch.getPX(), patch.getPY());
            screenPoint = getScreenCoordinates(framePoint, frameWidth, frameHeight, drawViewWidth, drawViewHeight, cameraDisplayOrientation);

            patch.setDrawViewPX(screenPoint.x);
            patch.setDrawViewPY(screenPoint.y);

//            // Check here for the orientation:
//            // Note: frameWidth is always greater than frameHeight - regardless of the orientation.
//            switch (cameraDisplayOrientation) {
//
//                case ORIENTATION_LANDSCAPE:
//                    drawViewPX = patch.getPX() / frameWidth * drawViewWidth;
//                    drawViewPY = patch.getPY() / frameHeight * drawViewHeight;
//                    break;
//
//                case ORIENTATION_PORTRAIT:
//                    drawViewPX = (frameHeight - patch.getPY()) / frameHeight * drawViewWidth;
//                    drawViewPY = patch.getPX() / frameWidth * drawViewHeight;
//                    break;
//
//                case ORIENTATION_FLIPPED_LANDSCAPE:
//                    drawViewPX = (frameWidth - patch.getPX()) / frameWidth * drawViewWidth;
//                    drawViewPY = (frameHeight - patch.getPY()) / frameHeight * drawViewHeight;
//                    break;
//
//            }
//
//            if (drawViewPX > drawViewWidth)
//                drawViewPX = drawViewWidth;
//
//            if (drawViewPY > drawViewHeight)
//                drawViewPY = drawViewHeight;
//
//            patch.setDrawViewPX(drawViewPX);
//            patch.setDrawViewPY(drawViewPY);

        }

        mDrawView.setFocusPatches(patches);
    }

    private PointF getScreenCoordinates(PointF framePos, int frameWidth, int frameHeight, int drawWidth, int drawHeight, int orientation) {


        float drawViewPX = -1;
        float drawViewPY = -1;

        switch (orientation) {

            case ORIENTATION_LANDSCAPE:
                drawViewPX = framePos.x / frameWidth * drawWidth;
                drawViewPY = framePos.y / frameHeight * drawHeight;
                break;

            case ORIENTATION_PORTRAIT:
                drawViewPX = (frameHeight - framePos.y) / frameHeight * drawWidth;
                drawViewPY = framePos.x / frameWidth * drawHeight;
                break;

            case ORIENTATION_FLIPPED_LANDSCAPE:
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


    // Scales the camera view so that the preview has original width to height ratio:
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mCameraView == null) {
            setChildMeasuredDimension(width, height);
            return;
        }

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
