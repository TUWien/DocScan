package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 4/4/2018.
 */

public class PageImageView extends SubsamplingScaleImageView {

    private SingleClickListener mClickCallBack;
    private ArrayList<PointF> mPoints;
//    private ArrayList<PointF> mPoints, mOuterPoints;

    private static final String CLASS_NAME = "PageImageView";

    private Paint mQuadPaint;
    private Path mQuadPath;

//    private Paint mQuadPaint, mOuterQuadPaint;
//    private Path mQuadPath, mOuterQuadPath;

    public PageImageView(Context context, AttributeSet attr) {
        super(context, attr);

        mClickCallBack = (SingleClickListener) context;

        initPaint();

    }

    private void initPaint() {

        mQuadPaint = new Paint();
        mQuadPath = new Path();

        mQuadPaint.setStyle(Paint.Style.STROKE);
        mQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_live_preview_stroke_width));
        mQuadPaint.setColor(getResources().getColor(R.color.hud_page_rect_color));
        mQuadPaint.setAntiAlias(true);

//        mOuterQuadPaint = new Paint();
//        mOuterQuadPath = new Path();
//
//        mOuterQuadPaint.setStyle(Paint.Style.STROKE);
//        mOuterQuadPaint.setStrokeWidth(getResources().getDimension(R.dimen.page_live_preview_stroke_width));
//        mOuterQuadPaint.setColor(getResources().getColor(R.color.page_outer_color));
//        mOuterQuadPaint.setAntiAlias(true);

    }

    public void setPoints(ArrayList<PointF> points, boolean isFocused) {

        mPoints = points;
        if (!isFocused && mQuadPaint != null)
            mQuadPaint.setColor(getResources().getColor(R.color.hud_focus_unsharp_rect_color));

        invalidate();

    }

//    public void setPoints(ArrayList<PointF> points, ArrayList<PointF> outerPoints) {
//
//        mPoints = points;
//        mOuterPoints = outerPoints;
//        invalidate();
//
//    }

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

        // This comment was in the example of SubSamplingImageView:
        // Don't draw before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }

        if ((canvas != null) && (mPoints != null) && (mPoints.size() > 0)) {
            drawQuad(canvas, mPoints, mQuadPath, mQuadPaint);
//            drawQuad(canvas, mOuterPoints, mOuterQuadPath, mOuterQuadPaint);
        }

    }

    private void drawQuad(Canvas canvas, ArrayList<PointF> points, Path path, Paint paint) {

//        TODO: optimize the drawing speed! consider this link: https://developer.android.com/topic/performance/vitals/render#common-jank
        path.reset();
        boolean isStartSet = false;

        for (PointF point : points) {

            PointF transPoint = new PointF();
            sourceToViewCoord(point, transPoint);

            if (!isStartSet) {
                path.moveTo(transPoint.x, transPoint.y);
                isStartSet = true;
            } else
                path.lineTo(transPoint.x, transPoint.y);

        }

        path.close();
        canvas.drawPath(path, paint);

    }

    public void resetPoints() {

        mPoints = null;
//        mOuterPoints = null;

    }

    public interface SingleClickListener {

        void onSingleClick();

    }

}
