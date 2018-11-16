package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionText.TextBlock;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfCreator {

    private static final String CLASS_NAME = "PdfCreator";
    private static FirebaseVisionTextRecognizer textRecognizer;

    public static void createPdfWithOCR(File file) {
        recognizeText(file);
    }

    public static void createPdf(File file) {
        createPdfFromOCR(null, file);
    }


    private static void recognizeText(final File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotationInDegrees = exifToDegrees(rotation);
        Matrix matrix = new Matrix();
        if (rotation != 0) {
            matrix.preRotate(rotationInDegrees);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        getTextRecognizer().processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        createPdfFromOCR(result, file);
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Log.e(CLASS_NAME, "Failed to recognize text\n" + e.getMessage());
                            }
                        });
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private static void createPdfFromOCR(FirebaseVisionText result, File imageFile) {
        //get the Bitmap from the file to create document with correct measurements
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());

        try {
            //check if the image was rotated and rotate it accordingly
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);
            Matrix matrix = new Matrix();
            if (rotation != 0) {
                matrix.preRotate(rotationInDegrees);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            //resize the pdf to the image
            Rectangle pageSize;
            if (bitmap.getWidth() > bitmap.getHeight()) {
                //querformat
                float height = (PageSize.A4.getHeight() / bitmap.getWidth()) * bitmap.getHeight();
                pageSize = new Rectangle(PageSize.A4.getHeight(), height);
            } else {
                //hochformat
                float height = (PageSize.A4.getWidth() / bitmap.getWidth()) * bitmap.getHeight();
                pageSize = new Rectangle(PageSize.A4.getWidth(), height);
            }
            Document document = new Document(pageSize, 0, 0, 0, 0);

            //create the pdf with the name of the image file in /storage/emulated/0/Documents/
            String pdfName = imageFile.getName().split("\\.")[0] + ".pdf";
            File outputFile = new File(getDocumentsDir(), pdfName);


            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            //add the original image to the pdf and set the DPI of it to 300
            Image image = Image.getInstance(imageFile.getAbsolutePath());
            image.setRotationDegrees(-rotationInDegrees);
            image.scaleToFit(pageSize);
            image.setDpi(300, 300);
            document.add(image);

            if (result != null) {
                // the direct content where we write on
                // directContentUnder instead of directContent, because then the text is in the background)
                PdfContentByte cb = writer.getDirectContentUnder();
                BaseFont bf = BaseFont.createFont();

                //sort the result based on the y-Axis so that the markup order is correct
                List<FirebaseVisionText.Line> sortedLines = sortLines(result);

                //for (TextBlock textBlock : result.getTextBlocks()) {
                for (FirebaseVisionText.Line line : sortedLines) {
                    for (FirebaseVisionText.Element element : line.getElements()) {
                        // one FirebaseVisionText.Element corresponds to one word
                        // the rectangle we want to draw this word corresponds to the elements boundingBox
                        float left = ((float) element.getBoundingBox().left / (float) bitmap.getWidth()) * pageSize.getWidth();
                        float right = ((float) element.getBoundingBox().right / (float) bitmap.getWidth()) * pageSize.getWidth();
                        float top = ((float) element.getBoundingBox().top / (float) bitmap.getHeight()) * pageSize.getHeight();
                        float bottom = ((float) element.getBoundingBox().bottom / (float) bitmap.getHeight()) * pageSize.getHeight();
                        Rectangle rect = new Rectangle(left,
                                pageSize.getHeight() - bottom,
                                right,
                                pageSize.getHeight() - top);
                        String drawText = element.getText();
                        // try to get max font size that fit in rectangle
                        int textHeightInGlyphSpace = bf.getAscent(drawText) - bf.getDescent(drawText);
                        float fontSize = 1000f * rect.getHeight() / textHeightInGlyphSpace;
                        Phrase phrase = new Phrase(drawText, new Font(bf, fontSize));
                        // write the text on the pdf
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, phrase,
                                // center horizontally
                                (rect.getLeft() + rect.getRight()) / 2,
                                // shift baseline based on descent
                                rect.getBottom() - bf.getDescentPoint(drawText, fontSize),
                                0);
                    }
                }
            }
            //}

            document.close();
            Log.d(CLASS_NAME, "Document created at " + outputFile.getAbsolutePath());
        } catch (DocumentException | IOException e) {
            // Task failed with an exception
            Log.e(CLASS_NAME, "Failed to create document\n" + e.getMessage());
        }
    }

    @NonNull
    private static List<FirebaseVisionText.Line> sortLines(FirebaseVisionText result) {
        List<FirebaseVisionText.Line> lines = new ArrayList<>();
        List<FirebaseVisionText.Line> sortedLines = new ArrayList<>();
        for (TextBlock textBlock : result.getTextBlocks()) {
            lines.addAll(textBlock.getLines());
        }
        for (FirebaseVisionText.Line line : lines) {
            if (sortedLines.isEmpty()) {
                sortedLines.add(line);
            } else {
                int i = 0;
                while (i < sortedLines.size()) {
                    if (line.getCornerPoints()[0].y > sortedLines.get(i).getCornerPoints()[0].y) {
                        i++;
                    } else {
                        break;
                    }
                }
                sortedLines.add(i, line);
            }
        }
        return sortedLines;
    }

    //in case we need the text sorted by y-axis
    private static String orderText(FirebaseVisionText text) {
        StringBuilder textFix = new StringBuilder();
        List<TextBlock> textBlocks = text.getTextBlocks();
        List<FirebaseVisionText.Line> lines = new ArrayList<>();
        List<FirebaseVisionText.Line> sortedLines = new ArrayList<>();
        for (TextBlock textBlock : textBlocks) {
            lines.addAll(textBlock.getLines());
        }
        for (FirebaseVisionText.Line line : lines) {
            if (sortedLines.isEmpty()) {
                sortedLines.add(line);
            } else {
                int i = 0;
                while (i < sortedLines.size() && line.getCornerPoints()[0].y > sortedLines.get(i).getCornerPoints()[0].y) {
                    i++;
                }
                sortedLines.add(i, line);
            }
        }
        for (FirebaseVisionText.Line line : sortedLines) {
            textFix.append(line.getText());
        }
        return textFix.toString();
    }

    private static File getDocumentsDir() {
        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
        if (!docsFolder.exists()) {
            docsFolder.mkdir();
        }
        return docsFolder;
    }

    private static FirebaseVisionTextRecognizer getTextRecognizer() {
        if (textRecognizer == null) {
            textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        return textRecognizer;
    }
}
