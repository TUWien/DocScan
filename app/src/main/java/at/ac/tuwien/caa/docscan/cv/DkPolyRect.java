package at.ac.tuwien.caa.docscan.cv;

import android.graphics.PointF;

import java.util.ArrayList;

/**
 * Created by fabian on 05.07.2016.
 */
public class DkPolyRect {



    ArrayList<PointF> mPoints;
    ArrayList<PointF> mScreenPoints;

    // TODO: Pass a list as argument. This is just temporary, since I am not sure how this works in JNI:
    public DkPolyRect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

        mPoints = new ArrayList<PointF>();

        mPoints.add(new PointF(x1, y1));
        mPoints.add(new PointF(x2, y2));
        mPoints.add(new PointF(x3, y3));
        mPoints.add(new PointF(x4, y4));


    }

    public ArrayList<PointF> getPoints() {

        return mPoints;

    }

    public void setScreenPoints (ArrayList<PointF> screenPoints) {

        mScreenPoints = screenPoints;

    }

    public ArrayList<PointF> getScreenPoints() {

        return mScreenPoints;

    }


}
