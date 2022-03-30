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

package at.ac.tuwien.caa.docscan.camera.cv;

import androidx.annotation.Keep;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Class responsible for calling native methods (for page segmentation and focus measurement).
 */
@Keep
public class NativeWrapper {

    private static final String CLASS_NAME = "NativeWrapper";
    private static final int MAX_IMG_SIZE = 600;

    private static boolean mUseLab = true;
    private static DkPolyRect mOldRect = new DkPolyRect();

    /**
     * Returns an array of Patch objects, containing focus measurement results.
     *
     * @param src OpenCV Mat
     * @return array of Patch objects
     */
    public static Patch[] getFocusMeasures(Mat src) {

        Patch[] patches = nativeGetFocusMeasures(src.getNativeObjAddr());

        return patches;

    }

    /**
     * Returns an array of DkPolyRect objects, containing page segmentation results.
     *
     * @param src OpenCV Mat
     * @return array of DkPolyRect objects
     */
    public static DkPolyRect[] getPageSegmentation(Mat src) {

        DkPolyRect[] rects = nativeGetPageSegmentation(src.getNativeObjAddr(), mUseLab, mOldRect);

        if (rects.length > 0)
            mOldRect = rects[0];
        else
            mOldRect = new DkPolyRect();


        return rects;
    }

    public static float resize(Mat mat) {

        //        resize the image:
        int maxImgSide = Math.max(mat.rows(), mat.cols());
        float scale = (float) MAX_IMG_SIZE / maxImgSide;

        // do not upscale
        if (scale > 0.8f || scale <= 0.0f)
            scale = 1.0f;

        if (scale != 1.0f)
            Imgproc.resize(mat, mat,
                    new Size(Math.round(mat.width() * scale), Math.round(mat.height() * scale)));

        return scale;

    }

//    public static double getIllumination(Mat src, DkPolyRect polyRect) {
//        return nativeGetIllumination(src.getNativeObjAddr(), polyRect);
//    }

    /**
     * Native method for focus measurement.
     *
     * @param src input image
     * @return array of Patch objects
     */
    @SuppressWarnings("JniMissingFunction")
    private static native Patch[] nativeGetFocusMeasures(long src);


    /**
     * Native method for page segmentation.
     *
     * @param src input image
     * @return array of DKPolyRect objects
     */
    @SuppressWarnings("JniMissingFunction")
    private static native DkPolyRect[] nativeGetPageSegmentation(long src, boolean useLab, DkPolyRect polyRect);

    /**
     * Native method for illumination computation.
     *
     * @param src      input image
     * @param polyRect rectangle describing the page
     * @return double value measuring the illumination quality
     */
//    @SuppressWarnings("JniMissingFunction")
//    private static native double nativeGetIllumination(long src, DkPolyRect polyRect);

    public static void setUseLab(boolean useLab) {
        mUseLab = useLab;
    }

    public static boolean useLab() {
        return mUseLab;
    }


}
