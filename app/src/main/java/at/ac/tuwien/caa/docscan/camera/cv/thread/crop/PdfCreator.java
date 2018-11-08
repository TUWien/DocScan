package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

public class PdfCreator {

    private static FirebaseVisionTextRecognizer textRecognizer;


    private static FirebaseVisionTextRecognizer getTextRecognizer(){
        if (textRecognizer == null){
            textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        return textRecognizer;
    }
}
