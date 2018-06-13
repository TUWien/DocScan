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
import at.ac.tuwien.caa.docscan.camera.threads.crop.PageDetector;

public class CropRectTransform extends BitmapTransformation {

    private static final String ID = "at.ac.tuwien.caa.docscan.gallery.CropRectTransform";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private String mFileName;
    private Paint mQuadPaint;
    private Path mQuadPath;

    public CropRectTransform(String fileName, int strokeColor, float strokeWidth) {

        super();

        mFileName = fileName;
        initPaint(strokeColor, strokeWidth);

    }


    @Override
    protected Bitmap transform(BitmapPool bitmapPool, Bitmap original, int width, int height) {

        Bitmap result = bitmapPool.get(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        // If no matching Bitmap is in the pool, get will return null, so we should allocate.
        if (result == null) {
            // Use ARGB_8888 since we're going to add alpha to the image.
            result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        }

        // Create a Canvas backed by the result Bitmap.
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        Bitmap resized = Bitmap.createScaledBitmap(original, original.getWidth(), original.getHeight(), true);
        // Draw the original Bitmap onto the result Bitmap with a transformation:
        canvas.drawBitmap(resized, 0, 0, paint);

        ArrayList<PointF> points = PageDetector.getScaledCropPoints(mFileName,
                original.getWidth(), original.getHeight());
        drawQuad(canvas, points);

        return result;
    }

    private void initPaint(int strokeColor, float strokeWidth) {
        mQuadPaint = new Paint();
        mQuadPath = new Path();

        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(strokeWidth);
        mQuadPaint.setColor(strokeColor);
//        mQuadPaint.setStrokeWidth(4);
        mQuadPaint.setAntiAlias(true);
    }



    private void drawQuad(Canvas canvas, ArrayList<PointF> points) {

        mQuadPath.reset();
        boolean isStartSet = false;

        for (PointF point : points) {

            if (!isStartSet) {
                mQuadPath.moveTo(point.x, point.y);
                isStartSet = true;
            } else
                mQuadPath.lineTo(point.x, point.y);

        }

        mQuadPath.close();
        canvas.drawPath(mQuadPath, mQuadPaint);

    }


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
