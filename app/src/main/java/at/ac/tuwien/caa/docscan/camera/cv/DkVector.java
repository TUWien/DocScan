package at.ac.tuwien.caa.docscan.camera.cv;

import android.graphics.PointF;
import android.util.Log;

/**
 * Created by fabian on 27.09.2016.
 */
public class DkVector {

    public float x, y;

    private static final String CLASS_NAME = "DkVector";

    public DkVector(float x, float y) {

        this.x = x;
        this.y = y;

    }

    public DkVector(PointF point1, PointF point2) {

        x = point1.x - point2.x;
        y = point1.y - point2.y;

    }

    public DkVector getNormalVector() {

        return new DkVector(-y, x).norm();

    }

    public DkVector project(DkVector vector) {

        DkVector m = multiply(vector);
        float length = (float) (vector.length() * vector.length());
        DkVector d = m.divide(length);
        DkVector r = d.multiply(vector);

        return r;

    }

    public DkVector bisect(DkVector vector) {

        DkVector v1 = norm();
        DkVector v2 = vector.norm();

        return v1.add(v2);

    }

    private DkVector add(DkVector vector) {

        return new DkVector(x + vector.x, y + vector.y);

    }

    public DkVector norm() {

        return this.divide((float) this.length());

    }

    private DkVector divide(float scalar) {

        return new DkVector(x / scalar, y / scalar);

    }

    private DkVector multiply(DkVector vector) {

        return new DkVector(x * vector.x, y * vector.y);

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

//        Check if cosv is in valid range: [-1, 1]
        double c = cosv(vector);
        if (c < -1)
            c = -1;
        else if (c > 1)
            c = 1;

        return Math.toDegrees(Math.acos(c));

    }

    public double cosv(DkVector vector) {

        return (x * vector.x + y * vector.y) / (Math.sqrt(x * x + y * y) * Math.sqrt(vector.x * vector.x + vector.y * vector.y));

    }

    public DkVector rotate(float angle) {

        double a = Math.toRadians(angle);
        double cs = Math.cos(a);
        double sn = Math.sin(a);

        float xn = (float) (x * cs - y * sn);
        float yn = (float) (x * sn + y * cs);

        return new DkVector(xn, yn);

    }
}