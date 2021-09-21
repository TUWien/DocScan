/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   20. October 2016
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
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CameraPaintLayout extends FrameLayout {

    private int mFrameWidth, mFrameHeight;
    private CameraPreview.CameraPreviewCallback mCameraPreviewCallback;

    public CameraPaintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraPreviewCallback = (CameraPreview.CameraPreviewCallback) context;
    }

    public void setFrameDimensions(int frameWidth, int frameHeight) {

        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mFrameHeight == 0 || mFrameWidth == 0) {
//            setChildMeasuredDimension(width, height);
            setMeasuredDimension(width, height);
        } else {
            // Note that mFrameWidth > mFrameHeight - regardless of the orientation!

            // Portrait mode:
            if (width < height) {
                double resizeFac = (double) width / mFrameHeight;
                int scaledHeight = (int) Math.round(mFrameWidth * resizeFac);
                if (scaledHeight > height)
                    scaledHeight = height;
                setMeasuredDimension(width, scaledHeight);
            }

            // Landscape mode:
            else {
                double resizeFac = (double) height / mFrameHeight;
                int scaledWidth = (int) Math.round(mFrameWidth * resizeFac);
                if (scaledWidth > width)
                    scaledWidth = width;
                setMeasuredDimension(scaledWidth, height);
            }

        }

//        final int count = getChildCount();
//        for (int i = 0; i < count; i++) {
//            final View v = getChildAt(i);
//
//            v.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
//                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
//                    getMeasuredHeight(), MeasureSpec.EXACTLY));
//        }

        mCameraPreviewCallback.onMeasuredDimensionChange(getMeasuredWidth(), getMeasuredHeight());

    }


}
