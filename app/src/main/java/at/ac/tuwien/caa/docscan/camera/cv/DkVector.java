package at.ac.tuwien.caa.docscan.camera.cv;

import android.graphics.PointF;

/**
 * Created by fabian on 27.09.2016.
 */
public class DkVector {

    public float x, y;

    public DkVector(PointF point1, PointF point2) {

        x = point1.x - point2.x;
        y = point1.y - point2.y;

    }

    public double length() {

        double l = Math.sqrt(x * x + y * y);
        return l;

    }

    public float scalarProduct(DkVector vector) {

        return x * vector.x + y * vector.y;

    }

    public double angle(DkVector vector) {

        return Math.toDegrees(Math.acos(cosv(vector)));

    }

    private double cosv(DkVector vector) {

        return (x * vector.x + y * vector.y) / (Math.sqrt(x * x + y * y) * Math.sqrt(vector.x * vector.x + vector.y * vector.y));

    }
}