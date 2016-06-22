package at.ac.tuwien.caa.docscan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Based on Lunar Lander example:
 * https://github.com/Miserlou/Android-SDK-Samples/blob/master/LunarLander
 */

public class DrawView extends SurfaceView implements SurfaceHolder.Callback {

    class DrawerThread extends Thread {

        private SurfaceHolder mSurfaceHolder;
        private boolean mIsRunning;
        private int mCanvasWidth, mCanvasHeight;

        private Paint mRectPaint;

        public DrawerThread(SurfaceHolder surfaceHolder) {


            mSurfaceHolder = surfaceHolder;

//            Initialize drawing stuff:
            mRectPaint = new Paint();


        }

        @Override
        public void run() {
            while (mIsRunning) {
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        draw(canvas);
                    }
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


        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

            }
        }

        public void setRunning(boolean b) {

            mIsRunning = b;

        }

        private void draw(Canvas canvas) {

            if (canvas == null) {
                return;
            }

            canvas.drawRect(0, 0, 100, 100, mRectPaint);

//            canvas.save();
        }

    }


//    private SurfaceHolder mSurfaceHolder;
    private DrawerThread mDrawerThread;

    public DrawView(Context context, AttributeSet attrs) {

        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);



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



//    @Override
//    public void onWindowFocusChanged(boolean hasWindowFocus) {
//        if (!hasWindowFocus) mDrawerThread.setRunning(false);
//    }


}
