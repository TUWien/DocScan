package at.ac.tuwien.caa.docscan.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 21.11.2017.
 * Partially copied from: SimpleCropView
 * https://github.com/IsseiAoki/SimpleCropView/tree/master/simplecropview-sample/src/main/java/com/example/simplecropviewsample
 * Licence by SimpleCropView:
 * The MIT License (MIT)

 Copyright (c) 2015 Issei Aoki

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.

 */

public class CropView extends android.support.v7.widget.AppCompatImageView {

    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;
    private static final int WHITE = 0x00FFFFFF;
    private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);

    private Matrix mMatrix = null;
    private int mBackgroundColor;
    private Paint mPaintBitmap;
    private Paint mPaintTranslucent;
    private int mOverlayColor;
    private RectF mImageRect;
    private Paint mPaintFrame;
    private int mFrameColor;
    // TODO: i do not believe that this is device independent!
    private float mFrameStrokeWeight = 2.0f;
    private RectF mFrameRect;
    private Path mQuadPath;
    private Paint mQuadPaint;

    // used for layout size calculation:
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private float mScale = 1.0f;
    private float mImgWidth = 0.0f;
    private float mImgHeight = 0.0f;
    private PointF mCenter = new PointF();
    private PointF marker1 = new PointF();

    private ArrayList<PointF> mNormedPoints;



    private CropQuad mCropQuad;

    public CropView(Context context) {
        this(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setScaleType(ScaleType.FIT_CENTER);

        mBackgroundColor = TRANSPARENT;
        mMatrix = new Matrix();
        mPaintBitmap = new Paint();
        mPaintBitmap.setFilterBitmap(true);
        mPaintTranslucent = new Paint();
        mOverlayColor = TRANSLUCENT_BLACK;
        mPaintFrame = new Paint();
        mFrameColor = getResources().getColor(R.color.hud_page_rect_color);

        mFrameRect = new RectF(0,0, 200, 200);

//        ArrayList<PointF> points = new ArrayList<>();
//        points.add(new PointF(100,100));
//        points.add(new PointF(300,100));
//        points.add(new PointF(400,400));
//        points.add(new PointF(10, 100));
//
//        mCropQuad = new CropQuad(points);

        initQuadPaint();

    }

    public void setBitmapAndPoints(Bitmap bitmap, ArrayList<PointF> normedPoints) {

        setImageBitmap(bitmap);
        mCropQuad = new CropQuad(normedPoints, bitmap.getWidth(), bitmap.getHeight());

    }

//    public void setNormedPoints(ArrayList<PointF> normedPoints) {
//
//        mNormedPoints = normedPoints;
//        // Check if the bitmap is already set, if so create the CropQuad:
//        Bitmap bm = getBitmap();
//
//        // TODO: check what we can do against first setting points and then setting the bitmap:
//        if (bm != null)
//            mCropQuad = new CropQuad(normedPoints, bm.getWidth(), bm.getHeight());
//    }


//    public void setPoints()

    private void initQuadPaint() {

        mQuadPaint = new Paint();
        mQuadPaint.setColor(PAGE_RECT_COLOR);
        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_stroke_width));
        mQuadPaint.setAntiAlias(true);
        mQuadPath = new Path();

    }

    @Override public void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);

        setMatrix();
        Bitmap bm = getBitmap();
        if (bm != null) {
            canvas.drawBitmap(bm, mMatrix, mPaintBitmap);
            // draw edit frame
            drawCropFrame(canvas);
        }

    }

    private void drawCropFrame(Canvas canvas) {

        drawQuad(canvas);

//        if (!mIsCropEnabled) return;
//        if (mIsRotating) return;
//        drawOverlay(canvas);
//        drawFrame(canvas);
//        if (mShowGuide) drawGuidelines(canvas);
//        if (mShowHandle) drawHandles(canvas);
    }

//    private void drawOverlay(Canvas canvas) {
//        mPaintTranslucent.setAntiAlias(true);
//        mPaintTranslucent.setFilterBitmap(true);
//        mPaintTranslucent.setColor(mOverlayColor);
//        mPaintTranslucent.setStyle(Paint.Style.FILL);
//        Path path = new Path();
//        RectF overlayRect =
//                new RectF((float) Math.floor(mImageRect.left), (float) Math.floor(mImageRect.top),
//                        (float) Math.ceil(mImageRect.right), (float) Math.ceil(mImageRect.bottom));
//
//        path.addRect(overlayRect, Path.Direction.CW);
//        path.addRect(mFrameRect, Path.Direction.CCW);
//        canvas.drawPath(path, mPaintTranslucent);
//    }

    private void drawFrame(Canvas canvas) {
        mPaintFrame.setAntiAlias(true);
        mPaintFrame.setFilterBitmap(true);
        mPaintFrame.setStyle(Paint.Style.FILL);
//        mPaintFrame.setStyle(Paint.Style.STROKE);
        mPaintFrame.setColor(mFrameColor);
        mPaintFrame.setStrokeWidth(mFrameStrokeWeight);
        canvas.drawRect(mFrameRect, mPaintFrame);
    }

    private void drawQuad(Canvas canvas) {

//        canvas.drawCircle(marker1.x, marker1.y, 50, mQuadPaint);

        mQuadPath.reset();
        boolean isStartSet = false;

        for (PointF point : mCropQuad.getPoints()) {

            if (!isStartSet) {
                mQuadPath.moveTo(point.x, point.y);
                isStartSet = true;
            } else
                mQuadPath.lineTo(point.x, point.y);

        }

        mQuadPath.close();
        canvas.drawPath(mQuadPath, mQuadPaint);

    }


    @Override public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                return true;
