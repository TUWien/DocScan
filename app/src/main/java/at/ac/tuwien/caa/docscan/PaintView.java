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

package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Locale;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Class responsible for drawing the results of the page segmentation and focus measurement tasks.
 * The thread handling is based on this Android example:
 * <a href="https://github.com/Miserlou/Android-SDK-Samples/blob/master/LunarLander">Lunar Lander</a>
 */
public class PaintView extends SurfaceView implements SurfaceHolder.Callback {


    private DrawerThread mDrawerThread;
    private TaskTimer.TimerCallbacks mTimerCallbacks;

    // boolean indicating whether the values of the focus measurement should be drawn:
    private boolean mDrawFocusText;
    private SurfaceHolder mHolder;

    private CVResult mCVResult;


    private static final String TAG = "PaintView";

    /**
     * Creates the PaintView, the timerCallback and the thread responsible for the drawing.
     * @param context of the Activity
     * @param attrs attributes of the activity. Note that we need to pass the attributes in order to
     *              find the view by its ID (findViewById) in the activity.
     */
    public PaintView(Context context, AttributeSet attrs) {

        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // This is necessary to enable semi-transparent DrawView
        holder.setFormat(PixelFormat.TRANSLUCENT);
        mHolder = holder;

        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;

        mDrawerThread = new DrawerThread(holder);

        mDrawFocusText = false;

    }

