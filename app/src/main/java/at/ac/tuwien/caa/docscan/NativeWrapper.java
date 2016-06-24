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

import android.graphics.Bitmap;

import org.opencv.core.Mat;

import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Created by fabian on 07.06.2016.
 */
public class NativeWrapper {

//    public static void handleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap) {
//
//        nativeHandleFrame(width, height, nv21Data, bitmap);
//
//    }

    public static Patch[] getFocusMeasures(Mat src) {

        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());

        return patches;

    }




    public static void processFrame(Mat src) {
        nativeProcessFrame(src.getNativeObjAddr());
    }

    public static Patch processFrameTest(Mat src) {

        Patch patch =  null;
//        Patch patch = nativeProcessFrame2(src.getNativeObjAddr());

        return patch;

    }

    private static native Patch[] nativeGetFocusMeasures(long src);
    private static native void nativeProcessFrame(long src);
//    private static native Patch nativeProcessFrame2(long src);
//
//    public static native void handleFrame(int width, int height, byte yuv[], int[] rgba);
////    public static native void handleFrame2(int width, int height, byte yuv[], int[] rgba);
    public static native void handleFrame2(int width, int height, byte[] nv21Data, Bitmap bitmap);
//    public static native void nativeHandleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap);

    public static void logPolar(Mat src, Mat dst, float xCenter, float yCenter, double scaleLog, double scale, double angle) {

        nativeLogPolar(src.getNativeObjAddr(), dst.getNativeObjAddr(), xCenter, yCenter, scaleLog, scale, angle);

    }

    private static native void nativeLogPolar(long src, long dst, float xCenter, float yCenter,  double scaleLog, double scale, double angle);

    // Callbacks:

    public static interface CVCallback {

        void onFocusMeasured(Patch[] patches);

    }
}
