package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;

public class CropRectTransform extends BitmapTransformation {

    private static final String ID = "at.ac.tuwien.caa.docscan.gallery.CropRectTransform";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private String mFileName;
    private Paint mQuadPaint;
    private Path mQuadPath;

//    private Paint mQuadPaint, mOuterQuadPaint;
//    private Path mQuadPath, mOuterQuadPath;

    public CropRectTransform(String fileName, Context context) {

        super();

        mFileName = fileName;
        initPaint(context);

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

        PageDetector.PageFocusResult pfResult = PageDetector.getScaledCropPoints(mFileName,
                original.getWidth(), original.getHeight());

        if (!pfResult.isFocused())
            mQuadPaint.setColor(Color.parseColor("#FF5722"));
        drawQuad(canvas, pfResult.getPoints(), mQuadPath, mQuadPaint);

//        ArrayList<PointF> points = PageDetector.getScaledCropPoints(mFileName,
//                original.getWidth(), original.getHeight());
//        drawQuad(canvas, points, mQuadPath, mQuadPaint);


        return result;
    }

    private void initPaint(Context context) {

        float strokeWidth = context.getResources().getDimension(R.dimen.page_gallery_stroke_width);
        int strokeColor = context.getResources().getColor(R.color.hud_page_rect_color);

        mQuadPaint = new Paint();
        mQuadPath = new Path();

        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(strokeWidth);
        mQuadPaint.setColor(strokeColor);
        mQuadPaint.setAntiAlias(true);

//        float outerStrokeWidth = context.getResources().getDimension(R.dimen.page_gallery_stroke_width);
//        int outerStrokeColor = context.getResources().getColor(R.color.page_outer_color);
//
//        mOuterQuadPaint = new Paint();
//        mOuterQuadPath = new Path();
//
//        mOuterQuadPaint.setStyle(Paint.Style.STROKE);
//        mOuterQuadPaint.setStrokeWidth(outerStrokeWidth);
//        mOuterQuadPaint.setColor(outerStrokeColor);
//        mOuterQuadPaint.setAntiAlias(true);

    }



    private void drawQuad(Canvas canvas, ArrayList<PointF> points, Path path, Paint paint) {

//        initOuterValues(canvas);

        path.reset();
        boolean isStartSet = false;

        for (PointF point : points) {

            if (!isStartSet) {
                path.moveTo(point.x, point.y);
                isStartSet = true;
            } else {
                path.lineTo(point.x, point.y);
            }

        }

        path.close();
        canvas.drawPath(path, paint);

    }

//    private void initOuterValues(Canvas canvas) {
//
//        centerPoint = new PointF((float) canvas.getWidth() / 2, (float) canvas.getHeight() / 2);
//        outerOffset = 5;
//
//    }
//
//    private PointF getOuterPoint(PointF point) {
//
//        PointF result = new PointF(point.x, point.y);
//
//        if (isLeftOfCenter(point))
//            result.x -= outerOffset;
//        else
//            result.x += outerOffset;
//
//        if (isAboveCenter(point))
//            result.y -= outerOffset;
//        else
//            result.y += outerOffset;
//
//
//        return result;
//
//    }
//
//    private boolean isLeftOfCenter(PointF point) {
//
//        return point.x < centerPoint.x;
//
//    }
//
//    private boolean isAboveCenter(PointF point) {
//
//        return point.y < centerPoint.y;
//
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
