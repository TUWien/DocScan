package at.ac.tuwien.caa.docscan;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;

    private static final int PERMISSION_CAMERA = 0;
    private static Activity mActivity;


    static {

        boolean init = OpenCVLoader.initDebug();

        if (init) {
//            System.loadLibrary("wrapper");
            System.loadLibrary("opencv-jni");
//            System.loadLibrary("openCVLibrary310");
        }
        int b = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreview = new CameraPreview(this);
        setContentView(mPreview);


//        setContentView(R.layout.activity_main);
//
//        ImageView i = new ImageView(this);
//
//        SurfaceView camView = new SurfaceView(this);
//        SurfaceHolder camHolder = camView.getHolder();
//        int PreviewSizeWidth = 640;
//        int PreviewSizeHeight= 480;
//
//        CameraPreview2 camPreview = new CameraPreview2(640, 480, i);
//
//        camHolder.addCallback(camPreview);
//        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//
//        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.camera_preview);
//        mainLayout.addView(camView, new LayoutParams(PreviewSizeWidth, PreviewSizeHeight));
//        mainLayout.addView(i, new LayoutParams(PreviewSizeWidth, PreviewSizeHeight));

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

        super.onResume();
        mActivity = this;

        mPreview.setActivity(this);



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
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
        }
        else
            mPreview.openCamera();
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
                    mPreview.openCamera();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public static void setCameraDisplayOrientation(android.hardware.Camera camera) {


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
        camera.setDisplayOrientation(result);
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
