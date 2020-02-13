package at.ac.tuwien.caa.docscan.camera.cv.thread.crop;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionText.TextBlock;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.itextpdf.text.Document;
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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity;


import static at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor.MESSAGE_CREATED_DOCUMENT;

public class PdfCreator {

    private static final String CLASS_NAME = "PdfCreator";
    private static FirebaseVisionTextRecognizer sTextRecognizer;
    public static final String PDF_INTENT = "PDF_INTENT";
    public static final String PDF_FILE_NAME = "PDF_FILE_NAME";
    public static final String PDF_CHANNEL_ID = "PDF_CHANNEL_ID";
    public static final CharSequence PDF_CHANNEL_NAME = "DocScan Pdf";// The user-visible name of the channel.

    public static void createPdfWithoutOCR(String documentName, final ArrayList<File> files,
                                           CropRunnable cropRunnable, WeakReference<Context> context) {

        if (context == null || context.get() == null)
            return;

        final Context contextF = context.get();
        final NotificationManager notificationManager = (NotificationManager)
                contextF.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = getNotificationBuilder(documentName, notificationManager,
                contextF);

        progressNotification(documentName, -1, notificationManager, contextF,
                builder);

        boolean saved = savePdf(documentName, files, null, cropRunnable, contextF);
        if (saved)
            successNotification(documentName, notificationManager, contextF, builder);
        else
            errorNotification(documentName, notificationManager, contextF, builder);


    }

    public static void createPdfWithOCR(final String documentName, final ArrayList<File> files,
                                        final CropRunnable cropRunnable, WeakReference<Context> context) {

        final FirebaseVisionText[] ocrResults = new FirebaseVisionText[files.size()];

        for (final File file : files) {
            boolean success =
                    processFile(documentName, files, cropRunnable, context, ocrResults, file);

//            No need to show an error message here, because it is shown by processFile
            if (!success)
                return;

        }


    }

    private static boolean processFile(final String documentName, final ArrayList<File> files,
                                    final CropRunnable cropRunnable, WeakReference<Context> context,
                                    final FirebaseVisionText[] ocrResults, final File file) {

        try {


            FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context.get(),
                    Uri.fromFile(file));

            final Context contextF = context.get();
            final NotificationManager notificationManager = (NotificationManager)
                    contextF.getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder builder = getNotificationBuilder(documentName,
                    notificationManager, contextF);

            Task<FirebaseVisionText> task = getTextRecognizer().processImage(image);

            try {

                Tasks.await(task);

                task.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {

                        ocrResults[files.indexOf(file)] = result;

                        int finishCnt = getFinishCnt(ocrResults);

                        int progress = (int) Math.floor(finishCnt / (double) files.size() * 100);
                        progressNotification(documentName, progress, notificationManager, contextF,
                                builder);


                        if (finishCnt == ocrResults.length) {
                            boolean success =
                                    savePdf(documentName, files, ocrResults, cropRunnable, contextF);
                            if (success)
                                successNotification(documentName, notificationManager, contextF, builder);
                            else
                                errorNotification(documentName, notificationManager, contextF, builder);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.e(CLASS_NAME, "Failed to recognize text\n" + e.getMessage());
                    }
                });
            } catch (ExecutionException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            } catch (InterruptedException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

        } catch (OutOfMemoryError e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            return false;
        }

        return true;

    }

    private static void notifyPdfChanged(File file, Context context) {

        if (file == null || context == null)
            return;

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);

