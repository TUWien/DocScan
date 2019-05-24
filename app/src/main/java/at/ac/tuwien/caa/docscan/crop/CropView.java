package at.ac.tuwien.caa.docscan.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
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

public class CropView extends androidx.appcompat.widget.AppCompatImageView {

    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;
    private static final int WHITE = 0x00FFFFFF;
    private static final String CLASS_NAME = "CropView";

    private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);
    private final int PAGE_OUTER_RECT_COLOR = getResources().getColor(R.color.page_outer_color);
    private final int DETAIL_OUTLINE_COLOR = getResources().getColor(R.color.detail_crop_outline);
    private final int CROSS_COLOR = getResources().getColor(R.color.cross_color);
    private final float CORNER_CIRCLE_RADIUS =
            getResources().getDimension(R.dimen.crop_circle_radius);
    private final float DETAIL_WIDTH =
            getResources().getDimension(R.dimen.crop_detail_width);
    private final float DETAIL_OUTER_WIDTH =
            getResources().getDimension(R.dimen.crop_detail_outer_width);
    private final float DETAIL_OFFSET =
            getResources().getDimension(R.dimen.crop_detail_offset);
    private final float CROSS_WIDTH =
            getResources().getDimension(R.dimen.crop_detail_width);
    private final int DETAIL_IMG_WIDTH = 50;

    private Paint mDetailOutlinePaint;
    private Matrix mMatrix = null;
    private int mBackgroundColor;
    private Paint mPaintBitmap;
    private Paint mDetailPaint;
    private Paint mPaintTranslucent;
    private int mOverlayColor;
    private RectF mImageRect;
    private Paint mPaintFrame;
    private int mFrameColor;
    private RectF mFrameRect;
    private Path mQuadPath, mOuterQuadPath;
    private Paint mQuadPaint, mOuterQuadPaint, mCrossPaint, mCirclePaint;

    // used for layout size calculation:
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private float mScale = 1.0f;
    private float mImgWidth = 0.0f;
    private float mImgHeight = 0.0f;
    private PointF mCenter = new PointF();
    private boolean mIsPointDragged = false;



    private CropQuad mCropQuad;
    private ArrayList<PointF> mNormedPoints;
