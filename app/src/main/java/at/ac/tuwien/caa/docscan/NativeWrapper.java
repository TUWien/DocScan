/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   07. June 2016
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

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Class responsible for calling native methods (for page segmentation and focus measurement).
 */
public class NativeWrapper {


    /**
     * Returns an array of Patch objects, containing focus measurement results.
     * @param src OpenCV Mat
     * @return array of Patch objects
     */
    public static Patch[] getFocusMeasures(Mat src) {

        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());

        return patches;

    }

    /**
     * Returns an array of DkPolyRect objects, containing page segmentation results.
     * @param src OpenCV Mat
     * @return array of DkPolyRect objects
     */
    public static DkPolyRect[] getPageSegmentation(Mat src) {

        return nativeGetPageSegmentation(src.getNativeObjAddr());

    }

    public static double getIllumination(Mat src, DkPolyRect polyRect) {
        return nativeGetIllumination(src.getNativeObjAddr(), polyRect);
    }

    /**
     * Native method for focus measurement.
     * @param src
     * @return
     */
    private static native Patch[] nativeGetFocusMeasures(long src);


    /**
     * Native method for page segmentation.
     * @param src
     * @return
     */
    private static native DkPolyRect[] nativeGetPageSegmentation(long src);

    private static native double nativeGetIllumination(long src, DkPolyRect polyRect);


    // Callbacks:
    public interface CVCallback {

        void onFocusMeasured(Patch[] patches);
        void onPageSegmented(DkPolyRect[] polyRects);
        void onIluminationComputed(double value);

    }
}
