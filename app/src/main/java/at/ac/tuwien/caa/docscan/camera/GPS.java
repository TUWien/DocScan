package at.ac.tuwien.caa.docscan.camera;

import android.location.Location;

/**
 * Taken from:
 * @see <a href="http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android">http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android</a>
 * @author fabien
 */
public class GPS {
    private static StringBuilder sb = new StringBuilder(20);
    private String mLongitude, mLatitude;

    public GPS(Location location) {
        if (location != null) {
            mLongitude = convert(location.getLongitude());
            mLatitude = convert(location.getLatitude());
        }
    }

    public GPS(String longitude, String latitude) {

        mLongitude = longitude;
        mLatitude = latitude;

    }
    public GPS(double longitudeVal, double latitudeVal) {

        mLongitude = convert(longitudeVal);
        mLatitude = convert(latitudeVal);

    }

    public String getLongitude() {
        return mLongitude;
    }

    public String getLatitude() {
        return mLatitude;
    }

    /**
     * returns ref for latitude which is S or N.
     * @param latitude
     * @return S or N
     */
    public static String latitudeRef(double latitude) {
        return latitude<0.0d?"S":"N";
    }

    /**
     * returns ref for latitude which is S or N.
     * @param longitude
     * @return S or N
     */
    public static String longitudeRef(double longitude) {
        return longitude<0.0d?"W":"E";
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param latitude could be longitude.
     * @return
     */
    synchronized public static final String convert(double latitude) {
        latitude=Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude*1000.0d);

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }
}