//                Send the broadcast:
        context.sendBroadcast(mediaScanIntent);

    }

    private static int getFinishCnt(FirebaseVisionText[] ocrResults) {

        int finishCnt = 0;

        for (FirebaseVisionText r : ocrResults) {
            if (r != null)
                finishCnt++;
        }

        return finishCnt;
    }

    private static NotificationCompat.Builder getNotificationBuilder(String documentName,
            NotificationManager notificationManager, Context context) {

        String title = context.getString(R.string.sync_notification_title);

        //        Create an intent that is started, if the user clicks on the notification:
        Intent intent = new Intent(context, DocumentViewerActivity.class);
        intent.putExtra(PDF_INTENT, true);

        intent.putExtra(PDF_FILE_NAME, getPdfFile(documentName).getAbsolutePath());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                PDF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_docscan_notification)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setChannelId(PDF_CHANNEL_ID);

        // On Android O we need a NotificationChannel, otherwise the notification is not shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_LOW disables the notification sound:
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(
                    PDF_CHANNEL_ID, PDF_CHANNEL_NAME, importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        return builder;
    }

    private static void progressNotification(String documentName, int progress,
                                                  NotificationManager notificationManager,
                                                  Context context, NotificationCompat.Builder builder) {

        if (builder == null)
            return;

        if (progress != -1)
            builder.setContentTitle(context.getString(R.string.pdf_notification_exporting))
                    .setContentText(documentName)
                    .setProgress(100, progress, false);
        else
            builder.setContentTitle(context.getString(R.string.pdf_notification_exporting))
                    .setContentText(documentName)
                    // Removes the progress bar
                    .setProgress(0, 0, false);

        // show the new notification:
        notificationManager.notify(25, builder.build());

    }

    private static void successNotification(String documentName,
                                            NotificationManager notificationManager,
                                            Context context, NotificationCompat.Builder builder) {

        if (builder == null)
            return;

        builder.setContentTitle(context.getString(R.string.pdf_notification_done))
                .setContentText(documentName)
                // Removes the progress bar
                .setProgress(0, 0, false);

        // show the new notification:
        notificationManager.notify(25, builder.build());

    }

    private static void errorNotification(String documentName,
                                            NotificationManager notificationManager,
                                            Context context, NotificationCompat.Builder builder) {

        if (builder == null)
            return;

        builder.setContentTitle(context.getString(R.string.pdf_notification_error))
                .setContentText(documentName)
                // Removes the progress bar
                .setProgress(0, 0, false);

        // show the new notification:
        notificationManager.notify(25, builder.build());

    }



    private static boolean savePdf(String documentName, ArrayList<File> files,
                                FirebaseVisionText[] ocrResults, CropRunnable cropRunnable, Context context) {

        BitmapSize size = new BitmapSize(files.get(0));
        boolean landscapeFirst = isLandscape(size);

        Log.d(CLASS_NAME, "is landscape: " + landscapeFirst);
        Log.d(CLASS_NAME, "first bm size: " + size.mWidth + "x" + size.mHeight);
        Rectangle firstPageSize = getPageSize(size, landscapeFirst);

        Log.d(CLASS_NAME, "page size: " + firstPageSize.getWidth() + " " + firstPageSize.getHeight());

        File outputFile = getPdfFile(documentName);
        Document document = new Document(firstPageSize, 0, 0, 0, 0);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                int rotationInDegrees = Helper.getAngleFromExif(Helper.getExifOrientation(file));

                //add the original image to the pdf and set the DPI of it to 600
                Image image = Image.getInstance(file.getAbsolutePath());
                image.setRotationDegrees(-rotationInDegrees);
                if (rotationInDegrees == 0 || rotationInDegrees == 180)
                    image.scaleAbsolute(document.getPageSize().getWidth(),
                            document.getPageSize().getHeight());
                else
                    image.scaleAbsolute(document.getPageSize().getHeight(),
                            document.getPageSize().getWidth());

                image.setDpi(600, 600);
                document.add(image);


                if (ocrResults != null && ocrResults[i] != null) {
                    // the direct content where we write on
                    // directContentUnder instead of directContent, because then the text is in the background)
                    //PdfContentByte cb = writer.getDirectContentUnder();
                    PdfContentByte cb = writer.getDirectContentUnder();
                    BaseFont bf = BaseFont.createFont();

                    //sort the result based on the y-Axis so that the markup order is correct
                    List<List<FirebaseVisionText.TextBlock>> sortedBlocks = sortBlocks(ocrResults[i]);
                    size = new BitmapSize(file);

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

                            if (line.getBoundingBox() == null)
                                continue;

                            float left = ((float) line.getBoundingBox().left / (float) size.mWidth) * document.getPageSize().getWidth();
                            float right = ((float) line.getBoundingBox().right / (float) size.mWidth) * document.getPageSize().getWidth();
                            float top = ((float) line.getBoundingBox().top / (float) size.mHeight) * document.getPageSize().getHeight();
                            float bottom = ((float) line.getBoundingBox().bottom / (float) size.mHeight) * document.getPageSize().getHeight();
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
                    BitmapSize aSize = new BitmapSize(file);
                    Rectangle pageSize = getPageSize(aSize, landscapeFirst);
                    document.setPageSize(pageSize);
                    document.newPage();
                }
            }

            document.close();

            notifyPdfChanged(outputFile, context);
            Log.d(CLASS_NAME, "Document created at " + outputFile.getAbsolutePath());
            cropRunnable.mCropTask.handleState(MESSAGE_CREATED_DOCUMENT);

        } catch (Exception e) {

            Crashlytics.logException(e);
            Log.e(CLASS_NAME, "Failed to create document: " + e.getMessage());
            return false;
        }


        return true;

    }

    @NotNull
    private static File getPdfFile(String documentName) {
        String pdfName = documentName + ".pdf";
        return new File(Helper.getPDFStorageDir("DocScan"), pdfName);
    }

    private static boolean isLandscape(BitmapSize size) {

        return size.mWidth > size.mHeight;

    }

    private static Rectangle getPageSize(BitmapSize size, boolean landscape) {

        Rectangle pageSize;
        if (landscape) {
            float height = (PageSize.A4.getHeight() / size.mWidth) * size.mHeight;
            pageSize = new Rectangle(PageSize.A4.getHeight(), height);
        } else {
            float height = (PageSize.A4.getWidth() / size.mWidth) * size.mHeight;
            pageSize = new Rectangle(PageSize.A4.getWidth(), height);
        }
        return pageSize;

    }


    @NonNull
    private static List<List<FirebaseVisionText.TextBlock>> sortBlocks(FirebaseVisionText ocrResult) {
        List<List<FirebaseVisionText.TextBlock>> sortedBlocks = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> biggestBlocks = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> blocksSortedByWidth = sortByWidth(ocrResult.getTextBlocks());
        for (FirebaseVisionText.TextBlock block : blocksSortedByWidth) {

            if (block.getBoundingBox() == null)
                continue;

            if (sortedBlocks.isEmpty()) {
                List<FirebaseVisionText.TextBlock> blocks = new ArrayList<>();
                blocks.add(block);
                biggestBlocks.add(block);
                sortedBlocks.add(blocks);
            } else {
                boolean added = false;
                for (TextBlock checkBlock : biggestBlocks) {

                    if (checkBlock.getBoundingBox() == null)
                        continue;

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
                        if (biggestBlocks.get(i).getBoundingBox() == null ||
                                block.getBoundingBox().centerX() > biggestBlocks.get(i).getBoundingBox().centerX()) {
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

            if (textBlock.getBoundingBox() == null)
                continue;

            if (sortedBlocks.isEmpty()) {
                sortedBlocks.add(textBlock);
            } else {
                int i = 0;
                while (i < sortedBlocks.size()) {
                    if (sortedBlocks.get(i).getBoundingBox() == null ||
                            textBlock.getBoundingBox().width() < sortedBlocks.get(i).getBoundingBox().width()) {
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

//                if (line.getCornerPoints() == null || line.getCornerPoints().length == 0)
//                    continue;

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

//    public static File getDocumentsDir() {
//        File docsFolder;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//            docsFolder = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS);
////            docsFolder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOCUMENTS);
//        } else {
//            docsFolder = new File(Environment.getExternalStorageDirectory(), "Documents");
////            docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
//        }
//        if (!docsFolder.exists()) {
//            docsFolder.mkdir();
//        }
//        return docsFolder;
//    }

    private static FirebaseVisionTextRecognizer getTextRecognizer() {
        if (sTextRecognizer == null) {
            sTextRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        return sTextRecognizer;
    }


    private static class BitmapSize {

        private int mWidth, mHeight;

        BitmapSize(File file) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            //Returns null, sizes are in the options variable
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            mWidth = options.outWidth;
            mHeight = options.outHeight;

            //        Is the image rotated in the metadata?
            int exifOrientation = -1;
            try {
                exifOrientation = Helper.getExifOrientation(file);
            } catch (IOException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
            }

            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                int tmp = mHeight;
                mHeight = mWidth;
                mWidth = tmp;
            }


        }

    }

}
