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

import java.util.ArrayList;

/**
 * Class used to measure the execution time of time intensive tasks. Each task has an ID and a an
 * assigned execution time.
 */
public class TaskTimer {


    public static final int FOCUS_MEASURE_ID = 0;
    public static final int PAGE_SEGMENTATION_ID = 1;
    public static final int DRAW_VIEW_ID = 2;
    public static final int CAMERA_FRAME_ID = 3;
    public static final int MAT_CONVERSION_ID = 4;
    public static final int ILLUMINATION_ID = 5;

    private ArrayList<Task> mTasks;

    /**
     * Creates a list of Tasks.
     */
    public TaskTimer() {

        mTasks = new ArrayList<>();
        mTasks.add(new Task(FOCUS_MEASURE_ID));
        mTasks.add(new Task(PAGE_SEGMENTATION_ID));
        mTasks.add(new Task(DRAW_VIEW_ID));
        mTasks.add(new Task(CAMERA_FRAME_ID));
        mTasks.add(new Task(MAT_CONVERSION_ID));

    }

    /**
     * Starts a timer.
     * @param taskId ID of the task
     */
    public void startTaskTimer(int taskId) {

        Task task = getTask(taskId);
        if (task != null)
            task.startTimer();

    }

    /**
     * Stops the timer and returns the execution time.
     * @param taskId ID of the task
     * @return time in milliseconds
     */
    public long getTaskTime(int taskId) {

        Task task = getTask(taskId);
        if (task != null)
            return task.stopTimer();
        else
            return -1;

    }

    /**
     * Finds a task by ID in the task list.
     * @param taskId ID of the task
     * @return Task
     */
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

    /**
     * Task class.
     */
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
