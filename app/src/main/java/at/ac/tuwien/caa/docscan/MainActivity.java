package at.ac.tuwien.caa.docscan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;

    private static final int PERMISSION_CAMERA = 0;


    static {

        boolean init = OpenCVLoader.initDebug();

        if (init) {
            System.loadLibrary("wrapper");
//            System.loadLibrary("openCVLibrary310");
        }
        int b = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        mPreview = new CameraPreview(this);
        setContentView(mPreview);

        requestPermission();
//        initCamera();

    }

    @Override
    protected void onPause() {

        super.onPause();
        mPreview.releaseCamera();

    }

    @Override
    protected void onResume() {
//        Log.i(TAG, "onResume");

        mPreview.openCamera();
        super.onResume();

    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void requestPermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_CAMERA);
        }
//        else
//            mPreview.openCamera();
//        initCamera();

//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                Manifest.permission.CAMERA)) {
//
//        }

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
//                    initCamera();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

//    private void initCamera() {
//
//        // Create an instance of Camera
////        mCamera = Camera.open(0);
//
//        // Create our Preview view and set it as the content of our activity.
//        mPreview = new CameraPreview(this);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(mPreview);
//
//    }
}
