/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   16. June 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 07.06.2016.
 */
public class NativeWrapper {

    public static Patch[] getFocusMeasures(Mat src) {

        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());

        return patches;

    }

    private static native Patch[] nativeGetFocusMeasures(long src);

    public static DkPolyRect[] getPageSegmentation(Mat src) {

        return nativeGetPageSegmentation(src.getNativeObjAddr());

    }

    private static native DkPolyRect[] nativeGetPageSegmentation(long src);


    public static void processFrame(Mat src) {
        nativeProcessFrame(src.getNativeObjAddr());
    }

//    private static void nativeGetPageSegmentation(long src);


    private static native void nativeProcessFrame(long src);

    // Callbacks:
    public static interface CVCallback {
        void onFocusMeasured(Patch[] patches);
        void onPageSegmented(DkPolyRect[] polyRects);
    }
}
