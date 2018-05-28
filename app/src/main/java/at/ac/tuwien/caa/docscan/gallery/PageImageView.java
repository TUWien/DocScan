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

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 4/4/2018.
 */

public class PageImageView extends SubsamplingScaleImageView {

    private SingleClickListener mClickCallBack;
    private ArrayList<PointF> mPoints;

    private final int PAGE_RECT_COLOR = getResources().getColor(R.color.hud_page_rect_color);
    private static final String CLASS_NAME = "PageImageView";
    private Paint mQuadPaint;
    private Path mQuadPath;

    public PageImageView(Context context, AttributeSet attr) {
        super(context, attr);

        mClickCallBack = (SingleClickListener) context;
        initPaint();

    }

    private void initPaint() {
        mQuadPaint = new Paint();
        mQuadPath = new Path();

        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_stroke_width));
        mQuadPaint.setColor(PAGE_RECT_COLOR);
//        mQuadPaint.setStrokeWidth(4);
        mQuadPaint.setAntiAlias(true);
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

//        TODO: optimize the drawing speed! consider this link: https://developer.android.com/topic/performance/vitals/render#common-jank

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

        }

        mQuadPath.close();
        canvas.drawPath(mQuadPath, mQuadPaint);

    }

    public interface SingleClickListener {

        void onSingleClick();

    }

}
