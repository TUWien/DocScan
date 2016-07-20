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
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;

import org.opencv.android.OpenCVLoader;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

public class MainActivity extends AppCompatActivity implements NativeWrapper.CVCallback, TaskTimer.TimerCallbacks {

    public static final int STACK_TRACE_ELEMENT_NUM = 2;

    private static String TAG = "MainActivity";

    private static final int PERMISSION_CAMERA = 0;
    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";

    private static boolean mIsDebugViewEnabled = false;

    private static Activity mActivity;
    private OverlayView mOverlayView;
    private CameraView mCameraView;
    private DrawView mDrawView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;

    // Used for stopping the time of 'heavy' tasks:
    private TaskTimer mTaskTimer;


    static {

        boolean init = OpenCVLoader.initDebug();

        if (init) {
            System.loadLibrary("docscan-native");
        }
        int b = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mActivity = this;

        setupNavigationDrawer();

        mCameraView = (CameraView) findViewById(R.id.camera_view);
        mDrawView = (DrawView) findViewById(R.id.draw_view);
        mDrawView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mOverlayView = (OverlayView) findViewById(R.id.overlay_view);

//        mOverlayView.setCameraView(mCameraView);
//        mOverlayView.setDrawView(mDrawView);
        mOverlayView.setViews(mCameraView, mDrawView);

        // This must be called after the CameraView has been created:
        requestCameraPermission();

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);


        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;


    }

    @Override
    protected void onPause() {

        super.onPause();
        Log.d(TAG, "pausing draw view...");
        mDrawView.pause();
        Log.d(TAG, "pausing camera view...");
        mCameraView.pause();

        Log.d(TAG, "all views are paused...");

    }

    @Override
    protected void onResume() {

        super.onResume();
        Log.d(TAG, "resuming camera view...");
        mCameraView.resume();
        Log.d(TAG, "resuming draw view...");
        mDrawView.resume();

        Log.d(TAG, "all views are resumed...");

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }


    private void requestCameraPermission() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        }
        else
            mCameraView.giveCameraPermission();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
//        initCamera();
        switch (requestCode) {
            case PERMISSION_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraView.giveCameraPermission();
                }
            }

        }
    }

    // 0 is regular landscape
    // 90 is portrait
    // 180 is flipped landscape
    public static int getCameraDisplayOrientation() {


        int cameraId = 0;

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = mActivity.getWindowManager().getDefaultDisplay()
                .getRotation();

        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;

    }

    // ================= start: CALLBACKS called from native files =================

    @Override
    public void onFocusMeasured(Patch[] patches) {

        mOverlayView.setFocusPatches(patches);

    }

    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects) {

        mOverlayView.setDkPolyRects(dkPolyRects);

    }

    // Debug routine that is only invoked if debug view is shown:
    @Override
    public void onFocusMeasured(Patch[] patches, final long time) {

        if (mOverlayView != null)
            mOverlayView.setFocusPatches(patches);

        if (mDebugViewFragment != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDebugViewFragment.setFocusMeasureTime(time);
                }
            });
        }

    }

    // Debug routine that is only invoked if debug view is shown:
    @Override
    public void onPageSegmented(DkPolyRect[] dkPolyRects, long time) {

        mOverlayView.setDkPolyRects(dkPolyRects);

    }

    // ================= end: CALLBACKS called from native files =================


    // ================= start: CALLBACKS invoking TaskTimer =================

    @Override
    public void onTimerStarted(int senderId) {

        if (mTaskTimer == null)
            return;

        mTaskTimer.startTaskTimer(senderId);


    }

    @Override
    public void onTimerStopped(final int senderId) {

        if (mTaskTimer == null)
            return;

        final long timePast = mTaskTimer.getTaskTime(senderId);

        // Normally the timer callback should just be called if the debug view is visible:
        if (mIsDebugViewEnabled) {

            if (mDebugViewFragment != null) {
                // The update of the UI elements must be done from the UI thread:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDebugViewFragment.setTimeText(senderId, timePast);
                    }
                });
            }

        }


    }

    // ================= end: CALLBACKS invoking TaskTimer =================


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }



        switch (item.getItemId()) {

            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

        }


        return super.onOptionsItemSelected(item);
    }


    public static boolean isDebugViewEnabled() {

        return mIsDebugViewEnabled;

    }

    private void setupNavigationDrawer() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        NavigationView mDrawer = (NavigationView) findViewById(R.id.left_drawer);
        setupDrawerContent(mDrawer);



        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Menu menu = mDrawer.getMenu();
        MenuItem item = menu.findItem(R.id.action_show_debug_view);

        if (mIsDebugViewEnabled)
            item.setTitle(R.string.hide_debug_view_text);
        else
            item.setTitle(R.string.show_debug_view_text);

    }


    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(

                new NavigationView.OnNavigationItemSelectedListener() {

                    @Override

                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        selectDrawerItem(menuItem);

                        return true;

                    }

                });

    }

    private void selectDrawerItem(MenuItem menuItem) {

        switch(menuItem.getItemId()) {

            case R.id.action_show_debug_view:

                // Create the debug view - if it is not already created:
                if (mDebugViewFragment == null) {
                    mDebugViewFragment = new DebugViewFragment();
                }

                // Show the debug view:
                if (getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT) == null) {
                    mIsDebugViewEnabled = true;
                    menuItem.setTitle(R.string.hide_debug_view_text);
                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
                }
                // Hide the debug view:
                else {
                    mIsDebugViewEnabled = false;
                    menuItem.setTitle(R.string.show_debug_view_text);
                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
                }



        }

        mDrawerLayout.closeDrawers();

    }

}
