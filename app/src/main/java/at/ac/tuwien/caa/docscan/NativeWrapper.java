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

        Patch[] patches = nativeGetPatches(src.getNativeObjAddr());

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

    private static native Patch[] nativeGetPatches(long src);
    private static native void nativeProcessFrame(long src);
    private static native Patch nativeProcessFrame2(long src);

    public static native void handleFrame(int width, int height, byte yuv[], int[] rgba);
//    public static native void handleFrame2(int width, int height, byte yuv[], int[] rgba);
    public static native void handleFrame2(int width, int height, byte[] nv21Data, Bitmap bitmap);
    public static native void nativeHandleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap);

    public static void logPolar(Mat src, Mat dst, float xCenter, float yCenter, double scaleLog, double scale, double angle) {

        nativeLogPolar(src.getNativeObjAddr(), dst.getNativeObjAddr(), xCenter, yCenter, scaleLog, scale, angle);

    }

    private static native void nativeLogPolar(long src, long dst, float xCenter, float yCenter,  double scaleLog, double scale, double angle);

    // Callbacks:

    public static interface CVCallback {

        void onFocusMeasured(Patch[] patches);

    }
}
