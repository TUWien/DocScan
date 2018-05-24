package at.ac.tuwien.caa.docscan.gallery;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.threads.Cropper;

public class CropRectTransform extends BitmapTransformation {

    private static final String ID = "at.ac.tuwien.caa.docscan.gallery.CropRectTransform";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));
    private final int PAGE_RECT_COLOR = R.color.hud_page_rect_color;

    private String mFileName;

    public CropRectTransform(String fileName) {

        super();
        mFileName = fileName;

    }

//    @Override
//    public Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
////        if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight) {
////            return toTransform;
////        }
//
//        return Bitmap.createScaledBitmap(toTransform, toTransform.getWidth(), toTransform.getHeight(), /*filter=*/ true);
//
////        return Bitmap.createScaledBitmap(toTransform, outWidth, outHeight, /*filter=*/ true);
//    }

    @Override
    protected Bitmap transform(BitmapPool bitmapPool, Bitmap original, int width, int height) {
//        Bitmap result = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888);
        Bitmap result = bitmapPool.get(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        // If no matching Bitmap is in the pool, get will return null, so we should allocate.
        if (result == null) {
            // Use ARGB_8888 since we're going to add alpha to the image.
            result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
//            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        // Create a Canvas backed by the result Bitmap.
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

//        Bitmap resized = Bitmap.createScaledBitmap(original, width, height, true);
        Bitmap resized = Bitmap.createScaledBitmap(original, original.getWidth(), original.getHeight(), true);
        // Draw the original Bitmap onto the result Bitmap with a transformation:
        canvas.drawBitmap(resized, 0, 0, paint);

//        ArrayList<PointF> points = Cropper.getNormedCropPoints(mFileName, original.getWidth(), original.getHeight());
        ArrayList<PointF> points = Cropper.getScaledCropPoints(mFileName,
                original.getWidth(), original.getHeight());

//        ArrayList<PointF> points = getNormedCropPoints(width, height);
        drawQuad(canvas, points);
        // Since we've replaced our original Bitmap, we return our new Bitmap here. Glide will
        // will take care of returning our original Bitmap to the BitmapPool for us.
        return result;
    }



    private void drawQuad(Canvas canvas, ArrayList<PointF> points) {

//        canvas.drawCircle(marker1.x, marker1.y, 50, mQuadPaint);

        Path mQuadPath = new Path();
        Paint mQuadPaint;
        mQuadPaint = new Paint();
//        mQuadPaint.setColor(PAGE_RECT_COLOR);
        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(4);
        mQuadPaint.setAntiAlias(true);

        mQuadPath.reset();
        boolean isStartSet = false;

        for (PointF point : points) {

            if (!isStartSet) {
                mQuadPath.moveTo(point.x, point.y);
                isStartSet = true;
            } else
                mQuadPath.lineTo(point.x, point.y);

            // draw the circle around the corner:
//            canvas.drawCircle(point.x, point.y, CORNER_CIRCLE_RADIUS, mCirclePaint);

        }

        mQuadPath.close();
        canvas.drawPath(mQuadPath, mQuadPaint);

    }

//    @Override
//    public Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
////        if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight) {
////            return toTransform;
////        }
//
////        TODO: disable this later!
////        Bitmap result = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888);
////        // If no matching Bitmap is in the pool, get will return null, so we should allocate.
////        if (result == null) {
////            // Use ARGB_8888 since we're going to add alpha to the image.
////            result = Bitmap.createScaledBitmap(toTransform, outWidth, outHeight, /*filter=*/ true);
//////            result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
////        }
//
//        Bitmap ground = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
//        Bitmap result = Bitmap.createScaledBitmap(toTransform, outWidth, outHeight, true);
//
//        Canvas canvas = new Canvas(ground);
//        Paint paint = new Paint();
//        paint.setAlpha(20);
////        canvas.drawBitmap(result, 0, 0, paint);
//        canvas.drawBitmap(toTransform, 0, 0, paint);
////        return Bitmap.createScaledBitmap(result, outWidth, outHeight, /*filter=*/ true);
//        return result;
//    }


    @Override
    public boolean equals(Object o) {
        return o instanceof CropRectTransform;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}
