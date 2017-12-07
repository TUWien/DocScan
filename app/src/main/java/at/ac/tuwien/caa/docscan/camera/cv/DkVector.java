package at.ac.tuwien.caa.docscan.camera.cv;

import android.graphics.PointF;

/**
 * Created by fabian on 27.09.2016.
 */
public class DkVector {

    public float x, y;

    private PointF mPoint1, mPoint2;

    public DkVector(float x, float y) {

        this.x = x;
        this.y = y;

    }

    public DkVector(PointF point1, PointF point2) {

        x = point1.x - point2.x;
        y = point1.y - point2.y;

        mPoint1 = point1;
        mPoint2 = point2;

    }

    public PointF getPoint1() {
        return mPoint1;
    }

    public PointF getPoint2() {
        return mPoint2;
    }

    public double length() {

        double l = Math.sqrt(x * x + y * y);
        return l;

    }

    public float scalarProduct(DkVector vector) {

        return x * vector.x + y * vector.y;

    }

    public DkVector multiply(float scalar) {

        float nx = x * scalar;
        float ny = y * scalar;

        return new DkVector(nx, ny);

    }



    public double angle(DkVector vector) {

        return Math.toDegrees(Math.acos(cosv(vector)));

    }

    public double cosv(DkVector vector) {

        return (x * vector.x + y * vector.y) / (Math.sqrt(x * x + y * y) * Math.sqrt(vector.x * vector.x + vector.y * vector.y));

    }
}