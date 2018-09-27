/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   21. July 2016
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

package at.ac.tuwien.caa.docscan.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import at.ac.tuwien.caa.docscan.R;


/**
 * Activity called after the app is started. This activity is responsible for requesting the camera
 * permission. If the permission is given the CameraActivity is started via an intent.
 * Based on this example: <a href="https://github.com/googlesamples/android-RuntimePermissionsBasic">android-RuntimePermissionsBasic
 </a>
 */
public class StartActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSION_CAMERA = 0;
    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_container_view);
        mLayout = findViewById(R.id.main_frame_layout);
//        showCameraPreview();

        askForPermissions();

    }

    /**
     * Asks for permissions that are really needed. If they are not given, the app is unusable.
     */
    private void askForPermissions() {

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        else
            startCamera();

    }

    /**
     * Ask for multiple permissions. Taken from:
     * https://stackoverflow.com/a/34343101/9827698
     * @param context
     * @param permissions
     * @return
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    private void showPermissionRequiredAlert(String alertText) {


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertText += "\n" + getResources().getString(R.string.start_permission_retry_text);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.start_permission_title)
                .setPositiveButton(R.string.start_confirm_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)  {
                        askForPermissions();
                    }
                })
                .setNegativeButton(R.string.start_cancel_button_text, null)
                .setCancelable(true)
                .setMessage(alertText);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        boolean permissionsGiven = true;

        for (int i = 0; i < permissions.length; i++) {

            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
//                These are the permissions that are definitely needed:
                if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    showPermissionRequiredAlert(getResources().getString(
                            R.string.start_permission_camera_text));
                    permissionsGiven = false;
                }
                else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPermissionRequiredAlert(getResources().getString(
                            R.string.start_permission_storage_text));
                    permissionsGiven = false;
                }

            }
        }

        if (permissionsGiven)
            startCamera();

    }
//
//    private void showCameraPreview() {
//        // BEGIN_INCLUDE(startCamera)
//        // Check if the Camera permission has been granted
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_GRANTED) {
//            // Permission is already available, start camera preview
//            Snackbar.make(mLayout,
//                    "Camera permission is available. Starting preview.",
//                    Snackbar.LENGTH_SHORT).show();
//
//            startCamera();
//
//        } else {
//            // Permission is missing and must be requested.
//            requestCameraPermission();
//        }
//        // END_INCLUDE(startCamera)
//    }

//
//
//    private void requestCameraPermission() {
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
//        }
//        else
//            startCamera();
//
//    }

    private void startCamera() {

        Intent intent = new Intent(this, CameraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();

    }


}
