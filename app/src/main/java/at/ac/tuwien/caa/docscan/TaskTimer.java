package at.ac.tuwien.caa.docscan;

import java.util.ArrayList;

/**
 * Created by fabian on 12.07.2016.
 */
public class TaskTimer {


    public static final int FOCUS_MEASURE_ID = 0;
    public static final int PAGE_SEGMENTATION_ID = 1;

    private ArrayList<Task> mTasks;

    public TaskTimer() {

        mTasks = new ArrayList<Task>();
        mTasks.add(new Task(FOCUS_MEASURE_ID));
        mTasks.add(new Task(PAGE_SEGMENTATION_ID));

    }

    public void startTaskTimer(int taskId) {

        Task task = getTask(taskId);
        if (task != null)
            task.startTimer();

    }

    public long getTaskTime(int taskId) {

        Task task = getTask(taskId);
        if (task != null)
            return task.stopTimer();
        else
            return -1;

    }

    private Task getTask(int taskId) {

        for (Task task : mTasks) {
            if (task.getId() == taskId)
                return task;
        }

        return null;

    }

    public interface TimerCallbacks {

        void onTimerStarted(int senderId);
        void onTimerStopped(int senderId);

    }

    private class Task {

        private long mStartTime;
        private int mId;

        private Task(int id) {

            mId = id;

        }

        private int getId() {

            return mId;

        }

        private void startTimer() {
            mStartTime = System.currentTimeMillis();
        }

        private long stopTimer() {

            return System.currentTimeMillis() - mStartTime;

        }

    }
}