//    private int mAngle;

    public CropView(Context context) {
        this(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);

        setScaleType(ScaleType.FIT_CENTER);

//        mAngle = 0;

        mBackgroundColor = TRANSPARENT;
        mMatrix = new Matrix();
        mPaintBitmap = new Paint();
        mPaintBitmap.setFilterBitmap(true);

        mPaintTranslucent = new Paint();
        mOverlayColor = TRANSLUCENT_BLACK;
        mPaintFrame = new Paint();
        mFrameColor = getResources().getColor(R.color.hud_page_rect_color);

        mFrameRect = new RectF(0,0, 200, 200);

        initQuadPaint();
        initOuterQuadPaint();
        initCrossPaint();
        initCirclePaint();
        initDetailOutlinePaint();

    }

    private void initDetailOutlinePaint() {

        mDetailOutlinePaint = new Paint();
        mDetailOutlinePaint.setStyle(Paint.Style.FILL);
        mDetailOutlinePaint.setColor(DETAIL_OUTLINE_COLOR);

    }


    public void setPoints(ArrayList<PointF> normedPoints) {

        mNormedPoints = normedPoints;
//        mCropQuad = new CropQuad(normedPoints, getBitmap().getWidth(), getBitmap().getHeight());
    }

    public void rotate90Degrees() {

//        TODO: The view is rotated, but the scaling is not performed.
        setRotation((getRotation() + 90) % 360);

        invalidate();

    }

    private void initQuadPaint() {

        mQuadPaint = new Paint();
        mQuadPaint.setColor(PAGE_RECT_COLOR);
        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_live_preview_stroke_width));
        mQuadPaint.setAntiAlias(true);
        mQuadPath = new Path();

    }

    private void initOuterQuadPaint() {

        mOuterQuadPaint = new Paint();
        mOuterQuadPaint.setColor(PAGE_OUTER_RECT_COLOR);
        mOuterQuadPaint.setStyle(Paint.Style.STROKE);
        mOuterQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_live_preview_stroke_width));
        mOuterQuadPaint.setAntiAlias(true);
        mOuterQuadPath = new Path();

    }

    private void initCrossPaint() {

        mCrossPaint = new Paint();
        mCrossPaint.setColor(CROSS_COLOR);
        mCrossPaint.setStyle(Paint.Style.STROKE);
        mCrossPaint.setStrokeWidth(getResources().getDimension(R.dimen.cross_stroke_width));
        mCrossPaint.setAntiAlias(true);

    }

    private void initCirclePaint() {

        mCirclePaint = new Paint();
        mCirclePaint.setColor(CROSS_COLOR);
        mCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mCirclePaint.setStrokeWidth(getResources().getDimension(R.dimen.cross_stroke_width));
        mCirclePaint.setAntiAlias(true);

    }

    @Override public void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);

        setMatrix();
        Bitmap bm = getBitmap();
        if (bm != null) {
            canvas.drawBitmap(bm, mMatrix, mPaintBitmap);

            if ((mCropQuad == null) && (mNormedPoints != null)) {
                mCropQuad = new CropQuad(mNormedPoints, getBitmap().getWidth(), getBitmap().getHeight());
                // Tell the CropQuad the transformation:
//                mapCropQuadPoints();
                mapPoints();
            }
            // draw crop frame
            drawCropFrame(canvas);

            // draw the detail where the finger is pointing to:
            if (mIsPointDragged)
                drawDetail(canvas, mCropQuad.getActivePoint());
        }

    }


    private PointF[] viewToImage(PointF point) {

        Matrix inv = new Matrix();
        mMatrix.invert(inv);

        // view coordinates:
        float[] mappedPoint = new float[4];
        mappedPoint[0] = point.x - DETAIL_IMG_WIDTH;
        mappedPoint[1] = point.y - DETAIL_IMG_WIDTH;
        mappedPoint[2] = point.x + DETAIL_IMG_WIDTH;
        mappedPoint[3] = point.y + DETAIL_IMG_WIDTH;

        inv.mapPoints(mappedPoint);

        // map to image coordinates:
//        Bitmap bitmap = getBitmap();
//        if (mappedPoint[0] < 0)
//            mappedPoint[0] = 0;
//        if (mappedPoint[1] < 0)
//            mappedPoint[1] = 0;
//        if (mappedPoint[2] > bitmap.getWidth())
//            mappedPoint[2] = bitmap.getWidth();
//        if (mappedPoint[3] > bitmap.getHeight())
//            mappedPoint[3] = bitmap.getHeight();

//        Touch point in image space:
        PointF[] results = new PointF[2];
        results[0] = new PointF(mappedPoint[0], mappedPoint[1]);
        results[1] = new PointF(mappedPoint[2], mappedPoint[3]);

        return results;

    }

    private void drawDetail(Canvas canvas, PointF point) {

        if (point == null)
            return;

        Bitmap bitmap = getBitmap();
        if (bitmap == null)
            return;

        PointF[] imgPoints = viewToImage(point);

//        Points in image space:
        PointF imgTL = imgPoints[0];
        PointF imgBR = imgPoints[1];

        Log.d(getClass().getName(), "Point imgTL " + imgTL.x + " y: " + imgTL.y);
        Log.d(getClass().getName(), "Point imgBR " + imgBR.x + " y: " + imgBR.y);

        PointF offSet = getDetailOffSet(point);

        float offSetX = offSet.x;
        float offSetY = offSet.y;

        // draw the outline:
        Rect outerRect = new Rect(
                Math.round(point.x - DETAIL_OUTER_WIDTH + offSetX),
                Math.round(point.y - DETAIL_OUTER_WIDTH - offSetY),
                Math.round(point.x + DETAIL_OUTER_WIDTH + offSetX),
                Math.round(point.y + DETAIL_OUTER_WIDTH - offSetY));
        canvas.drawRect(outerRect, mDetailOutlinePaint);

        // draw the bitmap:
        Rect src = new Rect(Math.round(imgTL.x), Math.round(imgTL.y), Math.round(imgBR.x), Math.round(imgBR.y));

        Rect dst = getDestRect(point, imgTL, imgBR, offSet);

        canvas.drawBitmap(bitmap, src, dst, mPaintBitmap);

        // draw the cross:
        canvas.drawLine(point.x - CROSS_WIDTH + offSetX, point.y - offSetY,
                point.x + CROSS_WIDTH + offSetX, point.y -  offSetY, mCrossPaint);
        canvas.drawLine(point.x + offSetX, point.y - offSetY - CROSS_WIDTH,
                point.x + offSetX, point.y - offSetY + CROSS_WIDTH, mCrossPaint);

    }

    private Rect getDestRect(PointF viewPoint, PointF imgTL, PointF imgBR, PointF offSet) {

        int bmpW = getBitmap().getWidth();
        int bmpH = getBitmap().getHeight();

        PointF tl = new PointF(viewPoint.x - DETAIL_WIDTH + offSet.x,viewPoint.y - DETAIL_WIDTH - offSet.y);
        PointF br = new PointF(viewPoint.x + DETAIL_WIDTH + offSet.x, viewPoint.y + DETAIL_WIDTH - offSet.y);

        if (imgTL.x < 0)
            tl.x += getDetailOffSet(Math.abs(imgTL.x)); // shift the detail to the right
        else if (imgBR.x > bmpW)
            br.x -= getDetailOffSet(imgBR.x - bmpW); // shift the detail to the left
        if (imgTL.y < 0)
            tl.y += getDetailOffSet(Math.abs(imgTL.y)); // shift the detail to the bottom
        else if (imgBR.y > bmpH)
            br.y -= getDetailOffSet(imgBR.y - bmpH); // shift the detail to the top

        Rect dst = new Rect(Math.round(tl.x), Math.round(tl.y),
                Math.round(br.x), Math.round(br.y));

        return dst;

    }

    /**
     * Calculates the length of the shift for an detail that would lay partially outside the image border.
     * @param overlapLength
     * @return
     */
    private float getDetailOffSet(float overlapLength) {
        return overlapLength / DETAIL_IMG_WIDTH * DETAIL_WIDTH;
    }

    private PointF getDetailOffSet(PointF point) {

        // This is the usual offset:
        PointF offSet = new PointF(0, DETAIL_OFFSET);

        // Check if the view is rotated:
        int angle = Math.round(getRotation());
        boolean isPointOutside = isPointOutside(point, angle);

        if (isPointOutside) {
            offSet.x = isLeftOfCenter(point, angle) ? DETAIL_OFFSET : -DETAIL_OFFSET;
            offSet.y = 0;
        }

        // Rotate the point according to the view rotation:
        rotatePoint(offSet, angle);

        return offSet;

    }

    private boolean isPointOutside(PointF point, int angle) {

        switch (angle) {
            case 90:
                return point.x < DETAIL_OFFSET;
            case 180:
                return point.y > getBitmap().getHeight() - DETAIL_OFFSET;
            case 270:
                return point.x > getBitmap().getWidth() - DETAIL_OFFSET;
            default:
                return point.y < DETAIL_OFFSET;
        }
    }

    /**
     * Rotates a point according to an angle.
     * @param point
     * @param angle discrete angle (0, 90, 180, 270)
     */
    private void rotatePoint(PointF point, int angle) {

        switch (angle) {

            case 90:
                float tmpY = point.x;
                point.x = -point.y;
                point.y = tmpY;
                break;
            case 180:
                point.x = -point.x;
                point.y = -point.y;
                break;
            case 270:
                float tmpX = point.y;
                point.y = -point.x;
                point.x = tmpX;
                break;
            default:
                break; // do not alter the point here.
        }

    }

    private void rotateNormedPoint(PointF point, int angle) {

        switch (angle) {

            case 90:
                float tmpY = point.x;
                point.x = 1-point.y;
                point.y = tmpY;
                break;
            case 180:
                point.x = 1-point.x;
                point.y = 1-point.y;
                break;
            case 270:
                float tmpX = point.y;
                point.y = 1-point.x;
                point.x = tmpX;
                break;
            default:
                break; // do not alter the point here.

        }

    }


