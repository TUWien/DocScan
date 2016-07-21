package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import at.ac.tuwien.caa.docscan.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.cv.Patch;

/**
 * Based on Lunar Lander example:
 * https://github.com/Miserlou/Android-SDK-Samples/blob/master/LunarLander
 */

public class PaintView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private DrawerThread mDrawerThread;
    private TaskTimer.TimerCallbacks mTimerCallbacks;

    public PaintView(Context context, AttributeSet attrs) {

        super(context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // This is necessary to enable semi-transparent DrawView
        holder.setFormat(PixelFormat.TRANSLUCENT);

        mTimerCallbacks = (TaskTimer.TimerCallbacks) context;


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

        mDrawerThread = new DrawerThread(holder);
        mDrawerThread.setRunning(true);
        // TODO: check why the thread is already started - if the app is restarted. The thread should be dead!
        if (mDrawerThread.getState() == Thread.State.NEW)
            mDrawerThread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


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

        if (mDrawerThread != null)
            mDrawerThread.setRunning(true);


    }

    public void pause() {

        if (mDrawerThread != null)
            mDrawerThread.setRunning(false);

    }

    class DrawerThread extends Thread {

        private SurfaceHolder mSurfaceHolder;
        private boolean mIsRunning;

        private Paint mTextPaint;
        private Paint mSegmentationPaint;
        private Patch[] mFocusPatches;
        private DkPolyRect[] mPolyRects;
        private Path mSegmentationPath;
        private final int GOOD_TEXT_COLOR = getResources().getColor(R.color.hud_bad_text_color);
        private final int BAD_TEXT_COLOR = getResources().getColor(R.color.hud_good_text_color);
        private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);

        private int mCanvasWidth, mCanvasHeight;

        private Paint mRectPaint;

        // Used for debug output:

        public DrawerThread(SurfaceHolder surfaceHolder) {


            mSurfaceHolder = surfaceHolder;

//            Initialize drawing stuff:

            // Used to print out measured focus:
            mTextPaint = new Paint();

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

            // Used for debugging rectangle
            mRectPaint = new Paint();

        }



        private void setFocusPatches(Patch[] focusPatches) {

            mFocusPatches = focusPatches;

        }

        private void setPolyRects(DkPolyRect[] polyRects) {

            mPolyRects = polyRects;

        }

        @Override
        public void run() {

            while (mIsRunning) {

                Canvas canvas = null;

                try {
                    canvas = mSurfaceHolder.lockCanvas();
//                    synchronized (mSurfaceHolder) {
//                        draw(canvas);
//                    }

                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

            }

//            if (mCameraView == null)
//                return;

//            if (mSurfaceHolder == null)
//                return;
//
//            synchronized (mSurfaceHolder) {
//
//                while (mIsRunning) {
//
//                    Canvas canvas = null;
//
//                    try {
//
//                        canvas = mSurfaceHolder.lockCanvas(null);
//
////                        synchronized (mCameraView) {
////
////                            try {
////
////                                mCameraView.wait();
////
////
////                            } catch (InterruptedException e) {
////
////                            }
////                        }
//
//
//                    if (MainActivity.isDebugViewEnabled())
//                        mTimerCallbacks.onTimerStarted(TaskTimer.DRAW_VIEW_ID);
//
//                    draw(canvas);
//
//                    if (MainActivity.isDebugViewEnabled())
//                        mTimerCallbacks.onTimerStopped(TaskTimer.DRAW_VIEW_ID);
//
////                        }
//
//                    } finally {
//                        // do this in a finally so that if an exception is thrown
//                        // during the above, we don't leave the Surface in an
//                        // inconsistent state
//                        if (canvas != null) {
//                            mSurfaceHolder.unlockCanvasAndPost(canvas);
//                        }
//                    }
//                }
//            }
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

//            Clear the screen from previous drawings:
            canvas.drawColor(Color.BLUE, PorterDuff.Mode.CLEAR);

//            Debugging rectangle:
            mRectPaint.setARGB(200,0,100,0);
//            canvas.drawRect(mCanvasWidth - 200, mCanvasHeight - 200, mCanvasWidth + 200, mCanvasHeight + 200, mRectPaint);

//            canvas.drawRect(0, 0, mCanvasWidth, mCanvasHeight, mRectPaint);
//            mRectPaint.setARGB(255,100,100,0);
            canvas.drawRect(0, 0, 1000, 1000, mRectPaint);


//            if (mFocusPatches != null) {
//
//                Patch patch;
//
//                for (int i = 0; i < mFocusPatches.length; i++) {
//
//                    patch = mFocusPatches[i];
//                    String fValue = String.format("%.2f", patch.getFM());
//
//                    if (patch.getIsForeGround()) {
//                        if (patch.getIsSharp())
//                            mTextPaint.setColor(GOOD_TEXT_COLOR);
//                        else
//                            mTextPaint.setColor(BAD_TEXT_COLOR);
//
//                        canvas.drawText(fValue, patch.getDrawViewPX(), patch.getDrawViewPY() + 50, mTextPaint);
//                    }
//
//                }
//
//            }
//
//            if (mPolyRects != null) {
//
//                for (DkPolyRect polyRect : mPolyRects) {
//
//                    // TODO: find out why this is partially NULL:
//                    if (polyRect == null)
//                        continue;
//
//                    mSegmentationPath.reset();
//
//                    ArrayList<PointF> screenPoints = polyRect.getScreenPoints();
//
//                    boolean isStartSet = false;
//
//                    for (PointF point : screenPoints) {
//
//                        if (!isStartSet) {
//                            mSegmentationPath.moveTo(point.x, point.y);
//                            isStartSet = true;
//                        }
//                        else
//                            mSegmentationPath.lineTo(point.x, point.y);
//
//                    }
//
//                    mSegmentationPath.close();
//
//                    canvas.drawPath(mSegmentationPath, mSegmentationPaint);
//
//                }
//
//            }
//
        }

    }


}
