/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   12. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Locale;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.DkVector;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.ui.CameraActivity;

/**
 * Class responsible for drawing the results of the page segmentation and focus measurement tasks.
 * The thread handling is based on this Android example:
 * <a href="https://github.com/Miserlou/Android-SDK-Samples/blob/master/LunarLander">Lunar Lander</a>
 */
public class PaintView extends SurfaceView implements SurfaceHolder.Callback {


    private static final String CLASS_NAME = "PaintView";
    private Paint mGridPaint;
    private DrawerThread mDrawerThread;
    private TaskTimer.TimerCallbacks mTimerCallbacks;

    // boolean indicating whether the values of the focus measurement should be drawn:
    private boolean mDrawFocusText;
    private boolean mDrawGuideLines = false;
    private boolean mDrawMovementIndicator = false;
    private boolean mDrawWaitingIndicator = false;
    private boolean mDrawFocusTouch = false;
    private PointF mFocusTouchPoint;
    private long mFocusTouchStart;
    private SurfaceHolder mHolder;
    private static Flicker mFlicker;
    private CVResult mCVResult;
    private boolean mDrawGrid = false;
//    This is basically the exif orientation, but we do not name it this way in the UI
    private int mTextOrientation = CameraActivity.IMG_ORIENTATION_0;
    private boolean mDrawTextDirLarge = false;

    /**
     * Creates the PaintView, the timerCallback and the thread responsible for the drawing.
     * @param context of the Activity
     * @param attrs attributes of the activity. Note that we need to pass the attributes in order to
     *              find the view by its ID (findViewById) in the activity.
     */
    public PaintView(Context context, AttributeSet attrs) {

        super(context, attrs);

        // This is necessary to assure that the view is on top of the camera view. On some devices
        // like Galaxy Tab 10.1 this was not always the case.
        this.setZOrderMediaOverlay(true);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // This is necessary to enable semi-transparent DrawView
        holder.setFormat(PixelFormat.TRANSLUCENT);
        mHolder = holder;

        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;

        mDrawerThread = new DrawerThread(holder);

        mDrawFocusText = false;

        mFlicker = new Flicker();

    }

    /**
     * Method used for turning the drawing of the focus values on or off
     * @param drawText boolean
     */
    public void drawFocusText(boolean drawText) {

        mDrawFocusText = drawText;

    }

    public void setTextOrientation(int textOrientation) {

        mTextOrientation = textOrientation;

    }

    public void drawTextOrientationLarge(boolean large) {

        mDrawTextDirLarge = large;

    }

    public void drawGrid(boolean drawGrid) {

        mDrawGrid = drawGrid;

    }

    /**
     * Returns if the focus values are drawn.
     * @return boolean
     */
    public boolean isFocusTextVisible() {

        return mDrawFocusText;

    }

    /**
     * Sets the CVResult. The draw method is only called if the CVResult notifies the PaintView that
     * it has been updated.
     * @param cvResult CVResult to which the PaintView is connected
     */
    public void setCVResult(CVResult cvResult) {

        mCVResult = cvResult;

    }

    /**
     * Called after the surface is created.
     * @param holder owner of the surface
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

        mHolder = holder;
        mDrawerThread.setRunning(true);

        if (mDrawerThread.getState() == Thread.State.NEW)
            mDrawerThread.start();

    }

    /**
     * Called after the surface is changed.
     * @param holder owner of the surface
     * @param format format
     * @param width width of the surface
     * @param height height of the surface
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mHolder = holder;

    }

    /**
     * Called if the surface is destroyed. Shuts down the drawer thread.
     * @param holder the surface holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        mDrawerThread.setRunning(false);
        while (retry) {
            try {
                mDrawerThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Crashlytics.logException(e);
                Log.d(CLASS_NAME, e.toString());
            }
        }

    }

    /**
     * Called if the activity is resumed. Here the drawer thread is recreated if necessary.
     */
    public void resume() {



        if (mDrawerThread != null) {
            // It is necessary to call notify here, because otherwise there will be a deadlock in the
            // run method, which waits for the mCVResult object. The deadlock will arise after the
            // app is resumed (for example after an orientation change).
            forceDrawUpdate();

            if (mDrawerThread.getState() == Thread.State.TERMINATED) {

                mDrawerThread = new DrawerThread(mHolder);
                mDrawerThread.setRunning(true);
                mDrawerThread.start();
            }
            else
                mDrawerThread.setRunning(true);
        }


    }

