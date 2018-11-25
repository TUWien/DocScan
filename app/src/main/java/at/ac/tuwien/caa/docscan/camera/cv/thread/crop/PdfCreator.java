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

    public static void testOCR(final ArrayList<File> files) {
        final FirebaseVisionText[] ocrResults = new FirebaseVisionText[files.size()];
        for (final File file : files) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            int rotationInDegrees = getRotationInDegrees(file);
            bitmap = getRotatedBitmap(bitmap, rotationInDegrees);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            getTextRecognizer().processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText result) {
                            ocrResults[files.indexOf(file)] = result;
                            boolean f = true;
                            for (FirebaseVisionText r : ocrResults) {
                                if (r == null) {
                                    f = false;
                                }
                            }
                            if (f) {
                                test(files, ocrResults);
                            }
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

    }

    public static void test(ArrayList<File> files, FirebaseVisionText[] ocrResults) {
        File firstPage = files.get(0);
        int rotationInDegrees = getRotationInDegrees(firstPage);
        Bitmap firstPageBitmap = BitmapFactory.decodeFile(firstPage.getPath());
        firstPageBitmap = getRotatedBitmap(firstPageBitmap, rotationInDegrees);
        boolean landscapeFirst = firstPageBitmap.getWidth() > firstPageBitmap.getHeight();
        Rectangle firstPageSize = getPageSize(firstPageBitmap, landscapeFirst);
        String pdfName = "test.pdf";
        File outputFile = new File(getDocumentsDir(), pdfName);
        Document document = new Document(firstPageSize, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                rotationInDegrees = getRotationInDegrees(file);
                //add the original image to the pdf and set the DPI of it to 300
                Image image = Image.getInstance(file.getAbsolutePath());
                image.setRotationDegrees(-rotationInDegrees);
                image.scaleAbsolute(document.getPageSize().getWidth(), document.getPageSize().getHeight());
                image.setDpi(300, 300);
                document.add(image);


                if (ocrResults[i] != null) {
                    // the direct content where we write on
                    // directContentUnder instead of directContent, because then the text is in the background)
                    //PdfContentByte cb = writer.getDirectContentUnder();
                    PdfContentByte cb = writer.getDirectContent();
                    BaseFont bf = BaseFont.createFont();

                    //sort the result based on the y-Axis so that the markup order is correct
                    List<FirebaseVisionText.TextBlock> sortedBlocks = sortBlocks(ocrResults[i]);

                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    bitmap = getRotatedBitmap(bitmap, getRotationInDegrees(file));

                    for (TextBlock textBlock : sortedBlocks) {
                        cb = writer.getDirectContent();
                        float xleft = ((float) textBlock.getBoundingBox().left / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                        float xright = ((float) textBlock.getBoundingBox().right / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                        float xtop = ((float) textBlock.getBoundingBox().top / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                        float xbottom = ((float) textBlock.getBoundingBox().bottom / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                        Rectangle xrect = new Rectangle(xleft,
                                document.getPageSize().getHeight() - xbottom,
                                xright,
                                document.getPageSize().getHeight() - xtop);
                        xrect.setBorder(Rectangle.BOX);
                        xrect.setBorderWidth(2);
                        cb.rectangle(xrect);

                        //cb = writer.getDirectContentUnder();
                        for (FirebaseVisionText.Line line : textBlock.getLines()) {
                            //for (FirebaseVisionText.Element element : line.getElements()) {
                            // one FirebaseVisionText.Element corresponds to one word
                            // the rectangle we want to draw this word corresponds to the elements boundingBox
                            float left = ((float) line.getBoundingBox().left / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                            float right = ((float) line.getBoundingBox().right / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                            float top = ((float) line.getBoundingBox().top / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                            float bottom = ((float) line.getBoundingBox().bottom / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                            Rectangle rect = new Rectangle(left,
                                    document.getPageSize().getHeight() - bottom,
                                    right,
                                    document.getPageSize().getHeight() - top);
                            String drawText = line.getText();
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


                if (i < files.size() - 1) {
                    file = files.get(i + 1);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    Rectangle pageSize = getPageSize(bitmap, landscapeFirst);
                    document.setPageSize(pageSize);
                    document.newPage();
                }
            }

            document.close();
            Log.d(CLASS_NAME, "Document created at " + outputFile.getAbsolutePath());
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

    }

    public static Rectangle getPageSize(Bitmap bitmap, boolean landscape) {
        Rectangle pageSize;
        if (landscape) {
            //querformat
            float height = (PageSize.A4.getHeight() / bitmap.getWidth()) * bitmap.getHeight();
            pageSize = new Rectangle(PageSize.A4.getHeight(), height);
        } else {
            //hochformat
            float height = (PageSize.A4.getWidth() / bitmap.getWidth()) * bitmap.getHeight();
            pageSize = new Rectangle(PageSize.A4.getWidth(), height);
        }
        return pageSize;
    }

    private static Bitmap getRotatedBitmap(Bitmap bitmap, int rotationInDegrees) {
        Matrix matrix = new Matrix();
        if (rotationInDegrees != 0) {
            matrix.preRotate(rotationInDegrees);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    private static int getRotationInDegrees(File file) {
        //check if the image was rotated and rotate it accordingly
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return exifToDegrees(rotation);

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

    @NonNull
    private static List<FirebaseVisionText.TextBlock> sortBlocks(FirebaseVisionText ocrResult) {
        List<List<FirebaseVisionText.TextBlock>> sortedBlocks = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> biggestBlocks = new ArrayList<>();
        for (FirebaseVisionText.TextBlock block : ocrResult.getTextBlocks()) {
            if (sortedBlocks.isEmpty()) {
                List<FirebaseVisionText.TextBlock> blocks = new ArrayList<>();
                blocks.add(block);
                biggestBlocks.add(block);
                sortedBlocks.add(blocks);
            } else {
                boolean added = false;
                for (TextBlock checkBlock : biggestBlocks) {
                    if (block.getBoundingBox().centerX() > checkBlock.getBoundingBox().left &&
                            block.getBoundingBox().centerX() < checkBlock.getBoundingBox().right) {
                        sortedBlocks.get(biggestBlocks.indexOf(checkBlock)).add(block);
                        if (block.getBoundingBox().width() > checkBlock.getBoundingBox().width()) {
                            biggestBlocks.set(biggestBlocks.indexOf(checkBlock), block);
                        }
                        added = true;
                    }
                }
                if (!added){
                    List<FirebaseVisionText.TextBlock> blocks = new ArrayList<>();
                    blocks.add(block);
                    biggestBlocks.add(block);
                    sortedBlocks.add(blocks);
                }
            }
        }
        for (List<TextBlock> textBlocks : sortedBlocks){
            sortedBlocks.set(sortedBlocks.indexOf(textBlocks), sortColumn(textBlocks));
        }
        List<TextBlock> sortedRows = sortRows(biggestBlocks);
        List<List<FirebaseVisionText.TextBlock>> sorted = (List<List<TextBlock>>) ((ArrayList<List<TextBlock>>) sortedBlocks).clone();
        for (List<TextBlock> blocks : sortedBlocks) {
            int i = sortedBlocks.indexOf(blocks);
            int j = sortedRows.indexOf(biggestBlocks.get(i));
            sorted.add(j, blocks);
        }
        List<TextBlock> result = new ArrayList<>();
        for (List<TextBlock> textBlocks : sorted){
            result.addAll(textBlocks);
        }
        return result;
    }

    @NonNull
    private static List<TextBlock> sortColumn(List<TextBlock> result) {
        List<FirebaseVisionText.TextBlock> sortedBlocks = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : result) {
            if (sortedBlocks.isEmpty()) {
                sortedBlocks.add(textBlock);
            } else {
                int i = 0;
                while (i < sortedBlocks.size()) {
                    if (textBlock.getCornerPoints()[0].y > sortedBlocks.get(i).getCornerPoints()[0].y) {
                        i++;
                    } else {
                        break;
                    }
                }
                sortedBlocks.add(i, textBlock);
            }
        }
        return sortedBlocks;
    }

    @NonNull
    private static List<TextBlock> sortRows(List<TextBlock> result) {
        List<FirebaseVisionText.TextBlock> sortedBlocks = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : result) {
            if (sortedBlocks.isEmpty()) {
                sortedBlocks.add(textBlock);
            } else {
                int i = 0;
                while (i < sortedBlocks.size()) {
                    if (textBlock.getBoundingBox().centerX() > sortedBlocks.get(i).getBoundingBox().centerX()) {
                        i++;
                    } else {
                        break;
                    }
                }
                sortedBlocks.add(i, textBlock);
            }
        }
        return sortedBlocks;
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
