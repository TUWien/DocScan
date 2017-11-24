package at.ac.tuwien.caa.docscan.crop;

/**
 * Created by fabian on 23.11.2017.
 */

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Class designed to be passable between multiple intents (by implementing Parcelable). It contains
 * a polygon that is calculated with the PageSegmentation
 */
public class CropInfo implements Parcelable {

    public static final String CROP_INFO_NAME = "CROP_INFO_NAME";

    private ArrayList<PointF> mPoints;
    private String mFileName;

    public CropInfo(ArrayList<PointF> points, String fileName) {

        mPoints = points;
        mFileName = fileName;

    }

    protected CropInfo(Parcel in) {

        mPoints = in.createTypedArrayList(PointF.CREATOR);
        mFileName = in.readString();

    }

    public static final Creator<CropInfo> CREATOR = new Creator<CropInfo>() {
        @Override
        public CropInfo createFromParcel(Parcel in) {
            return new CropInfo(in);
        }

        @Override
        public CropInfo[] newArray(int size) {
            return new CropInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeTypedList(mPoints);
        dest.writeString(mFileName);

    }

    public String getFileName() {
        return mFileName;
    }

    public ArrayList<PointF> getPoints() {
        return mPoints;
    }
}
