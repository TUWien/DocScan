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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment used to show the execution time of time intensive tasks.
 */
public class DebugViewFragment extends Fragment {


    private TextView mFocusMeasureTextView, mPageSegmentationTextView, mDrawViewTextView,
            mCameraFrameTextView, mMatConversionTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View debugView = inflater.inflate(R.layout.debug_view, container, false);

        mFocusMeasureTextView = (TextView) debugView.findViewById(R.id.focus_measure_time_view);
        mPageSegmentationTextView = (TextView) debugView.findViewById(R.id.page_segmentation_time_view);
        mPageSegmentationTextView = (TextView) debugView.findViewById(R.id.page_segmentation_time_view);
        mDrawViewTextView = (TextView) debugView.findViewById(R.id.draw_view_time_view);
        mCameraFrameTextView = (TextView) debugView.findViewById(R.id.camera_frame_time_view);
        mMatConversionTextView = (TextView) debugView.findViewById(R.id.mat_conversion_time_view);

        return debugView;

    }

    /**
     * Updates a TextView showing the execution time of a task.
     * @param senderId ID of the Task. See TaskTimer for possible values.
     * @param time time in milliseconds
     */
    public void setTimeText(int senderId, long time) {

        String text = String.valueOf(time);
        TextView textView = null;

        switch (senderId) {

            case TaskTimer.FOCUS_MEASURE_ID:
                textView = mFocusMeasureTextView;
                break;
            case TaskTimer.PAGE_SEGMENTATION_ID:
                textView = mPageSegmentationTextView;
                break;
            case TaskTimer.DRAW_VIEW_ID:
                textView = mDrawViewTextView;
                break;
            case TaskTimer.CAMERA_FRAME_ID:
                textView = mCameraFrameTextView;
                break;
            case TaskTimer.MAT_CONVERSION_ID:
                textView = mMatConversionTextView;
                break;

        }

        if (textView != null)
            setTextViewText(textView, text);

    }

    private void setTextViewText(TextView textView, String text) {

        textView.setText(text);

    }

}
