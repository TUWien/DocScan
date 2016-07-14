package at.ac.tuwien.caa.docscan;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by fabian on 12.07.2016.
 */
public class DebugViewFragment extends Fragment {


    private TextView mFocusMeasureTextView, mPageSegmentationTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View debugView = (View) inflater.inflate(R.layout.debug_view, container, false);

        mFocusMeasureTextView = (TextView) debugView.findViewById(R.id.focus_measure_time_view);
        mPageSegmentationTextView = (TextView) debugView.findViewById(R.id.page_segmentation_time_view);

        return debugView;

    }

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


        }

        if (textView != null)
            setTextViewText(textView, text);

    }

    private void setTextViewText(TextView textView, String text) {

        textView.setText(text);

    }

    public void setFocusMeasureTime(long time) {

        mFocusMeasureTextView.setText(String.valueOf(time));

    }

}
