/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   17. January 2017
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

package at.ac.tuwien.caa.docscan.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

import java.util.List;

/**
 * Created by fabian on 17.01.2017.
 */
public class LocationHandler implements LocationListener {

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final double MIN_ACCURACY = 30;
    private static final long UPDATE_TIME = 100; // Location update time in milli-seconds.
    private static final float UPDATE_DISTANCE = 10; // Location update in meters.
    private static final long MAX_TIME_RUNNING = 1000 * 60 * 5;  // Maximum time the location is requested - in milli-seconds. Note: Normally this time should not pass, but a useful location should be found before!

//    We use here a singleton, because the location should be read one time after the app starts.
//    The CameraActivity can be created multiple times, during the app is running, so we use here
//    a static instance to prevent multiple location accesses.
    private static LocationHandler mInstance = null;

    private Location mLocation;
    private LocationManager mLocationManager;
    private Context mContext;
    private long mStartTime;

    public static LocationHandler getInstance(Context context) {

        if (mInstance == null)
            mInstance = new LocationHandler(context);

        return mInstance;

    }

    private LocationHandler(Context context) {

        mContext = context;

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                boolean stopManager = false;

                // Called when a new location is found:
                if (isBetterLocation(location, mLocation)) {
                    mLocation = location;

                    if (mLocation.getAccuracy() <= MIN_ACCURACY)
                        stopManager = true;
                }
                if (System.currentTimeMillis() - mStartTime >= MAX_TIME_RUNNING)
                    stopManager = true;

                if (stopManager && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED)
                    mLocationManager.removeUpdates(this);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<String> providers = mLocationManager.getProviders(true);
            if (providers.contains(LocationManager.NETWORK_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_TIME, UPDATE_DISTANCE, locationListener);
            if (providers.contains(LocationManager.GPS_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME, UPDATE_DISTANCE, locationListener);

            mStartTime = System.currentTimeMillis();
        }


    }

    public Location getLocation() {

        if (mLocation != null)
            return mLocation;
        else {
//            If no location has been found yet, use the last known location as a fallback:
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED) {
                Location l1 = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location l2 = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (l1 != null && l2 != null) {
                    if (l1.getAccuracy() > l2.getAccuracy())
                        return l1;
                    else
                        return l2;
                }
                else {
                    if (l1 != null)
                        return l1;
                    else
                        return l2;
                }
            }
        }

//        If the user has given no permission to access location return nothing:
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /** Determines whether one Location reading is better than the current Location fix
     * This code is taken from:
     * @see <a href="https://developer.android.com/guide/topics/location/strategies.html">https://developer.android.com/guide/topics/location/strategies.html</a>
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


}