//    private PointF getDetailOffSet(PointF point) {
//
//        // This is the usual offset:
//        PointF offSet = new PointF(0, DETAIL_OFFSET);
//
//        //        Determine if the detail is outside of the screen (with default offset):
//        PointF checkTop = new PointF(point.x,point.y - DETAIL_OFFSET - CROSS_WIDTH);
//        // Is the point above the top?
//        if (!isInsideFrame(checkTop)) {
//            offSet.x = isLeftOfCenter(point) ? DETAIL_OFFSET : -DETAIL_OFFSET;
//            offSet.y = 0;
//        }
//        else {
//            PointF checkLeft = new PointF(point.x - CROSS_WIDTH, point.y);
//            if (!isInsideFrame(checkLeft)) {
//                offSet.x = DETAIL_OFFSET;
//                offSet.y = 0;
//            }
//            else {
//                PointF checkRight = new PointF(point.x + CROSS_WIDTH, point.y);
//                if (!isInsideFrame(checkRight)) {
//                    offSet.x = -DETAIL_OFFSET;
//                    offSet.y = 0;
//                }
//            }
//        }
//
//        return offSet;
//
//    }

    private void drawCropFrame(Canvas canvas) {

//        if ((mCropQuad != null) && (mCropQuad.getViewPoints() != null))
//            drawQuad(canvas);

        if ((mCropQuad != null) && (mCropQuad.getViewPoints() != null)) {
            drawQuad(canvas, mCropQuad.getViewPoints(), mQuadPath, mQuadPaint);
            drawCircles(canvas, mCropQuad.getViewPoints(), mCirclePaint);
        }
//        if ((mCropQuad != null) && (mCropQuad.getOuterViewPoints() != null))
//            drawQuad(canvas, mCropQuad.getOuterViewPoints(), mOuterQuadPath, mOuterQuadPaint);

    }

    private void drawQuad(Canvas canvas, ArrayList<PointF> points, Path path, Paint paint) {

//        canvas.drawCircle(marker1.x, marker1.y, 50, mQuadPaint);

        path.reset();
        boolean isStartSet = false;

        for (PointF point : points) {

            if (!isStartSet) {
                path.moveTo(point.x, point.y);
                isStartSet = true;
            } else
                path.lineTo(point.x, point.y);

            // draw the circle around the corner:
//            Log.d(CLASS_NAME, "drawQuad: point: " + point);
//            canvas.drawCircle(point.x, point.y, CORNER_CIRCLE_RADIUS, mCirclePaint);

        }

        path.close();
        canvas.drawPath(path, paint);

    }

    private void drawCircles(Canvas canvas, ArrayList<PointF> points, Paint paint) {

        for (PointF point : points) {

            canvas.drawCircle(point.x, point.y, CORNER_CIRCLE_RADIUS, paint);

        }

    }



    private void drawQuad(Canvas canvas) {

//        canvas.drawCircle(marker1.x, marker1.y, 50, mQuadPaint);

        mQuadPath.reset();
        boolean isStartSet = false;

        for (PointF point : mCropQuad.getViewPoints()) {

            if (!isStartSet) {
                mQuadPath.moveTo(point.x, point.y);
                isStartSet = true;
            } else
                mQuadPath.lineTo(point.x, point.y);

            // draw the circle around the corner:
            Log.d(CLASS_NAME, "drawQuad: point: " + point);
            canvas.drawCircle(point.x, point.y, CORNER_CIRCLE_RADIUS, mCirclePaint);

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
                if (mIsPointDragged)
                    onMove(event);
                return true;

            case MotionEvent.ACTION_UP:
                onUp(event);
                return true;
        }
        return false;

    }

    private void onUp(MotionEvent e) {
        mIsPointDragged = false;
        invalidate();
    }

    private void onMove(MotionEvent e) {

        PointF point = getPoint(e);

        // Just draw updates if a point is really moved:
        if (isInsideFrame(point)) {

            if (mCropQuad.moveActivePoint(point))
                invalidate();
        }
    }

    private void onDown(MotionEvent e) {

        if ((mCropQuad == null) || (mCropQuad.getImgPoints() == null))
            return;

        invalidate();

        PointF point = getPoint(e);

        if (mCropQuad.isPointClose(point)) {
            mIsPointDragged = true;
        }
        else
            mIsPointDragged = false;

    }

    private boolean isInsideFrame(PointF point) {


        if (mFrameRect.left <= point.x && mFrameRect.right >= point.x) {
            if (mFrameRect.top <= point.y && mFrameRect.bottom >= point.y) {
                return true;
            }
        }
        return false;
    }

    private boolean isLeftOfCenter(PointF point, int rotation) {

        switch (rotation) {
            case 90:
                return point.y > mFrameRect.centerY();
            case 180:
                return point.x > mFrameRect.centerX();
            case 270:
                return point.y < mFrameRect.centerY();
            default:
                return point.x < mFrameRect.centerX();
        }

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

        // setupLayout is also called on device orientation change, we need to initialize mCropQuad
//        again, to rotate90Degrees it.
        if ((getBitmap() != null) && (mNormedPoints != null)) {
            mCropQuad = new CropQuad(mNormedPoints, getBitmap().getWidth(), getBitmap().getHeight());
            // Tell the CropQuad the transformation:
            mapPoints();
        }

        mImageRect = calcImageRect(new RectF(0f, 0f, mImgWidth, mImgHeight), mMatrix);
        mFrameRect = calcFrameRect(mImageRect);

        invalidate();
    }

    private RectF calcImageRect(RectF rect, Matrix matrix) {
        RectF applied = new RectF();
        matrix.mapRect(applied, rect);
        return applied;
    }

    private RectF calcFrameRect(RectF imageRect) {
        float frameW = imageRect.width();
        float frameH = imageRect.height();
        float imgRatio = imageRect.width() / imageRect.height();
        float frameRatio = frameW / frameH;
        float l = imageRect.left, t = imageRect.top, r = imageRect.right, b = imageRect.bottom;
        if (frameRatio >= imgRatio) {
            l = imageRect.left;
            r = imageRect.right;
            float hy = (imageRect.top + imageRect.bottom) * 0.5f;
            float hh = (imageRect.width() / frameRatio) * 0.5f;
            t = hy - hh;
            b = hy + hh;
        } else if (frameRatio < imgRatio) {
            t = imageRect.top;
            b = imageRect.bottom;
            float hx = (imageRect.left + imageRect.right) * 0.5f;
            float hw = imageRect.height() * frameRatio * 0.5f;
            l = hx - hw;
            r = hx + hw;
        }
        float w = r - l;
        float h = b - t;
        float cx = l + w / 2;
        float cy = t + h / 2;
        float sw = w * 1;
        float sh = h * 1;
        return new RectF(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2);
    }


    private ArrayList<PointF> imagePointsToViewPoints(ArrayList<PointF> imgPoints) {

        ArrayList<PointF> viewPoints = new ArrayList<>();

        int numPts = 8;
        float[] imgPointsArray = new float[numPts];

        imgPointsArray[0] = imgPoints.get(0).x;
        imgPointsArray[1] = imgPoints.get(0).y;

        imgPointsArray[2] = imgPoints.get(1).x;
        imgPointsArray[3] = imgPoints.get(1).y;

        imgPointsArray[4] = imgPoints.get(2).x;
        imgPointsArray[5] = imgPoints.get(2).y;

        imgPointsArray[6] = imgPoints.get(3).x;
        imgPointsArray[7] = imgPoints.get(3).y;

        mMatrix.mapPoints(imgPointsArray);

        viewPoints.add(new PointF(imgPointsArray[0], imgPointsArray[1]));
        viewPoints.add(new PointF(imgPointsArray[2], imgPointsArray[3]));
        viewPoints.add(new PointF(imgPointsArray[4], imgPointsArray[5]));
        viewPoints.add(new PointF(imgPointsArray[6], imgPointsArray[7]));

//        mCropQuad.setViewPoints(viewPoints);

        return viewPoints;
    }

    private void mapPoints() {

        ArrayList<PointF> viewPoints = imagePointsToViewPoints(mCropQuad.getImgPoints());
        mCropQuad.setViewPoints(viewPoints);

//        ArrayList<PointF> outerViewPoints = imagePointsToViewPoints(mCropQuad.getOuterImgPoints());
//        mCropQuad.setOuterViewPoints(outerViewPoints);

    }

