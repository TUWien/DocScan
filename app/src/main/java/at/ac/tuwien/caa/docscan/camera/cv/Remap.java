package at.ac.tuwien.caa.docscan.camera.cv;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
//import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
class Remap {

    private Mat mapX = new Mat();
    private Mat mapY = new Mat();
    private Mat dst = new Mat();
    private int ind = 0;
    private void updateMap() {
        float buffX[] = new float[(int) (mapX.total() * mapX.channels())];
        mapX.get(0, 0, buffX);
        float buffY[] = new float[(int) (mapY.total() * mapY.channels())];
        mapY.get(0, 0, buffY);
        for (int i = 0; i < mapX.rows(); i++) {
            for (int j = 0; j < mapX.cols(); j++) {
                switch (ind) {
                    case 0:
                        if( j > mapX.cols()*0.25 && j < mapX.cols()*0.75 && i > mapX.rows()*0.25 && i < mapX.rows()*0.75 ) {
                            buffX[i*mapX.cols() + j] = 2*( j - mapX.cols()*0.25f ) + 0.5f;
                            buffY[i*mapY.cols() + j] = 2*( i - mapX.rows()*0.25f ) + 0.5f;
                        } else {
                            buffX[i*mapX.cols() + j] = 0;
                            buffY[i*mapY.cols() + j] = 0;
                        }
                        break;
                    case 1:
                        buffX[i*mapX.cols() + j] = j;
                        buffY[i*mapY.cols() + j] = mapY.rows() - i;
                        break;
                    case 2:
                        buffX[i*mapX.cols() + j] = mapY.cols() - j;
                        buffY[i*mapY.cols() + j] = i;
                        break;
                    case 3:
                        buffX[i*mapX.cols() + j] = mapY.cols() - j;
                        buffY[i*mapY.cols() + j] = mapY.rows() - i;
                        break;
                    default:
                        break;
                }
            }
        }
        mapX.put(0, 0, buffX);
        mapY.put(0, 0, buffY);
        ind = (ind+1) % 4;
    }

    public void run(Mat src) {

        long t0 = Core.getTickCount();
        Mat input = Mat.zeros(4000, 3000, src.type());
        mapX = new Mat(input.size(), CvType.CV_32F);
        mapY = new Mat(input.size(), CvType.CV_32F);
        long t1 = Core.getTickCount();
        double secs = (t1-t0) / Core.getTickFrequency();
        Log.d("Remap", "Java init took: " + secs + " seconds");

        t0 = Core.getTickCount();
        updateMap();
        t1 = Core.getTickCount();
        secs = (t1-t0) / Core.getTickFrequency();
        Log.d("Remap", "Java updateMap took: " + secs + " seconds");

        t0 = Core.getTickCount();
        Imgproc.remap(input, dst, mapX, mapY, Imgproc.INTER_LANCZOS4);
        t1 = Core.getTickCount();
        secs = (t1-t0) / Core.getTickFrequency();
        Log.d("Remap", "Java remap took: " + secs + " seconds");

    }

//    public void run(String[] args) {
//        String filename = args.length > 0 ? args[0] : "../data/chicky_512.png";
//        Mat src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_COLOR);
//        if (src.empty()) {
//            System.err.println("Cannot read image: " + filename);
//            System.exit(0);
//        }
//        mapX = new Mat(src.size(), CvType.CV_32F);
//        mapY = new Mat(src.size(), CvType.CV_32F);
//        final String winname = "Remap demo";
////        HighGui.namedWindow(winname, HighGui.WINDOW_AUTOSIZE);
//        for (;;) {
//            updateMap();
//            Imgproc.remap(src, dst, mapX, mapY, Imgproc.INTER_LINEAR);
//            HighGui.imshow(winname, dst);
//            if (HighGui.waitKey(1000) == 27) {
//                break;
//            }
//        }
//        System.exit(0);
//    }
}
//public class RemapDemo {
//    public static void main(String[] args) {
//        // Load the native OpenCV library
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        new Remap().run(args);
//    }
//}