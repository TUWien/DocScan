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

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.R;

/**
 * Fragment used to show the execution time of time intensive tasks.
 */
public class DebugViewFragment extends Fragment {


    private TextView mFocusMeasureTextView, mPageSegmentationTextView, mCameraFrameTextView,
            mShotTextView, mFlipShotTextView, mMovementCheckTextView, mNewDocTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View debugView = inflater.inflate(R.layout.debug_view, container, false);

        mFocusMeasureTextView = (TextView) debugView.findViewById(R.id.focus_measure_time_view);
        mPageSegmentationTextView = (TextView) debugView.findViewById(R.id.page_segmentation_time_view);
//        mCameraFrameTextView = (TextView) debugView.findViewById(R.id.camera_frame_time_view);
//        mShotTextView = (TextView) debugView.findViewById(R.id.inter_shot_time_view);
//        mFlipShotTextView = (TextView) debugView.findViewById(R.id.flip_shot_time_view);
//        mMovementCheckTextView = (TextView) debugView.findViewById(R.id.movement_check_time_view);
//        mNewDocTextView = (TextView) debugView.findViewById(R.id.new_doc_time_view);

        return debugView;

    }


    /**
     * Updates a TextView showing the execution time of a task.
     * @param type
     * @param time time in milliseconds
     */
    public void setTimeText(TaskTimer.TaskType type, long time) {


        String text = "-";
        if (type == TaskTimer.TaskType.CAMERA_FRAME) {
            //        Compute the FPS:
            if (time != 0)
                time = 1000 / time;
        }

        if (time != 0)
            text = String.valueOf(time);

        TextView textView = null;

        switch (type) {

            case FOCUS_MEASURE:
                textView = mFocusMeasureTextView;
                break;
            case PAGE_SEGMENTATION:
                textView = mPageSegmentationTextView;
                break;
            case CAMERA_FRAME:
                textView = mCameraFrameTextView;
                break;
            case SHOT_TIME:
                textView = mShotTextView;
                break;
            case FLIP_SHOT_TIME:
                textView = mFlipShotTextView;
                break;
            case MOVEMENT_CHECK:
                textView = mMovementCheckTextView;
                Log.d(this.getTag(), "time: " + time);
                break;
            case NEW_DOC:
                textView = mNewDocTextView;
                Log.d(this.getTag(), "new doc time: " + time);
                break;
        }

        if (textView != null)
            setTextViewText(textView, text);

    }


    private void setTextViewText(TextView textView, String text) {

        textView.setText(text);

    }

}