    /**
     * Method used for turning the drawing of the focus values on or off
     * @param drawText boolean
     */
    public void drawFocusText(boolean drawText) {

        mDrawFocusText = drawText;

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
     * @param holder
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
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mHolder = holder;

//        if (mDrawerThread != null)
//            mDrawerThread.setSurfaceSize(width, height);
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
            synchronized (mCVResult) {
                mCVResult.notify();
            }

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
            synchronized (mCVResult) {
                mCVResult.notify();
            }
            mDrawerThread.setRunning(false);
        }

    }


    /**
     * Class responsible for the actual drawing.
     */
    class DrawerThread extends Thread {

        private SurfaceHolder mSurfaceHolder;
        private boolean mIsRunning;

        private Paint mTextPaint;
        private Paint mSegmentationPaint;
        private Path mSegmentationPath;

        private static final float RECT_HALF_SIZE = 30;
        private static final String TEXT_FORMAT = "%.2f";

        private final int GOOD_TEXT_COLOR = getResources().getColor(R.color.hud_bad_text_color);
        private final int BAD_TEXT_COLOR = getResources().getColor(R.color.hud_good_text_color);
        private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);
        private final int FOCUS_SHARP_RECT_COLOR = getResources().getColor(R.color.hud_focus_sharp_rect_color);
        private final int FOCUS_UNSHARP_RECT_COLOR = getResources().getColor(R.color.hud_focus_unssharp_rect_color);


        private Paint mFocusSharpRectPaint;
        private Paint mFocusUnsharpRectPaint;

        // Used for debug output:

        public DrawerThread(SurfaceHolder surfaceHolder) {


            mSurfaceHolder = surfaceHolder;

//            Initialize drawing stuff:

            // Used to print out measured focus:
            mTextPaint = new Paint();
            // Get device independent screen units:
            // Taken from here: http://stackoverflow.com/questions/3061930/how-to-set-unit-for-paint-settextsize
            int scaledFontSize = getResources().getDimensionPixelSize(R.dimen.draw_view_focus_font_size);
            mTextPaint.setTextSize(scaledFontSize);


            // Used to paint the page segmentation boundaries:
            mSegmentationPaint = new Paint();
            mSegmentationPaint = new Paint();
            mSegmentationPaint.setColor(PAGE_RECT_COLOR);
            mSegmentationPaint.setStyle(Paint.Style.STROKE);
            mSegmentationPaint.setStrokeWidth(7);
            mSegmentationPath = new Path();


            mFocusSharpRectPaint = new Paint();
            mFocusSharpRectPaint.setStrokeWidth(2);
            mFocusSharpRectPaint.setStyle(Paint.Style.STROKE);
            mFocusSharpRectPaint.setColor(FOCUS_SHARP_RECT_COLOR);

            mFocusUnsharpRectPaint = new Paint();
            mFocusUnsharpRectPaint.setStrokeWidth(2);
            mFocusUnsharpRectPaint.setStyle(Paint.Style.STROKE);
            mFocusUnsharpRectPaint.setColor(FOCUS_UNSHARP_RECT_COLOR);

        }

        /**
         * Continuous looping method used for waiting for updates of the CVResult object. The draw
         * method is only called after the CVResult object is updated. Note that this saves a lot of
         * CPU usage.
         * @see <a href="https://developer.android.com/training/custom-views/optimizing-view.html">Optimizing the View</a>
         */
        @Override
        public void run() {

            while (mIsRunning) {

                Canvas canvas = null;
//
                // This wait is used to assure that the drawing function is just called after an update
                // from the native package:
                synchronized (mCVResult) {

                    try {
                        mCVResult.wait();
                    } catch (InterruptedException e) {

                    }

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


//        /* Callback invoked when the surface dimensions change. */
//        public void setSurfaceSize(int width, int height) {
//            // synchronized to make sure these all change atomically
//            synchronized (mSurfaceHolder) {
//                mCanvasWidth = width;
//                mCanvasHeight = height;
//
//            }
//        }

        /**
         * Used for pausing and resuming the drawer thread.
         * @param b
         */
        public void setRunning(boolean b) {

            synchronized (mSurfaceHolder) {

                mIsRunning = b;

            }

        }

        /**
         * Method used for the drawing of the CV result.
         * @param canvas
         */
        private void draw(Canvas canvas) {

            if (canvas == null) {
                return;
            }

            if (CameraActivity.isDebugViewEnabled())
                mTimerCallbacks.onTimerStarted(TaskTimer.DRAW_VIEW_ID);

//            Clear the screen from previous drawings:
            canvas.drawColor(Color.BLUE, PorterDuff.Mode.CLEAR);

            if (mCVResult != null) {


                // Focus measure:
                if (mCVResult.getPatches() != null)
                    drawFocusMeasure(canvas);

                // Page segmentation:
                if (mCVResult.getDKPolyRects() != null)
                    drawPageSegmentation(canvas);

            }

            if (CameraActivity.isDebugViewEnabled())
                mTimerCallbacks.onTimerStopped(TaskTimer.DRAW_VIEW_ID);

        }

        /**
         * Draws the output of the page segmentation task.
         * @param canvas
         */
        private void drawPageSegmentation(Canvas canvas) {

            for (DkPolyRect dkPolyRect : mCVResult.getDKPolyRects()) {

                mSegmentationPath.reset();

                ArrayList<PointF> screenPoints = dkPolyRect.getScreenPoints();

                boolean isStartSet = false;

                for (PointF point : screenPoints) {

                    if (!isStartSet) {
                        mSegmentationPath.moveTo(point.x, point.y);
                        isStartSet = true;
                    } else
                        mSegmentationPath.lineTo(point.x, point.y);

                }

                mSegmentationPath.close();
                canvas.drawPath(mSegmentationPath, mSegmentationPaint);


            }

        }

        /**
         * Draws the output of the focus measurement task.
         * @param canvas
         */
        private void drawFocusMeasure(Canvas canvas) {

            Rect textRect = new Rect();
            float textWidth = -1;
//            float textHeight = -1;

            for (Patch patch : mCVResult.getPatches()) {

                String fValue = String.format(Locale.ENGLISH, TEXT_FORMAT, patch.getFM());

                if (patch.getIsForeGround()) {

                    if (patch.getIsSharp()) {

                        if (mDrawFocusText)
                            mTextPaint.setColor(GOOD_TEXT_COLOR);

                        canvas.drawRect(patch.getDrawViewPX() - RECT_HALF_SIZE, patch.getDrawViewPY() - RECT_HALF_SIZE,
                                patch.getDrawViewPX() + RECT_HALF_SIZE, patch.getDrawViewPY() + RECT_HALF_SIZE, mFocusSharpRectPaint);

                    }
                    else {

                        if (mDrawFocusText)
                            mTextPaint.setColor(BAD_TEXT_COLOR);

                        canvas.drawRect(patch.getDrawViewPX() - RECT_HALF_SIZE, patch.getDrawViewPY() - RECT_HALF_SIZE,
                                patch.getDrawViewPX() + RECT_HALF_SIZE, patch.getDrawViewPY() + RECT_HALF_SIZE, mFocusUnsharpRectPaint);
                    }


                    if (mDrawFocusText) {

                        // Get the size of the text in screen units so that we can center the text:
                        // (Do this just for the first iteration.)
                        if (textWidth == -1) {
                            mTextPaint.getTextBounds(fValue, 0, fValue.length(), textRect);
                            textWidth = (float) textRect.width() / 2;
                        }

                        canvas.drawText(fValue, patch.getDrawViewPX() - textWidth,
                                patch.getDrawViewPY() - RECT_HALF_SIZE - 10, mTextPaint);

                    }

                }

            }


        }

    }

 }