//            case MotionEvent.ACTION_CANCEL:
//                getParent().requestDisallowInterceptTouchEvent(false);
//                onCancel();
//                return true;
//            case MotionEvent.ACTION_UP:
//                getParent().requestDisallowInterceptTouchEvent(false);
//                onUp(event);
//                return true;
        }
        return false;

    }

    private void onDown(MotionEvent e) {

//        Check if we need this right here:
        invalidate();

        PointF point = getPoint(e);
        if (mCropQuad.isPointClose(point)) {
            Log.d(this.getClass().getName(), "point is touched");
        }

//        mLastX = e.getX();
//        mLastY = e.getY();
//        checkTouchArea(e.getX(), e.getY());
    }

    // Calculations for layout size:

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);

        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getDrawable() != null) setupLayout(mViewWidth, mViewHeight);
    }

    private void setupLayout(int viewW, int viewH) {
        if (viewW == 0 || viewH == 0) return;
        mCenter = new PointF(getPaddingLeft() + viewW * 0.5f, getPaddingTop() + viewH * 0.5f);
        mScale = calcScale(viewW, viewH);
        setMatrix();

        // Tell the CropQuad the transformation:
        Bitmap bm = getBitmap();
        if (bm != null && mCropQuad != null) {

            int numPts = 8;
            float[] points = new float[numPts];
            float[] dstPoints = new float[numPts];

            points[0] = mCropQuad.getNormedPoints().get(0).x;
            points[1] = mCropQuad.getNormedPoints().get(0).y;

            points[2] = mCropQuad.getNormedPoints().get(1).x;
            points[3] = mCropQuad.getNormedPoints().get(1).y;

            points[4] = mCropQuad.getNormedPoints().get(2).x;
            points[5] = mCropQuad.getNormedPoints().get(2).y;

            points[6] = mCropQuad.getNormedPoints().get(3).x;
            points[7] = mCropQuad.getNormedPoints().get(3).y;

            mMatrix.mapPoints(points);

            ArrayList<PointF> mappedPoints = new ArrayList<>();
            mappedPoints.add(new PointF(points[0], points[1]));
            mappedPoints.add(new PointF(points[2], points[3]));
            mappedPoints.add(new PointF(points[4], points[5]));
            mappedPoints.add(new PointF(points[6], points[7]));

            mCropQuad.setPoints(mappedPoints);

        }

//        mImageRect = calcImageRect(new RectF(0f, 0f, mImgWidth, mImgHeight), mMatrix);

//        if (mInitialFrameRect != null) {
//            mFrameRect = applyInitialFrameRect(mInitialFrameRect);
//        } else {
//            mFrameRect = calcFrameRect(mImageRect);
//        }
//
//        mIsInitialized = true;
        invalidate();
    }

    private float calcScale(int viewW, int viewH) {
        mImgWidth = getDrawable().getIntrinsicWidth();
        mImgHeight = getDrawable().getIntrinsicHeight();
        if (mImgWidth <= 0) mImgWidth = viewW;
        if (mImgHeight <= 0) mImgHeight = viewH;
        float viewRatio = (float) viewW / (float) viewH;
        float imgRatio = mImgWidth / mImgHeight;
        float scale = 1.0f;
        if (imgRatio >= viewRatio) {
            scale = viewW / mImgWidth;
        } else if (imgRatio < viewRatio) {
            scale = viewH / mImgHeight;
        }
        return scale;
    }

    private void setMatrix() {

        mMatrix.reset();
        mMatrix.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mScale, mScale, mCenter.x, mCenter.y);
//        mMatrix.postRotate(mAngle, mCenter.x, mCenter.y);
    }

    private void onMove(MotionEvent e) {

        PointF point = getPoint(e);

        // Just draw updates if a point is really moved:
        if (mCropQuad.moveActivePoint(point))
            invalidate();

    }

    private PointF getPoint(MotionEvent e) {

        return new PointF(e.getX(), e.getY());

    }

    private Bitmap getBitmap() {
        Bitmap bm = null;
        Drawable d = getDrawable();
        if (d != null && d instanceof BitmapDrawable) bm = ((BitmapDrawable) d).getBitmap();
        return bm;
    }


}
