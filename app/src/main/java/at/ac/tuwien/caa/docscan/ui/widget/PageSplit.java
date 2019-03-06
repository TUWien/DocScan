package at.ac.tuwien.caa.docscan.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.tensorflow.lite.Interpreter;

import at.ac.tuwien.caa.docscan.logic.Helper;

/**
 *   Created by Matthias Wödlinger on 14.02.2019
 */

public class PageSplit {


    /** Name of the model file stored in Assets. */
    private static final String MODEL_NAME = "ps_1_from_resized_images_output";
    private static final String MODEL_PATH = MODEL_NAME + ".tflite";

    /** Folder where for output */
    private static final String FOLDER_NAME = "PageSplit";

    /** Log TAG */
    public static final String TAG = "PageSplit";

    /** TfLite model only works for fixed input dimension. Input will be resized. */
    static final int DIM_IMG_SIZE_X = 300;
    static final int DIM_IMG_SIZE_Y = 300;

    /** An instance of the TfLite Interpreter */
    private Interpreter mTflite;

    /** Initializes the PageSplit model */
    public PageSplit(Context mContext) throws IOException {

        long startTime = SystemClock.uptimeMillis();
        mTflite = new Interpreter(loadModelFile(mContext));
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Created a Tensorflow Lite model in " + Long.toString((endTime - startTime)) + "ms");
    }

    public int applyPageSplit(Uri uri, Context mContext) throws IOException {

        if (mTflite == null) {
            Log.e(TAG, "PageSplit has not been initialized; Skipped.");
            return 1;
        }

        // Load input bitmap
        Bitmap inBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
        inBitmap = Bitmap.createScaledBitmap(inBitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);

        // get bitmap metadata and rotate accordingly. PictureOut will be used for the overlay.
        //The PageSplit model will be applied to inBitmap.
        final ExifInterface exif = new ExifInterface(uri.getPath());
        final int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        final int rotationInDegrees = exifToDegrees(rotation);
        Matrix PictureOutMatrix = new Matrix();
        PictureOutMatrix.postRotate(rotationInDegrees);
        Log.d(TAG, "rotated by " + rotationInDegrees + " degrees");
        final Bitmap PictureOut = Bitmap.createBitmap(inBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, PictureOutMatrix, true);

        // The model needs the book rotated by 90°
        Matrix inBitmapMatrix = new Matrix();
        inBitmapMatrix.postRotate(90);
        inBitmap = Bitmap.createBitmap(inBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmapMatrix, true);


        long startTime = SystemClock.uptimeMillis();

        // The TfLite model needs a float array as input.
        // For alternatives see: https://www.tensorflow.org/lite/apis#running_a_model_2
        float[][][][] inFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];
        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                int color = inBitmap.getPixel(x,y);
                inFloat[0][x][y][0] = Color.red(color);
                inFloat[0][x][y][1] = Color.green(color);
                inFloat[0][x][y][2] = Color.blue(color);
            }
        }

        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "inBitmap -> inFloat took " + Long.toString((endTime - startTime)) + "ms");

        // The output of the model will be stored in outFloat
        float[][][][] outFloat = new float[1][DIM_IMG_SIZE_X][DIM_IMG_SIZE_Y][3];

        // Running the model
        startTime = SystemClock.uptimeMillis();
        mTflite.run(inFloat, outFloat);
        endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "tflite.run took " + Long.toString((endTime - startTime)) + "ms");

        startTime = SystemClock.uptimeMillis();

        // Create outBitmap and feed it with the pixels
        Bitmap outBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, inBitmap.getConfig());
        for (int x = 0; x < DIM_IMG_SIZE_X; ++x) {
            for (int y = 0; y < DIM_IMG_SIZE_Y; ++y) {
                int c0 = (int) (255.0 * outFloat[0][x][y][0]);
                int c1 = (int) (255.0 * outFloat[0][x][y][1]);
                int c2 = (int) (255.0 * outFloat[0][x][y][2]);
                outBitmap.setPixel(x, y, Color.argb(255, c0, c1, c2));
            }
        }

        // Rotate the output such that it fits the PictureOut Bitmap for the overlay
        Matrix afterTfLiteMatrix = new Matrix();
        afterTfLiteMatrix.postRotate(rotationInDegrees-90);
        inBitmap = Bitmap.createBitmap(inBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, afterTfLiteMatrix, true);
        outBitmap = Bitmap.createBitmap(outBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, afterTfLiteMatrix, true);

        // Create the overlay
        Bitmap overlayBitmap = Bitmap.createBitmap(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, PictureOut.getConfig());
        Canvas inOutOverlay = new Canvas(overlayBitmap);
        Paint paint = new Paint();
        inOutOverlay.drawBitmap(PictureOut, 0, 0, paint);
        paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN));
        inOutOverlay.drawBitmap(outBitmap, 0, 0, paint);



        final String filename = MODEL_NAME + ".jpg";

        saveBitmap(outBitmap, filename, mContext);
        saveBitmap(overlayBitmap, "overlay.jpg", mContext);
        saveBitmap(inBitmap, "inBitmap.jpg", mContext);

        endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Bitmap creation and save in gallery took " + Long.toString((endTime - startTime)) + "ms");

        return 0;
    }

    /** Save Bitmap in seperate folder named FOLDER_NAME shown in gallery */
    private void saveBitmap(final Bitmap bitmap, final String filename, Context mContext) {
        File mediaDir = Helper.getMediaStorageDir(FOLDER_NAME);//mContext.getString(R.string.app_name));

        final File file = new File(mediaDir, filename);
        Log.d(TAG, "created directory for PageSplit");
        if (file.exists()) {
            file.delete();
            Log.d(TAG, "Existing File overwritten");
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            Log.e(TAG, "Failed to save the file");
        }
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        Log.d(TAG, "PageSplit outPut image saved under " + filename);
    }

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Context mContext) throws IOException {
        AssetFileDescriptor fileDescriptor = mContext.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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

    /** Closes tflite to release resources. */
    public void close() {
        mTflite.close();
        mTflite = null;
    }
}

