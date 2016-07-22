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


public class NativeWrapper {

    private static long mPageSegmentationTime;
    private static long mFocusMeasureTime;

    public static Patch[] getFocusMeasures(Mat src) {

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());

        return patches;

    }

    // This is only called if the debug view is active:
    public static Patch[] getFocusMeasuresDebug(Mat src) {

        long startTime = System.currentTimeMillis();
        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());
        mFocusMeasureTime = System.currentTimeMillis() - startTime;

        return patches;

    }

    public static long getFocusMeasureTime() {

        return mFocusMeasureTime;

    }

    public static native void nativeGetPageSegmentationTest(int width, int height, byte yuv[], int[] rgba);

    private static native Patch[] nativeGetFocusMeasures(long src);



    public static DkPolyRect[] getPageSegmentation(Mat src) {

        return nativeGetPageSegmentation(src.getNativeObjAddr());

    }

    // This is only called if the debug view is active:
    public static DkPolyRect[] getPageSegmentationDebug(Mat src) {

        long startTime = System.currentTimeMillis();
        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());
        mPageSegmentationTime = System.currentTimeMillis() - startTime;

        return nativeGetPageSegmentation(src.getNativeObjAddr());

    }

    public static long getPageSegmentationTime() {

        return mPageSegmentationTime;

    }

    private static native DkPolyRect[] nativeGetPageSegmentation(long src);


    public static void processFrame(Mat src) {
        nativeProcessFrame(src.getNativeObjAddr());
    }

//    private static void nativeGetPageSegmentation(long src);


    private static native void nativeProcessFrame(long src);

    // Callbacks:
    public interface CVCallback {

        void onFocusMeasured(Patch[] patches);
        void onPageSegmented(DkPolyRect[] polyRects);


    }
}