    /**
     * Pauses the drawer thread.
     */
    public void pause() {

        if (mDrawerThread != null) {
            // It is necessary to call notify here, because otherwise there will be a deadlock in the
            // run method, which waits for the mCVResult object. The deadlock will arise after the
            // app is resumed (for example after an orientation change).
            forceDrawUpdate();
            mDrawerThread.setRunning(false);
        }

    }


    public void drawGuideLines(boolean drawGuideLines) {

        mDrawGuideLines = drawGuideLines;

    }

    public boolean areGuideLinesDrawn() {

        return mDrawGuideLines;

    }

    public void showFlicker() {

        mFlicker.mVisible = true;
        forceDrawUpdate();

    }

    public void clearScreen() {

        mFlicker.mVisible = false;
        mDrawMovementIndicator = false;
        forceDrawUpdate();

    }

    public void drawMovementIndicator(boolean drawMovementIndicator) {

        mDrawMovementIndicator = drawMovementIndicator;
        forceDrawUpdate();

    }

    public void drawWaitingIndicator(boolean drawWaitingIndicator) {

        mDrawWaitingIndicator = drawWaitingIndicator;
        forceDrawUpdate();

    }

    private void forceDrawUpdate() {

        // This is necessary, since we do not want to wait for the next update from the mCVResult:
        synchronized (mCVResult) {
            mCVResult.notify();
        }

    }

    public void drawFocusTouch(PointF point) {

        mDrawFocusTouch = true;
        mFocusTouchPoint = point;
        mFocusTouchStart = System.nanoTime();

        Log.d(CLASS_NAME, "focus tocuhed");
    }

    public void drawFocusTouchSuccess() {

        mDrawFocusTouch = false;

    }


    /**
     * Class responsible for the actual drawing.
     */
    class DrawerThread extends Thread {

        private static final String TAG = "DrawerThread";
        private SurfaceHolder mSurfaceHolder;
        private boolean mIsRunning;


        private Paint mTextPaint;
        private Paint mBGPaint;
        private Paint mSegmentationPaint;
        private Path mSegmentationPath;
        private Path mFocusPath;
        private Paint mGuidePaint;
        private Paint mMovementPaint;
        private Paint mWaitingFramePaint;

        private final float FOCUS_RECT_HEIGHT = getResources().getDimension(R.dimen.focus_height);
        private final float FOCUS_RECT_HALF_HEIGHT = FOCUS_RECT_HEIGHT / 2;
        private final float FOCUS_RECT_SHORT_LENGTH = getResources().getDimension(R.dimen.focus_short_length);
        private final float FOCUS_RECT_OFFSET = (float) (FOCUS_RECT_SHORT_LENGTH * 1.5);
        private static final String TEXT_FORMAT = "%.2f";

        // Focus touch:
        private final long FOCUS_TOUCH_ANIMATION_DURATION =     200000000; // in nano seconds
        private final long FOCUS_TOUCH_MAX_DURATION =           15000000000L; // in nano seconds
        private final float FOCUS_TOUCH_CIRCLE_RADIUS_START =
                getResources().getDimension(R.dimen.focus_touch_circle_radius_start);
        private final float FOCUS_TOUCH_CIRCLE_RADIUS_END =
                getResources().getDimension(R.dimen.focus_touch_circle_radius_end);
        private Paint mFocusTouchCirclePaint;
        private Paint mFocusTouchOutlinePaint;

        private final int STATE_TEXT_BG_COLOR = getResources().getColor(R.color.hud_state_rect_color);
        private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);
        private final int FOCUS_SHARP_RECT_COLOR = getResources().getColor(R.color.hud_focus_sharp_rect_color);
        private final int FOCUS_UNSHARP_RECT_COLOR = getResources().getColor(R.color.hud_focus_unssharp_rect_color);
        private final int HELPER_LINE_COLOR = getResources().getColor(R.color.hud_helper_line_color);


