package at.ac.tuwien.caa.docscan.crop;

import android.graphics.PointF;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.cv.DkVector;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropQuad {

    private static final int NO_ACTIVE_POINT_IDX = -1;

    private ArrayList<PointF> mPoints;
    private int mActivePointIdx;

    public CropQuad() {

        mPoints = new ArrayList<>();

    }

    public CropQuad(ArrayList<PointF> points) {

        mPoints = points;

    }

    public ArrayList<PointF> getPoints() {

        return mPoints;

    }

    /**
     * Returns true if a point is close enough to be touched by the user.
     */
    public boolean isPointClose(PointF touchPoint) {

        int closestPointIdx = getClosestPoint(touchPoint);

        // Calculate how close the touch event is to the quad point:
        DkVector v = new DkVector(mPoints.get(closestPointIdx), touchPoint);
        double dist = v.length();

        // TODO: use a screen independent threshold here
        if (dist < 100) {
            mActivePointIdx = closestPointIdx;
            return true;
        }
        else {
            mActivePointIdx = NO_ACTIVE_POINT_IDX;
            return false;
        }

    }

    public boolean moveActivePoint(PointF touchPoint) {

        if (mActivePointIdx == NO_ACTIVE_POINT_IDX)
            return false;

        else {
            mPoints.get(mActivePointIdx).set(touchPoint);
            return true;
        }

    }

    private int getClosestPoint(PointF point) {

        double minDist = Double.POSITIVE_INFINITY;
        int minIdx = 0;
        int idx = 0;

        for (PointF corner : mPoints) {

            DkVector v = new DkVector(corner, point);
            double dist = v.length();
            if (dist < minDist) {
                minDist = dist;
                minIdx = idx;
            }

            idx++;
        }

        return minIdx;
    }

}
