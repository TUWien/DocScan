package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.util.ArrayList;

/**
 * Created by fabian on 4/4/2018.
 */

public class PageImageView extends SubsamplingScaleImageView {

    private SingleClickListener mClickCallBack;
    private ArrayList<PointF> mPoints;

    private static final String CLASS_NAME = "TouchImageView";

    public PageImageView(Context context, AttributeSet attr) {
        super(context, attr);

        mClickCallBack = (SingleClickListener) context;

    }

    public void setPoints(ArrayList<PointF> points) {

        mPoints = points;

        invalidate();

    }

    public void setClickCallBack(SingleClickListener listener) {

        mClickCallBack = listener;

    }

    public PageImageView(Context context) {
        super(context);

        mClickCallBack = (SingleClickListener) context;
    }

    @Override
    public boolean performClick () {

        if (mClickCallBack != null) {
            mClickCallBack.onSingleClick();
        }

        return mClickCallBack != null;

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }

        drawQuad(canvas, mPoints);


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

            PointF transPoint = new PointF();
            sourceToViewCoord(point, transPoint);

            if (!isStartSet) {
                mQuadPath.moveTo(transPoint.x, transPoint.y);
                isStartSet = true;
            } else
                mQuadPath.lineTo(transPoint.x, transPoint.y);

            // draw the circle around the corner:
//            canvas.drawCircle(point.x, point.y, CORNER_CIRCLE_RADIUS, mCirclePaint);

        }

        mQuadPath.close();
        canvas.drawPath(mQuadPath, mQuadPaint);

    }

    public interface SingleClickListener {

        void onSingleClick();

    }

}
