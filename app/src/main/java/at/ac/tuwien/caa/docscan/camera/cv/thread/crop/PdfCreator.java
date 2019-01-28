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
import com.itextpdf.text.BaseColor;
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

import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.MESSAGE_CREATED_DOCUMENT;

public class PdfCreator {

    private static final String CLASS_NAME = "PdfCreator";
    private static FirebaseVisionTextRecognizer textRecognizer;

    public static void createPdfWithoutOCR(String documentName, final ArrayList<File> files, CropRunnable cropRunnable) {
        createPdf(documentName, files, null, cropRunnable);
    }

    public static void createPdfWithOCR(final String documentName, final ArrayList<File> files, final CropRunnable cropRunnable) {
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
                                createPdf(documentName, files, ocrResults, cropRunnable);
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

    public static void createPdf(String documentName, ArrayList<File> files, FirebaseVisionText[] ocrResults, CropRunnable cropRunnable) {
        File firstPage = files.get(0);
        int rotationInDegrees = getRotationInDegrees(firstPage);
        Bitmap firstPageBitmap = BitmapFactory.decodeFile(firstPage.getPath());
        firstPageBitmap = getRotatedBitmap(firstPageBitmap, rotationInDegrees);
        boolean landscapeFirst = firstPageBitmap.getWidth() > firstPageBitmap.getHeight();
        Rectangle firstPageSize = getPageSize(firstPageBitmap, landscapeFirst);
        String pdfName = documentName + ".pdf";
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


                if (ocrResults != null && ocrResults[i] != null) {
                    // the direct content where we write on
                    // directContentUnder instead of directContent, because then the text is in the background)
                    //PdfContentByte cb = writer.getDirectContentUnder();
                    PdfContentByte cb = writer.getDirectContentUnder();
                    BaseFont bf = BaseFont.createFont();

                    //sort the result based on the y-Axis so that the markup order is correct
                    List<List<FirebaseVisionText.TextBlock>> sortedBlocks = sortBlocks(ocrResults[i]);


                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    bitmap = getRotatedBitmap(bitmap, getRotationInDegrees(file));

                    //int j = 0;
                    for (List<TextBlock> column : sortedBlocks) {
                        // Following lines are for debug purposes (to show the recognized text blocks
                        //    j++;
                        //for (TextBlock textBlock : column) {
                        //    cb = writer.getDirectContent();
                        //    float xleft = ((float) textBlock.getBoundingBox().left / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                        //    float xright = ((float) textBlock.getBoundingBox().right / (float) bitmap.getWidth()) * document.getPageSize().getWidth();
                        //    float xtop = ((float) textBlock.getBoundingBox().top / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                        //    float xbottom = ((float) textBlock.getBoundingBox().bottom / (float) bitmap.getHeight()) * document.getPageSize().getHeight();
                        //    Rectangle xrect = new Rectangle(xleft,
                        //            document.getPageSize().getHeight() - xbottom,
                        //            xright,
                        //            document.getPageSize().getHeight() - xtop);
                        //    xrect.setBorder(Rectangle.BOX);
                        //    xrect.setBorderWidth(2);
                        //    cb.rectangle(xrect);
                        //    Phrase phrase = new Phrase(j + "", new Font(bf, 20f));
                        //    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, phrase,
                        //            // center horizontally
                        //            (xrect.getLeft() + xrect.getRight()) / 2,
                        //            // shift baseline based on descent
                        //            xrect.getBottom() - bf.getDescentPoint(j + "", 20f),
                        //            0);
                        //}

                        for (FirebaseVisionText.Line line : sortLinesInColumn(column)) {
                            // one FirebaseVisionText.Line corresponds to one line
                            // the rectangle we want to draw this line corresponds to the lines boundingBox
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
                            while (bf.getWidthPoint(drawText, fontSize) < rect.getWidth()) {
                                fontSize++;
                            }
                            while (bf.getWidthPoint(drawText, fontSize) > rect.getWidth()) {
                                fontSize = fontSize - 0.1f;
                            }
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
            cropRunnable.mCropTask.handleState(MESSAGE_CREATED_DOCUMENT);
        } catch (DocumentException | IOException e) {
            Log.e(CLASS_NAME, "Failed to create document: " + e.getMessage());
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
    private static List<List<FirebaseVisionText.TextBlock>> sortBlocks(FirebaseVisionText ocrResult) {
        List<List<FirebaseVisionText.TextBlock>> sortedBlocks = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> biggestBlocks = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> blocksSortedByWidth = sortByWidth(ocrResult.getTextBlocks());
        for (FirebaseVisionText.TextBlock block : blocksSortedByWidth) {
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
                        break;
                    }
                }
                if (!added) {
                    List<FirebaseVisionText.TextBlock> blocks = new ArrayList<>();
                    blocks.add(block);
                    int i = 0;
                    while (i < biggestBlocks.size()) {
                        if (block.getBoundingBox().centerX() > biggestBlocks.get(i).getBoundingBox().centerX()) {
                            i++;
                        } else {
                            break;
                        }
                    }
                    biggestBlocks.add(i, block);
                    sortedBlocks.add(i, blocks);
                }
            }
        }
        for (List<TextBlock> textBlocks : sortedBlocks) {
            sortedBlocks.set(sortedBlocks.indexOf(textBlocks), textBlocks);
        }
        return sortedBlocks;
    }

    @NonNull
    private static List<TextBlock> sortByWidth(List<TextBlock> result) {
        List<FirebaseVisionText.TextBlock> sortedBlocks = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : result) {
            if (sortedBlocks.isEmpty()) {
                sortedBlocks.add(textBlock);
            } else {
                int i = 0;
                while (i < sortedBlocks.size()) {
                    if (textBlock.getBoundingBox().width() < sortedBlocks.get(i).getBoundingBox().width()) {
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
    private static List<FirebaseVisionText.Line> sortLinesInColumn(List<TextBlock> result) {
        List<FirebaseVisionText.Line> sortedLines = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : result) {
            for (FirebaseVisionText.Line line : textBlock.getLines())
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

    public static File getDocumentsDir() {
        File docsFolder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            docsFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOCUMENTS);
        } else {
            docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
        }
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
