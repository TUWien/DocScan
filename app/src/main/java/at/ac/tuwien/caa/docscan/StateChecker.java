package at.ac.tuwien.caa.docscan;

import android.content.Context;

/**
 * Created by fabian on 03.10.2016.
 */
public class StateChecker {

    private int mObserveTime;
    private long mStartTime;

    public StateChecker(Context context) {

        mObserveTime = context.getResources().getInteger(R.integer.observation_time);

    }

    public void start() {
        mStartTime = System.currentTimeMillis();
    }

    public void check(int state) {



    }
}
