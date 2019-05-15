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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.FirebaseApp;

import java.text.SimpleDateFormat;
import java.util.Date;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.DocumentStorage;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.intro.IntroFragment;
import at.ac.tuwien.caa.docscan.ui.intro.ZoomOutPageTransformer;

import static at.ac.tuwien.caa.docscan.ui.AboutActivity.KEY_SHOW_INTRO;


/**
 * Activity called after the app is started. This activity is responsible for requesting the camera
 * permission. If the permission is given the CameraActivity is started via an intent.
 * Based on this example: <a href="https://github.com/googlesamples/android-RuntimePermissionsBasic">android-RuntimePermissionsBasic
 </a>
 */
public class StartActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private AlertDialog mAlertDialog;
    private static final String KEY_FIRST_START_DATE = "KEY_FIRST_START_DATE";
    private static final int CREATE_DOCUMENT = 0;
    private boolean mDocumentCreateRequest = false;
    private static final String CLASS_NAME = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(KEY_SHOW_INTRO)) {
            if (getIntent().getBooleanExtra(KEY_SHOW_INTRO, false)) {
                startIntro();
                return;
            }
        }

        logFirstAppStart();

        //initialize Firebase
        FirebaseApp a = FirebaseApp.initializeApp(this);
        if (a == null || a.getOptions().getApiKey().isEmpty())
            Log.d(CLASS_NAME, getString(R.string.start_firebase_not_auth_text));

        int lastInstalledVersion = Settings.getInstance().loadIntKey(this, Settings.SettingEnum.INSTALLED_VERSION_KEY);
//        The user has already started the app:
        if (lastInstalledVersion != Settings.NO_ENTRY)
            startAppNoIntro();
        else
            startIntro();

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

//        In case the user created a document, close the intro, the CameraActivity is already started:
        if (requestCode == CREATE_DOCUMENT && resultCode == RESULT_OK)
            finish();

    }

    private void startIntro() {

        //        We save the version here, so that the intro is shown just once also in case the intro has stopped:
        int currentVersion = BuildConfig.VERSION_CODE;
        Settings.getInstance().saveIntKey(this, Settings.SettingEnum.INSTALLED_VERSION_KEY, currentVersion);

        setContentView(R.layout.activity_intro);

        final PageSlideAdapter pagerAdapter = new PageSlideAdapter(getSupportFragmentManager());
        final ViewPager pager = findViewById(R.id.intro_viewpager);
//        pager.setPageTransformer(true, new DepthPageTransformer());
        pager.setPageTransformer(true, new ZoomOutPageTransformer());
        pager.setAdapter(pagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(pager, true);

        AppCompatButton skipButton = findViewById(R.id.intro_skip_button);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    startAppNoIntro();
            }
        });


    }


    public void createDocument(View view) {

        mDocumentCreateRequest = true;
        askForPermissions();

    }

    public void triggerFlash(View view) {

        CheckBox checkBox = (CheckBox) view;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.key_flash_series_mode), checkBox.isChecked());
        editor.commit();

    }


    private class PageSlideAdapter extends FragmentPagerAdapter {

//        private IntroFragment mCurrentFragment;

        private PageSlideAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return IntroFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 5;
        }

    }


    private void startAppNoIntro() {

        setContentView(R.layout.main_container_view);
        askForPermissions();

    }


    private void logFirstAppStart() {
        //        Log the first start of the app:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref != null) {
            String firstStart = sharedPref.getString(KEY_FIRST_START_DATE, null);
            if (firstStart == null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                Log in the app:
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(KEY_FIRST_START_DATE, timeStamp);
                editor.commit();
//                Save in Crashlytics:
                Crashlytics.setString(KEY_FIRST_START_DATE, timeStamp);
            }
        }
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
        else {
//            The user wants to create a document and has already given the permissions:
            if (mDocumentCreateRequest) {
                Intent intent = new Intent(this, CreateDocumentActivity.class);
                startActivityForResult(intent, CREATE_DOCUMENT);
            }
            else
                startCamera();
        }


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

            return true;
        }

        return false;

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
//                        Restart the activity:
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);

                    }
                })
                .setNegativeButton(R.string.start_cancel_button_text, null)
                .setCancelable(true)
                .setMessage(alertText);

        // create alert dialog
        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();

        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

//        Note: we ignore here the values passed in grantResults and ask directly for
//        checkSelfPermissions, although this is a bad style. The reason for this is that if the app
//        is killed by the system, the grantResults provided are not correct, because the
//        permissions are asked again:

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)) {
            showPermissionRequiredAlert(getResources().getString(
                    R.string.start_permission_camera_text));
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            showPermissionRequiredAlert(getResources().getString(
                    R.string.start_permission_storage_text));
            return;
        }

        if (mDocumentCreateRequest) {
            Intent intent = new Intent(this, CreateDocumentActivity.class);
            startActivityForResult(intent, CREATE_DOCUMENT);
        }
        else
    //        If CAMERA and WRITE_EXTERNAL_STORAGE permissions are given start the camera, the GPS
    //        permissions are not required:
            startCamera();

    }

    private void startCamera() {

        DocumentStorage.loadJSON(this);
        SyncStorage.loadJSON(this);

        Intent intent = new Intent(this, CameraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();

    }


}
