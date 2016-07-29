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

package at.ac.tuwien.caa.docscan.cv;

import android.graphics.PointF;

import java.util.ArrayList;

/**
 * Class containing the output of the page segmentation task. This class is used in
 * DocScanInterface.cpp Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetPageSegmentation
 * There the DkPolyRect CPP object is converted to a DkPolyRect Java object.
 */
public class DkPolyRect {



    ArrayList<PointF> mPoints;
    ArrayList<PointF> mScreenPoints;

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
    public DkPolyRect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

        mPoints = new ArrayList<PointF>();

        mPoints.add(new PointF(x1, y1));
        mPoints.add(new PointF(x2, y2));
        mPoints.add(new PointF(x3, y3));
        mPoints.add(new PointF(x4, y4));


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


}
