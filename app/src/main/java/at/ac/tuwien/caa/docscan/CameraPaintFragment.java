package at.ac.tuwien.caa.docscan;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by fabian on 19.10.2016.
 */
public class CameraPaintFragment extends Fragment {

    private Camera mCamera;
    private Camera.CameraInfo mInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        TempCallback c = (TempCallback) getActivity();
//
//        Camera camera = getCameraInstance(0);
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(0, info);
//
//        // Get the rotation of the screen to adjust the preview image accordingly.
//        int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//
//        c.onCameraLoaded(camera, info, displayRotation);
//
//        mCamera = camera;
//        mInfo = info;
//
//
//        // retain this fragment
//        setRetainInstance(true);
    }

    private Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
//            // Camera is not available (in use or does not exist)
//            Toast.makeText(this, "Camera " + cameraId + " is not available: " + e.getMessage(),
//                    Toast.LENGTH_SHORT).show();
        }
        return c; // returns null if camera is unavailable
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // retain this fragment
//        setRetainInstance(true);

        View view = inflater.inflate(R.layout.camera_paint_view, container, false);

//        CameraPreview cameraPreview = (CameraPreview) view.findViewById(R.id.camera_view);
//        int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//        cameraPreview.setCamera(mCamera, mInfo, displayRotation);

//        Camera camera = getCameraInstance(0);
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(0, info);
//        cameraPreview.setCamera(camera, info, displayRotation);



        // Inflate the layout for this fragment
        return view;
    }





//    public interface TempCallback {
//
//        void onCameraLoaded(Camera camera, Camera.CameraInfo info, int displayRotation);
//
//    }


}
