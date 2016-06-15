package at.ac.tuwien.caa.docscan;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

/**
 * Created by fabian on 07.06.2016.
 */
public class NativeWrapper {

//    public static void handleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap) {
//
//        nativeHandleFrame(width, height, nv21Data, bitmap);
//
//    }

    public static native void handleFrame(int width, int height, byte yuv[], int[] rgba);

    public static native void nativeHandleFrame(int width, int height, byte[] nv21Data, Bitmap bitmap);

    public static void logPolar(Mat src, Mat dst, float xCenter, float yCenter, double scaleLog, double scale, double angle) {

        nativeLogPolar(src.getNativeObjAddr(), dst.getNativeObjAddr(), xCenter, yCenter, scaleLog, scale, angle);

    }

    private static native void nativeLogPolar(long src, long dst, float xCenter, float yCenter,  double scaleLog, double scale, double angle);
}
