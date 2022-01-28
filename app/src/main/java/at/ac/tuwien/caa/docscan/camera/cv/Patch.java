/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   22. June 2016
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

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;

/**
 * Class containing the output of the focus measurement task. This class is used in
 * DocScanInterface.cpp Java_at_ac_tuwien_caa_docscan_NativeWrapper_nativeGetFocusMeasures
 * There the Patch CPP object is converted to a Patch Java object.
 */
@Keep
public class Patch implements Parcelable {

    public static final String KEY_FOCUS = "KEY_FOCUS";

    private float mPX, mPY;
    private int mWidth, mHeight;
    private double mFm;
    private boolean mIsSharp, mIsForeGround;

    // These are coordinates used for drawing:
    private float mDrawViewPX, mDrawViewPY;

    /**
     * Creates a Patch object encoding the focus measurement of a patch of the camera frame.
     *
     * @param pX     x coordinate of the center of the patch
     * @param pY     y coordinate of the center of the patch
     * @param width  width of the patch
     * @param height height of the patch
     * @param fM     focus value
     */
    public Patch(float pX, float pY, int width, int height, double fM) {

        mPX = pX;
        mPY = pY;
        mWidth = width;
        mHeight = height;
        mFm = fM;

    }

    /**
     * Creates a Patch object encoding the focus measurement of a patch of the camera frame.
     *
     * @param pX           x coordinate of the center of the patch - in frame coordinates
     * @param pY           y coordinate of the center of the patch - in frame coordinates
     * @param width        width of the patch
     * @param height       height of the patch
     * @param fM           focus value
     * @param isSharp      boolean indicating if the focus value is larger than a user defined threshold
     * @param isForeGround boolean indicating if the patch is belonging to the foreground
     *                     (meaning it has sufficient inhomogeneous image content)
     */
    public Patch(float pX, float pY, int width, int height, double fM, boolean isSharp, boolean isForeGround) {

        mPX = pX;
        mPY = pY;
        mWidth = width;
        mHeight = height;
        mFm = fM;
        mIsSharp = isSharp;
        mIsForeGround = isForeGround;

    }

    public Patch() {

    }

    protected Patch(Parcel in) {
        mPX = in.readFloat();
        mPY = in.readFloat();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mFm = in.readDouble();
        mIsSharp = in.readByte() != 0;
        mIsForeGround = in.readByte() != 0;
        mDrawViewPX = in.readFloat();
        mDrawViewPY = in.readFloat();
    }

    public static final Creator<Patch> CREATOR = new Creator<Patch>() {
        @Override
        public Patch createFromParcel(Parcel in) {
            return new Patch(in);
        }

        @Override
        public Patch[] newArray(int size) {
            return new Patch[size];
        }
    };

    /**
     * Returns the x coordinate of the center in frame coordinates
     *
     * @return the x coordinate in frame coordinates
     */
    public float getPX() {

        return mPX;

    }

    /**
     * Returns the y coordinate of the center in frame coordinates
     *
     * @return the y coordinate in frame coordinates
     */
    public float getPY() {

        return mPY;

    }

    public PointF getPoint() {

        return new PointF(mPX, mPY);
    }

    /**
     * Returns the width of the patch in frame coordinates.
     *
     * @return
     */
    public int getWidth() {

        return mWidth;

    }

    /**
     * Returns the height of the patch in frame coordinates.
     *
     * @return
     */
    public int getHeight() {

        return mHeight;

    }

    /**
     * Returns the focus value.
     *
     * @return
     */
    public double getFM() {

        return mFm;

    }

    /**
     * Returns a boolean indicating whether the focus value exceeds a user defined threshold
     *
     * @return boolean indicating whether the patch content is in focus.
     */
    public boolean getIsSharp() {

        return mIsSharp;

    }

    public void setIsSharp(boolean isSharp) {
        mIsSharp = isSharp;
    }

    /**
     * Returns a boolean indicating whether the patch belongs to the foreground (meaning it has
     * enough image content).
     *
     * @return boolean
     */
    public boolean getIsForeGround() {

        return mIsForeGround;

    }

    public void setIsForeGround(boolean isForeGround) {

        mIsForeGround = isForeGround;

    }

    /**
     * Sets the x coordinate of the center of patch in screen coordinates.
     *
     * @param drawViewPX
     */
    public void setDrawViewPX(float drawViewPX) {

        mDrawViewPX = drawViewPX;
    }


    /**
     * Returns the x coordinate of the center of the patch in screen coordinates.
     *
     * @return
     */
    public float getDrawViewPX() {

        return mDrawViewPX;

    }

    /**
     * Sets the y coordinate of the center of patch in screen coordinates.
     *
     * @return
     */
    public float getDrawViewPY() {

        return mDrawViewPY;

    }

    /**
     * Returns the y coordinate of the center of the patch in screen coordinates.
     *
     * @param drawViewPY
     */
    public void setDrawViewPY(float drawViewPY) {
        mDrawViewPY = drawViewPY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeFloat(mPX);
        dest.writeFloat(mPY);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeDouble(mFm);
        dest.writeByte((byte) (mIsSharp ? 1 : 0));
        dest.writeByte((byte) (mIsForeGround ? 1 : 0));
        dest.writeFloat(mDrawViewPX);
        dest.writeFloat(mDrawViewPY);

    }
}

