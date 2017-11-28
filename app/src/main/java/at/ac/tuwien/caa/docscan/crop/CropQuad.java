package at.ac.tuwien.caa.docscan.crop;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.camera.cv.DkVector;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropQuad {

    private static final int NO_ACTIVE_POINT_IDX = -1;

    private ArrayList<PointF> mViewPoints, mImgPoints;
    private int mActivePointIdx;
//    private int mImgWidth, mImgHeight;


    public CropQuad(ArrayList<PointF> points, int imgWidth, int imgHeight) {

        mImgPoints = convertPoints(points, imgWidth, imgHeight);

    }

    public void setViewPoints(ArrayList<PointF> points) {

        mViewPoints = points;

    }


    private ArrayList convertPoints(ArrayList<PointF> normedPoints, int imgWidth, int imgHeight) {

        ArrayList points = new ArrayList<>();

        for (PointF normedPoint : normedPoints)
            points.add(new PointF(normedPoint.x * imgWidth, normedPoint.y * imgHeight));

        return points;

    }

    public ArrayList<PointF> getImgPoints() {

        return mImgPoints;

    }

    public ArrayList<PointF> getViewPoints() {

        return mViewPoints;

    }

    /**
     * Returns true if a point is close enough to be touched by the user.
     */
    public boolean isPointClose(PointF touchPoint) {

        int closestPointIdx = getClosestPoint(touchPoint);

        // Calculate how close the touch event is to the quad point:
        DkVector v = new DkVector(mViewPoints.get(closestPointIdx), touchPoint);
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

            // Check the new point position, we do not accept concavities:
            if (isLeft(mActivePointIdx, touchPoint))
                mViewPoints.get(mActivePointIdx).set(touchPoint);

            return true;
        }

    }

    private boolean isLeft(int pointIdx, PointF point) {


        Log.d(getClass().getName(), "points length: " + mViewPoints.size());

        int startIdx = (pointIdx - 1) % mViewPoints.size();
        if (startIdx < 0)
            startIdx += mViewPoints.size();
        int endIdx = (pointIdx + 1) % mViewPoints.size();

        PointF p1 = mViewPoints.get(startIdx);
        PointF p2 = mViewPoints.get(endIdx);

        return isLeft(p2, p1, point);

    }

    private boolean isLeft(PointF p1, PointF p2, PointF p3) {
        return (p2.x - p1.x) * (p3.y - p1.y)  - (p2.y - p1.y) * (p3.x - p1.x) > 0;
//        return ((b.X - a.X)*(c.Y - a.Y) - (b.Y - a.Y)*(c.X - a.X)) > 0;
    }

    private double getAngle(int pointIdx) {

        DkVector v1 = new DkVector(mViewPoints.get(pointIdx - 1), mViewPoints.get(pointIdx % mViewPoints.size()));
        DkVector v2 = new DkVector(mViewPoints.get(pointIdx  % mViewPoints.size()), mViewPoints.get((pointIdx+1) % mViewPoints.size()));

        double cAngle = v1.angle(v2);

        return cAngle;

    }

    private int getClosestPoint(PointF point) {

        double minDist = Double.POSITIVE_INFINITY;
        int minIdx = 0;
        int idx = 0;

        for (PointF corner : mViewPoints) {

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