        private Paint mFocusSharpRectPaint;
        private Paint mFocusUnsharpRectPaint;

        private Bitmap mTextDirBitmap, mTextDirLargeBitmap;

        // Used for debug output:x

        public DrawerThread(SurfaceHolder surfaceHolder) {


            mSurfaceHolder = surfaceHolder;

//            Initialize drawing stuff:

            // Used to print out measured focus:
            mTextPaint = new Paint();
            // Get device independent screen units:
            // Taken from here: http://stackoverflow.com/questions/3061930/how-to-set-unit-for-paint-settextsize
            int scaledFontSize = getResources().getDimensionPixelSize(R.dimen.draw_view_focus_font_size);
            mTextPaint.setTextSize(scaledFontSize);

            // Used to print out text direction:
            int textSize = getResources().getDimensionPixelSize(R.dimen.draw_view_textdir_font_size);
            mTextDirBitmap = textAsBitmap("T", textSize);
            int textLargeSize = getResources().getDimensionPixelSize(R.dimen.draw_view_textdirlarge_font_size);
            mTextDirLargeBitmap = textAsBitmap("T", textLargeSize);

            // Used to paint the page segmentation boundaries:
            mSegmentationPaint = new Paint();
            mSegmentationPaint.setColor(PAGE_RECT_COLOR);
            mSegmentationPaint.setStyle(Paint.Style.STROKE);
            mSegmentationPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_live_preview_stroke_width));
            mSegmentationPaint.setAntiAlias(true);
            mSegmentationPath = new Path();

            mFocusSharpRectPaint = new Paint();
            mFocusSharpRectPaint.setStrokeWidth(getResources().getDimension(R.dimen.focus_stroke_width));

            mFocusSharpRectPaint.setStyle(Paint.Style.STROKE);
            mFocusSharpRectPaint.setColor(FOCUS_SHARP_RECT_COLOR);

            mFocusUnsharpRectPaint = new Paint();
            mFocusUnsharpRectPaint.setStrokeWidth(getResources().getDimension(R.dimen.focus_stroke_width));
            mFocusUnsharpRectPaint.setStyle(Paint.Style.STROKE);
            mFocusUnsharpRectPaint.setColor(FOCUS_UNSHARP_RECT_COLOR);
            mFocusPath = new Path();

            mGuidePaint = new Paint();
            mGuidePaint.setStrokeWidth(2);
            mGuidePaint.setStyle(Paint.Style.STROKE);
            mGuidePaint.setColor(HELPER_LINE_COLOR);

            mBGPaint = new Paint();
            mBGPaint.setColor(STATE_TEXT_BG_COLOR);

            mMovementPaint = new Paint();
            mMovementPaint.setStyle(Paint.Style.FILL);
            mMovementPaint.setColor(getResources().getColor(R.color.movement_color));

            mWaitingFramePaint = new Paint();
            mWaitingFramePaint.setStyle(Paint.Style.FILL);
            mWaitingFramePaint.setColor(getResources().getColor(R.color.waiting_frame_color));

            mFocusTouchCirclePaint = new Paint();
            mFocusTouchCirclePaint.setColor(getResources().getColor(R.color.focus_touch_circle_fill));
            mFocusTouchCirclePaint.setStyle(Paint.Style.FILL);
            mFocusTouchCirclePaint.setStrokeWidth(getResources().getDimension(R.dimen.focus_circle_stroke_width));

            mGridPaint = new Paint();
            mGridPaint.setColor(getResources().getColor(R.color.grid_color));
            mGridPaint.setStyle(Paint.Style.STROKE);
            mGridPaint.setStrokeWidth(getResources().getDimension(R.dimen.focus_circle_stroke_width));

            mFocusTouchOutlinePaint = new Paint();
            mFocusTouchOutlinePaint.setColor(getResources().getColor(R.color.focus_touch_circle_outline));
            mFocusTouchOutlinePaint.setStyle(Paint.Style.STROKE);
            mFocusTouchOutlinePaint.setStrokeWidth(getResources().getDimension(R.dimen.focus_circle_stroke_width));
            mFocusTouchOutlinePaint.setAntiAlias(true);
        }

        private Bitmap textAsBitmap(String text, int textSize) {

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            Typeface font = Typeface.createFromAsset(getResources().getAssets(), "ptserif.ttf");
            paint.setTextSize(textSize);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(font);

            float baseline = -paint.ascent(); // ascent() is negative
            int width = (int) (paint.measureText(text) + 0.5f); // round
            int height = (int) (baseline + paint.descent() + 0.5f);
            Bitmap orig = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(orig);
            Paint bg = new Paint();
//            bg.setColor(getResources().getColor(R.color.grid_color));
            bg.setColor(Color.WHITE);
            RectF r = new RectF(0, 0, orig.getHeight(), orig.getHeight());
            canvas.drawRoundRect(r, 10, 10, bg);
            canvas.drawText(text, orig.getWidth() / 2f - width / 2f, baseline, paint);

            return orig;

        }



        /**
         * Continuous looping method used for waiting for updates of the CVResult object. The draw
         * method is only called after the CVResult object is updated. Note that this saves a lot of
         * CPU usage.
         * @see <a href="https://developer.android.com/training/custom-views/optimizing-view.html">Optimizing the View</a>
         */
        @Override
        public void run() {

            long mLastTime = 0;

            while (mIsRunning) {

                Canvas canvas = null;

                if (mFlicker.mVisible)
                    mFlicker.draw();

                else {

                    // This wait is used to assure that the drawing function is just called after an update
                    // from the native package:
                    synchronized (mCVResult) {

                        try {
                            mCVResult.wait(50);
                        } catch (InterruptedException e) {
                            Crashlytics.logException(e);
                            Log.d(TAG, e.toString());
                        }

                        if (!mCVResult.isRedrawNecessary()) {
                            if (mSegmentationPaint.getAlpha() >= 170)
                                mSegmentationPaint.setAlpha(mSegmentationPaint.getAlpha() - 20);
                        }
                        else {
                            mSegmentationPaint.setAlpha(221);
                            mFocusSharpRectPaint.setAlpha(170);
                            mFocusUnsharpRectPaint.setAlpha(170);
                        }


                        if (mFlicker.mVisible)
                            continue;

                        try {
                            canvas = mSurfaceHolder.lockCanvas();
                            draw(canvas);
                        } finally {
                            // do this in a finally so that if an exception is thrown
                            // during the above, we don't leave the Surface in an
                            // inconsistent state
                            if (canvas != null) {
                                mSurfaceHolder.unlockCanvasAndPost(canvas);
                            }
                        }

                    }
                }


            }
        }

        /**
         * Used for pausing and resuming the drawer thread.
         * @param b boolean indicating if the thread is running
         */
        public void setRunning(boolean b) {

            synchronized (mSurfaceHolder) {

                mIsRunning = b;

            }

        }



        /**
         * Method used for the drawing of the CV result.
         * @param canvas canvas
         */
        private void draw(Canvas canvas) {

            if (canvas == null) {
                return;
            }

//            if (CameraActivity.isDebugViewEnabled())
//                mTimerCallbacks.onTimerStarted(DRAW_VIEW);

//            Clear the screen from previous drawings:
            canvas.drawColor(Color.BLUE, PorterDuff.Mode.CLEAR);

            if (mDrawMovementIndicator) {
                Rect rect = new Rect(0,0, getWidth(), getHeight());
                canvas.drawRect(rect, mMovementPaint);
            }

            if (mDrawWaitingIndicator) {
                Rect rect = new Rect(0,0, getWidth(), getHeight());
                canvas.drawRect(rect, mWaitingFramePaint);
            }

            if (mDrawGrid)
                drawGrid(canvas);

            if (mDrawFocusTouch)
                drawFocusCircle(canvas);

            if (mCVResult != null) {

                ArrayList<PointF> points = null;
                if (mCVResult.getDKPolyRects() != null && mCVResult.getDKPolyRects().length > 0)
                    points = mCVResult.getDKPolyRects()[0].getScreenPoints();

                drawTextOrientation(canvas, points);
                // Page segmentation:
                if (mCVResult.getDKPolyRects() != null) {
                    drawPageSegmentation(canvas);

//                    This prevents that the focus patches are drawn, although there is no polyRect found:
                    if (mCVResult.getDKPolyRects().length > 0) {
                        // Focus measure:
                        if (mCVResult.getPatches() != null)
                            drawFocusMeasure(canvas);
                    }
                }
            }

        }

        private void drawFocusCircle(Canvas canvas) {

            long timeDiff = System.nanoTime() - mFocusTouchStart;

            // Just draw the circle if the duration has not passed:
            if (timeDiff <= FOCUS_TOUCH_MAX_DURATION) {

                float radius = FOCUS_TOUCH_CIRCLE_RADIUS_END; // used for static display
                // Animate:
                if (timeDiff <= FOCUS_TOUCH_ANIMATION_DURATION) {
                    mFocusTouchCirclePaint.setAlpha(150);
                    Log.d(TAG, "focus animation");
                    radius = (int) (FOCUS_TOUCH_CIRCLE_RADIUS_END +
                            Math.round(FOCUS_TOUCH_CIRCLE_RADIUS_START - FOCUS_TOUCH_CIRCLE_RADIUS_END)
                                    * (1 - (float) timeDiff / FOCUS_TOUCH_ANIMATION_DURATION));
                }
                else {
                    Log.d(TAG, "focus stopped animation");
                    int alpha = mFocusTouchCirclePaint.getAlpha() - 20;
                    if (alpha < 0)
                        alpha = 0;
                    mFocusTouchCirclePaint.setAlpha(alpha);
                }

                canvas.drawCircle(mFocusTouchPoint.x, mFocusTouchPoint.y, radius, mFocusTouchCirclePaint); // Filling
                canvas.drawCircle(mFocusTouchPoint.x, mFocusTouchPoint.y, radius, mFocusTouchOutlinePaint); // Outline

            }
            else
                mDrawFocusTouch = false;

        }


        private void drawGrid(Canvas canvas) {

            int gridColumnNum = 3;
            int gridRowNum = 3;

//            Draw the vertical lines:
            for (int i = 0; i < gridColumnNum - 1; i++) {
                float x = ((float) getWidth() / gridColumnNum) * (i+1);
                canvas.drawLine(x, 0, x, getHeight(), mGridPaint);
            }

//            Draw the horizontal lines:
            for (int i = 0; i < gridRowNum - 1; i++) {
                float y = ((float) getHeight() / gridRowNum) * (i+1);
                canvas.drawLine(0, y, getWidth(), y, mGridPaint);
            }

        }

        /**
         * Draws the output of the page segmentation task.
         * @param canvas canvas
         */
        private void drawPageSegmentation(Canvas canvas) {


            for (DkPolyRect dkPolyRect : mCVResult.getDKPolyRects()) {

                mSegmentationPath.reset();
                ArrayList<PointF> screenPoints = dkPolyRect.getScreenPoints();
                boolean isStartSet = false;

                if (screenPoints == null)
                    return;


//                int idx = 0; // used for the drawing of the corner numbers
                for (PointF point : screenPoints) {

                    if (!isStartSet) {
                        mSegmentationPath.moveTo(point.x, point.y);
                        isStartSet = true;
                    } else
                        mSegmentationPath.lineTo(point.x, point.y);

                    if (mDrawGuideLines) {
                        canvas.drawLine(0, point.y, getWidth(), point.y, mGuidePaint);
                        canvas.drawLine(point.x, 0, point.x, getHeight(), mGuidePaint);
                    }

//                    String cornerNum = String.format(Locale.ENGLISH, "%d", idx);
//
//                    if (point == topLeft)
//                        cornerNum += "!";
//
////                    Draw the corner number:
//                    canvas.drawText(cornerNum, point.x, point.y, mTextPaint);
//                    idx++;
                }

                mSegmentationPath.close();
                canvas.drawPath(mSegmentationPath, mSegmentationPaint);

            }

        }


        private void drawTextOrientation(Canvas canvas, ArrayList<PointF> screenPoints) {

            double angle;
            PointF tl;
            if (screenPoints == null) {
                angle = getTextAngle(mTextOrientation);
                if (angle == -1)
                    return;
                tl = new PointF(getWidth() / 2f, getHeight() / 2f);
            }
            else {
                int csIdx = getTopLeftTextIdx(screenPoints);

                if (csIdx == -1)
                    return;

                int ceIdx = (csIdx + 1) % 4;

                PointF s = screenPoints.get(csIdx);
                PointF e = screenPoints.get(ceIdx);

                Log.d(CLASS_NAME, "s: " + s + " e: " + e);

//            Determine the angle of the text rotation:
                DkVector v = new DkVector(e, s);
                DkVector y = new DkVector(1, 0);
                if (v.y < 0)
                    angle = (360 - v.angle(y)) % 360;
                else
                    angle = v.angle(y) % 360;

                Log.d(CLASS_NAME, "angle: " + angle);

                tl = screenPoints.get(csIdx);
//            Make a deep copy, because we will modify the point:
                tl = new PointF(tl.x, tl.y);

                if (Float.isNaN((float) angle))
                    return;
            }

            drawTextDirBitmap(canvas, (float) angle, tl, screenPoints != null);

        }

        /**
         * Draws the text dir bitmap
         * @param canvas
         * @param angle
         * @param tl
         */
        private void drawTextDirBitmap(Canvas canvas, float angle, PointF tl, boolean drawInPage) {
            Bitmap bm;
            if (mDrawTextDirLarge)
                bm = mTextDirLargeBitmap;
            else
                bm = mTextDirBitmap;


            if ((bm != null) && (bm.getWidth() > 0) && (bm.getHeight() > 0)) {

                Matrix matrix = new Matrix();
                matrix.setRotate(angle);

                Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                if (rotatedBitmap == null)
                    return;

//                Determine the offset, not used currently:
//                DkVector half = new DkVector(v.x + lv.x / 2.f, v.y + lv.y / 2.f);
//                DkVector offset = half.norm().multiply(30);
                if (drawInPage) {
                    if (mTextOrientation == CameraActivity.IMG_ORIENTATION_90) {
                        tl.x -= rotatedBitmap.getWidth();
                    } else if (mTextOrientation == CameraActivity.IMG_ORIENTATION_180) {
                        tl.x -= rotatedBitmap.getWidth();
                        tl.y -= rotatedBitmap.getHeight();
                    } else if (mTextOrientation == CameraActivity.IMG_ORIENTATION_270) {
                        tl.y -= rotatedBitmap.getHeight();
                    }
                }
                else {
                    tl.x -= rotatedBitmap.getWidth() / 2f;
                    tl.y -= rotatedBitmap.getHeight() / 2f;
                }
                canvas.drawBitmap(rotatedBitmap, tl.x, tl.y, new Paint());

            }
        }

        private int getTextAngle(int textOrientation) {

            switch (textOrientation) {
                case CameraActivity.IMG_ORIENTATION_0:
                    return 0;
                case CameraActivity.IMG_ORIENTATION_90:
                    return 90;
                case CameraActivity.IMG_ORIENTATION_180:
                    return 180;
                case CameraActivity.IMG_ORIENTATION_270:
                    return 270;
                default:
                    return -1;
            }
        }

        private int getTopLeftTextIdx(ArrayList<PointF> screenPoints) {

//            Get the top left corner - regardless of text orientation:
            PointF topLeft = getTopLeftCorner(screenPoints);
            if (topLeft == null)
                return -1;

            int tlIdx = screenPoints.indexOf(topLeft);
            if (tlIdx == -1)
                return -1;

//            int offset;
//            switch (mTextOrientation) {
//                default:
//                    offset = 0;
//                    break;
//                case 90:
//                    offset = 1;
//                    break;
//                case 180:
//                    offset = 2;
//                    break;
//                case 270:
//                    offset = 3;
//                    break;
//            }

            return (tlIdx + mTextOrientation) % 4;
        }

        /**
         * Returns the left top corner point, depending on the orientation of the text.
         * @param points
         * @return
         */
        private PointF getTopLeftCorner(ArrayList<PointF> points) {

            PointF result = null;
            float mid = getMidPoint(points);
            float x = Float.POSITIVE_INFINITY;

            for (PointF point : points) {
//                Skip bottom points:
                if (point.y > mid)
                    continue;
//                Find the outer left point:
                if (point.x < x) {
                    result = point;
                    x = point.x;
                }
            }

            return result;

        }

        /**
         * Returns the mid point of the bounding box in x or y direction.
         * @param points
         * @return
         */
        private float getMidPoint(ArrayList<PointF> points) {

            // Get points that have the largest distance in x or y direction:
            float s = Float.POSITIVE_INFINITY;
            float e = 0;
            for (PointF point : points) {
                float p = point.y;
                if (p < s)
                    s = p;
                if (p > e)
                    e = p;
            }

            return s + (e-s) / 2;
        }

        /**
         * Draws the output of the focus measurement task.
         * @param canvas canvas
         */
        private void drawFocusMeasure(Canvas canvas) {

            Rect textRect = new Rect();
            float textWidth = -1;

            mFocusPath.reset();

            for (Patch patch : mCVResult.getPatches()) {

                String fValue = String.format(Locale.ENGLISH, TEXT_FORMAT, patch.getFM());

                if (patch.getIsForeGround()) {

                    if (patch.getIsSharp()) {

                        if (mDrawFocusText)
                            mTextPaint.setColor(FOCUS_SHARP_RECT_COLOR);

                        drawFocusRect(canvas, mFocusSharpRectPaint, patch.getDrawViewPX(), patch.getDrawViewPY());

                    }
                    else {

                        if (mDrawFocusText)
                            mTextPaint.setColor(FOCUS_UNSHARP_RECT_COLOR);

                        drawFocusRect(canvas, mFocusUnsharpRectPaint, patch.getDrawViewPX(), patch.getDrawViewPY());


                    }


                    if (mDrawFocusText) {

                        // Get the size of the text in screen units so that we can center the text:
                        // (Do this just for the first iteration.)
                        if (textWidth == -1) {
                            mTextPaint.getTextBounds(fValue, 0, fValue.length(), textRect);
                            textWidth = (float) textRect.width() / 2;
                        }


                        canvas.drawText(fValue, patch.getDrawViewPX() - textWidth,
                                patch.getDrawViewPY(), mTextPaint);

                    }
                }
            }
        }

        private void drawFocusRect(Canvas canvas, Paint paint, float x, float y) {

            mFocusPath.reset();

            mFocusPath.moveTo(x - FOCUS_RECT_OFFSET + FOCUS_RECT_SHORT_LENGTH, y - FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x - FOCUS_RECT_OFFSET, y - FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x - FOCUS_RECT_OFFSET, y + FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x - FOCUS_RECT_OFFSET + FOCUS_RECT_SHORT_LENGTH, y + FOCUS_RECT_HALF_HEIGHT);

            mFocusPath.moveTo(x + FOCUS_RECT_OFFSET - FOCUS_RECT_SHORT_LENGTH, y - FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x + FOCUS_RECT_OFFSET, y - FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x + FOCUS_RECT_OFFSET, y + FOCUS_RECT_HALF_HEIGHT);
            mFocusPath.lineTo(x + FOCUS_RECT_OFFSET - FOCUS_RECT_SHORT_LENGTH, y + FOCUS_RECT_HALF_HEIGHT);

            canvas.drawPath(mFocusPath, paint);

        }
    }

    /**
     * This class is used when a picture is taken.
     */
    private class Flicker {

        private Paint mFlickerPaint;
        private boolean mVisible;

        private Flicker() {

            mFlickerPaint = new Paint();
            mFlickerPaint.setStyle(Paint.Style.FILL);
            mFlickerPaint.setColor(getResources().getColor(R.color.flicker_color));
            mVisible = false;

        }

        private void draw() {

            Canvas canvas = mHolder.lockCanvas();

            if (canvas == null)
                return;

            Rect rect = new Rect(0,0, getWidth(), getHeight());
            canvas.drawRect(rect, mFlickerPaint);

            mHolder.unlockCanvasAndPost(canvas);

            // After the painting wait some time:
            long startTime = System.currentTimeMillis();
            long currentTime = -1;

            while(currentTime - startTime < getResources().getInteger(R.integer.flicker_time))
                currentTime = System.currentTimeMillis();

            // Remove the painting:
            canvas = mHolder.lockCanvas();
            canvas.drawColor(Color.BLUE, PorterDuff.Mode.CLEAR);

            mHolder.unlockCanvasAndPost(canvas);

            mVisible = false;

        }

    }

 }