//    private void mapCropQuadPoints() {
//
//        int numPts = 8;
//        float[] imgPoints = new float[numPts];
//
//        imgPoints[0] = mCropQuad.getImgPoints().get(0).x;
//        imgPoints[1] = mCropQuad.getImgPoints().get(0).y;
//
//        imgPoints[2] = mCropQuad.getImgPoints().get(1).x;
//        imgPoints[3] = mCropQuad.getImgPoints().get(1).y;
//
//        imgPoints[4] = mCropQuad.getImgPoints().get(2).x;
//        imgPoints[5] = mCropQuad.getImgPoints().get(2).y;
//
//        imgPoints[6] = mCropQuad.getImgPoints().get(3).x;
//        imgPoints[7] = mCropQuad.getImgPoints().get(3).y;
//
//        mMatrix.mapPoints(imgPoints);
//
//        ArrayList<PointF> viewPoints = new ArrayList<>();
//        viewPoints.add(new PointF(imgPoints[0], imgPoints[1]));
//        viewPoints.add(new PointF(imgPoints[2], imgPoints[3]));
//        viewPoints.add(new PointF(imgPoints[4], imgPoints[5]));
//        viewPoints.add(new PointF(imgPoints[6], imgPoints[7]));
//
//        mCropQuad.setViewPoints(viewPoints);
//
//    }

    public ArrayList<PointF> getCropPoints() {

        return mapCropQuadPointsInverse();

    }

    private ArrayList<PointF> mapCropQuadPointsInverse() {

        Bitmap bm = getBitmap();
        if (bm == null)
            return null;

        Matrix inv = new Matrix();
        mMatrix.invert(inv);

        int numPts = 8;
        float[] points = new float[numPts];


        points[0] = mCropQuad.getViewPoints().get(0).x;
        points[1] = mCropQuad.getViewPoints().get(0).y;

        points[2] = mCropQuad.getViewPoints().get(1).x;
        points[3] = mCropQuad.getViewPoints().get(1).y;

        points[4] = mCropQuad.getViewPoints().get(2).x;
        points[5] = mCropQuad.getViewPoints().get(2).y;

        points[6] = mCropQuad.getViewPoints().get(3).x;
        points[7] = mCropQuad.getViewPoints().get(3).y;

        inv.mapPoints(points);

        // We norm the points, because the image is down sampled:
        ArrayList<PointF> mappedPoints = new ArrayList<>();
        mappedPoints.add(new PointF(points[0] / bm.getWidth(), points[1] / bm.getHeight()));
        mappedPoints.add(new PointF(points[2] / bm.getWidth(), points[3] / bm.getHeight()));
        mappedPoints.add(new PointF(points[4] / bm.getWidth(), points[5] / bm.getHeight()));
        mappedPoints.add(new PointF(points[6] / bm.getWidth(), points[7] / bm.getHeight()));

        for (PointF point : mappedPoints)
            rotateNormedPoint(point, Math.round(getRotation()));

        return mappedPoints;
//        mCropQuad.setViewPoints(mappedPoints);

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

//    public void rotate90Degrees() {
//        mAngle = (mAngle + 90) % 360;
//        invalidate();
//    }

    private void setMatrix() {

        mMatrix.reset();
        mMatrix.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mScale, mScale, mCenter.x, mCenter.y);
//        mMatrix.postRotate(mAngle, mCenter.x, mCenter.y);
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
