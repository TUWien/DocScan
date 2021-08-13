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

import android.util.Log;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.CAMERA_FRAME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FOCUS_MEASURE;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.MOVEMENT_CHECK;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.NEW_DOC;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.SHOT_TIME;

/**
 * Class used to measure the execution time of time intensive tasks. Each task has an ID and a an
 * assigned execution time.
 */
public class TaskTimer {

    public enum TaskType {
        FOCUS_MEASURE, PAGE_SEGMENTATION, DRAW_VIEW, CAMERA_FRAME, SHOT_TIME, FLIP_SHOT_TIME,
        MOVEMENT_CHECK, NEW_DOC
    }

    private ArrayList<Task> mTasks;

    /**
     * Creates a list of Tasks.
     */
    public TaskTimer() {

        mTasks = new ArrayList<>();
        mTasks.add(new Task(FOCUS_MEASURE));
        mTasks.add(new Task(PAGE_SEGMENTATION));
        mTasks.add(new Task(CAMERA_FRAME));
        mTasks.add(new Task(SHOT_TIME));
        mTasks.add(new Task(FLIP_SHOT_TIME));
        mTasks.add(new Task(MOVEMENT_CHECK));
        mTasks.add(new Task(NEW_DOC));

    }


    /**
     * Starts a timer.
     *
     * @param type
     */
    public void startTaskTimer(TaskType type) {

        Task task = getTask(type);
        if (task != null)
            task.startTimer();

    }

    /**
     * Stops the timer and returns the execution time.
     *
     * @param type
     * @return time in milliseconds
     */
    public long getTaskTime(TaskType type) {

        Task task = getTask(type);
        if (task != null)
            if (task.isTimerStarted())
                return task.stopTimer();

        return -1;

    }


    /**
     * Finds a task by ID in the task list.
     *
     * @param type
     * @return Task
     */
    private Task getTask(TaskType type) {

        for (Task task : mTasks) {
            if (task.getType() == type)
                return task;
        }

        return null;

    }

    public interface TimerCallbacks {

        void onTimerStarted(TaskType type);

        void onTimerStopped(TaskType type);

    }

    /**
     * Task class.
     */
    private class Task {

        private long mStartTime;
        private long mTimeSum;
        private int mTaskCnt;
        private TaskType mType;


        private Task(TaskType type) {
            mType = type;
            mTaskCnt = 0;
            mTimeSum = 0;
            mStartTime = -1;
        }

        public boolean isTimerStarted() {
            return mStartTime != -1;
        }

        private TaskType getType() {
            return mType;
        }

        private void startTimer() {
            mStartTime = System.currentTimeMillis();
        }

        private long stopTimer() {

            long timePassed = System.currentTimeMillis() - mStartTime;
            updateTimeSum(timePassed);

            return timePassed;

        }

        private void updateTimeSum(long timePassed) {

            mTaskCnt++;
            mTimeSum += timePassed;


            double averageTime = mTimeSum / (double) mTaskCnt;

            String type = "undefined";
            switch (mType) {
                case PAGE_SEGMENTATION:
                    type = "page segmentation";
                    break;
                case FOCUS_MEASURE:
                    type = "focus measurement";
                    break;
            }

            Log.d(TAG, "task: " + type + " time: " + averageTime);
        }

    }
}
