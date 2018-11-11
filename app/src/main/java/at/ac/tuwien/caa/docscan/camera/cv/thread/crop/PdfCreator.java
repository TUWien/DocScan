package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfCreator {

    private static FirebaseVisionTextRecognizer textRecognizer;



    private static void getText(FirebaseVisionImage image){
        getTextRecognizer().processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        String text = orderText(result);
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
    }

    private static String orderText(FirebaseVisionText text){
        StringBuilder textFix = new StringBuilder();
        List<FirebaseVisionText.TextBlock> textBlocks = text.getTextBlocks();
        List<FirebaseVisionText.Line> lines = new ArrayList<>();
        List<FirebaseVisionText.Line> sortedLines = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : textBlocks){
            lines.addAll(textBlock.getLines());
        }
        for (FirebaseVisionText.Line line : lines){
            if (sortedLines.isEmpty()){
                sortedLines.add(line);
            } else {
                int i = 0;
                while (i < sortedLines.size() && line.getCornerPoints()[0].y > sortedLines.get(i).getCornerPoints()[0].y){
                    i++;
                }
                sortedLines.add(i, line);
            }
        }
        for (FirebaseVisionText.Line line : sortedLines){
            textFix.append(line.getText());
        }
        return textFix.toString();
    }

    private static FirebaseVisionImage getVisionImage(File file){
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        return FirebaseVisionImage.fromBitmap(bitmap);
    }


    private static FirebaseVisionTextRecognizer getTextRecognizer(){
        if (textRecognizer == null){
            textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        return textRecognizer;
    }
}
