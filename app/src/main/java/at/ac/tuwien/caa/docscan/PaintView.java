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
 * Based on Lunar Lander example:
 * https://github.com/Miserlou/Android-SDK-Samples/blob/master/LunarLander
 */

public class PaintView extends SurfaceView implements SurfaceHolder.Callback {


    private DrawerThread mDrawerThread;
    private TaskTimer.TimerCallbacks mTimerCallbacks;

    private boolean mDrawFocusText;
    private SurfaceHolder mHolder;

    private CVResult mCVResult;


    private static final String TAG = "PaintView";


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


    public void drawFocusText(boolean drawText) {

        mDrawFocusText = drawText;

    }

    public boolean isFocusTextVisible() {

        return mDrawFocusText;

    }




    public void setCVResult(CVResult cvResult) {

        mCVResult = cvResult;

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

        mHolder = holder;
        mDrawerThread.setRunning(true);

        if (mDrawerThread.getState() == Thread.State.NEW)
            mDrawerThread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mHolder = holder;

        if (mDrawerThread != null)
            mDrawerThread.setSurfaceSize(width, height);
    }


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

        private int mCanvasWidth, mCanvasHeight;


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


        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

            }
        }

        public void setRunning(boolean b) {

            synchronized (mSurfaceHolder) {

                mIsRunning = b;

            }

        }

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
