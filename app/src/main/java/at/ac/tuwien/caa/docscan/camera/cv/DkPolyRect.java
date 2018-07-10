/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   5. July 2016
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

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Class containing the output of the page segmentation task. This class is used in
 * DocScanInterface.cpp Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentation
 * There the DkPolyRect CPP object is converted to a DkPolyRect Java object.
 */
public class DkPolyRect implements Parcelable {

    public static final String KEY_POLY_RECT = "KEY_POLY_RECT";

    ArrayList<PointF> mPoints;
    ArrayList<PointF> mScreenPoints;
    int mChl = -1;
    int mThr = -1;
    private int mFrameID;

    /**
     * Creates a polygon with four corners, marking the borders of a document.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @param x4
     * @param y4
     */
    public DkPolyRect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int chl, int thr) {

        mPoints = new ArrayList<PointF>();

        mPoints.add(new PointF(x1, y1));
        mPoints.add(new PointF(x2, y2));
        mPoints.add(new PointF(x3, y3));
        mPoints.add(new PointF(x4, y4));

        mChl = chl;
        mThr = thr;
    }

    public DkPolyRect() {

        mPoints = new ArrayList<PointF>();
        mPoints.add(new PointF());
        mPoints.add(new PointF());
        mPoints.add(new PointF());
        mPoints.add(new PointF());

    }

    protected DkPolyRect(Parcel in) {
        mPoints = in.createTypedArrayList(PointF.CREATOR);
        mScreenPoints = in.createTypedArrayList(PointF.CREATOR);
        mChl = in.readInt();
        mThr = in.readInt();
        mFrameID = in.readInt();
    }

    public static final Creator<DkPolyRect> CREATOR = new Creator<DkPolyRect>() {
        @Override
        public DkPolyRect createFromParcel(Parcel in) {
            return new DkPolyRect(in);
        }

        @Override
        public DkPolyRect[] newArray(int size) {
            return new DkPolyRect[size];
        }
    };

    public void setFrameID(int frameID) {
        mFrameID = frameID;
    }

    public int getFrameID() {
        return mFrameID;
    }

    /**
     * Returns the list of corners (in frame coordinates).
     * @return a list of Points encoding the polygon corners.
     */
    public ArrayList<PointF> getPoints() {

        return mPoints;

    }

    /**
     * Sets the screen coordinates of the polygon.
     * @param screenPoints
     */
    public void setScreenPoints (ArrayList<PointF> screenPoints) {

        mScreenPoints = screenPoints;

    }

    /**
     * Returns the list of corners (in screen coordinates).
     * @return a list of Points encoding the polygon corners.
     */
    public ArrayList<PointF> getScreenPoints() {

        return mScreenPoints;

    }



    public double getLargestDistance(DkPolyRect polyRect) {

        // Unfortunately the corners are not ordered (just in a counter-clockwise order), meaning we cannot calculate the distance directly.
        // Instead we find the corner that is closest to the first corner and then use the following corners.

        int closestIdx = getClosestPoint(polyRect.getPoints().get(0));

        double maxDist = 0;

        for (int i = 0; i < mPoints.size(); i++) {

            PointF p1 = mPoints.get(i);
            PointF p2 = polyRect.getPoints().get((i + closestIdx) % mPoints.size());
            DkVector v = new DkVector(p1, p2);
            double dist = v.length();

            if (dist > maxDist)
                maxDist = dist;

        }

        return maxDist;

    }

    public PointF getLargestDistVector(DkPolyRect polyRect) {

        // Unfortunately the corners are not ordered (just in a counter-clockwise order), meaning we cannot calculate the distance directly.
        // Instead we find the corner that is closest to the first corner and then use the following corners.

        int closestIdx = getClosestPoint(polyRect.getPoints().get(0));

        double maxDist = 0;
        PointF result = null;

        for (int i = 0; i < mPoints.size(); i++) {

            PointF p1 = mPoints.get(i);
            PointF p2 = polyRect.getPoints().get((i + closestIdx) % mPoints.size());
            DkVector v = new DkVector(p1, p2);
            double dist = v.length();

            if (dist > maxDist) {
                maxDist = dist;
                result = new PointF(v.x, v.y);
            }

        }

        return result;

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

    /**
     * Returns the largest angle within a polyrect.
     * @return
     */

    public double getLargestAngle() {

        double maxAngle = 0;

        for (int i = 1; i < mPoints.size() + 1; i++) {

            DkVector v1 = new DkVector(mPoints.get(i - 1), mPoints.get(i % mPoints.size()));
            DkVector v2 = new DkVector(mPoints.get(i  % mPoints.size()), mPoints.get((i+1) % mPoints.size()));

            double cAngle = v1.angle(v2);
            if (cAngle > maxAngle)
                maxAngle = cAngle;

        }

        return maxAngle;
    }

    /**
     * Returns the overall rotation of the page segmentation result. The rotation is measured
     * by taking the angle between the 'main' vector x=0, y=1 and the side of the document that has
     * the smallest enclosing angle.
     * @return
     */
    public double getDocumentRotation() {

        double minAngle = 360;

        DkVector v1 = new DkVector(new PointF(0,0), new PointF(0,1));

        for (int i = 1; i < mPoints.size() + 1; i++) {


            DkVector v2 = new DkVector(mPoints.get(i  % mPoints.size()), mPoints.get((i+1) % mPoints.size()));

            double cAngle = v1.angle(v2);
            if (cAngle < minAngle)
                minAngle = cAngle;

        }

        return minAngle;

    }

    public double getArea() {

        double area = 0.5 * Math.abs((mPoints.get(0).y - mPoints.get(2).y) * (mPoints.get(3).x - mPoints.get(1).x) +
                        (mPoints.get(1).y - mPoints.get(3).y) * (mPoints.get(0).x - mPoints.get(2).x));

        return area;

    }

    public boolean isInside(PointF point) {

        float lastSign = 0;

        // we assume, that the polygon is convex
        // so if the point has a different scalar product
        // for one side of the polygon - it is not inside

        for (int i = 1; i < mPoints.size() + 1; i++) {

            DkVector dv = new DkVector(mPoints.get(i - 1), mPoints.get(i % mPoints.size()));
            float cSign = dv.scalarProduct(new DkVector(point, mPoints.get(i  % mPoints.size())));

            if (lastSign * cSign < 0) {
                return false;
            }

            lastSign = cSign;

        }

        return true;

    }

    public float getX1() { return mPoints.get(0).x; }
    public float getY1() { return mPoints.get(0).y; }

    public float getX2() { return mPoints.get(1).x; }
    public float getY2() { return mPoints.get(1).y; }

    public float getX3() { return mPoints.get(2).x; }
    public float getY3() { return mPoints.get(2).y; }

    public float getX4() { return mPoints.get(3).x; }
    public float getY4() { return mPoints.get(3).y; }

    public int channel() { return mChl; }

    public int threshold() { return mThr; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeFloat(mPoints.get(0).x);
        dest.writeFloat(mPoints.get(0).y);

        dest.writeFloat(mPoints.get(1).x);
        dest.writeFloat(mPoints.get(1).y);

        dest.writeFloat(mPoints.get(2).x);
        dest.writeFloat(mPoints.get(2).y);

        dest.writeFloat(mPoints.get(3).x);
        dest.writeFloat(mPoints.get(3).y);

    }

}
