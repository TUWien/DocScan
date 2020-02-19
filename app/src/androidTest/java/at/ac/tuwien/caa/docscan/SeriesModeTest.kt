package at.ac.tuwien.caa.docscan

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.ac.tuwien.caa.docscan.camera.cv.thread.preview.IPManager
import at.ac.tuwien.caa.docscan.ui.CameraActivity
import org.hamcrest.CoreMatchers.*

//fun getMat(frameWidth: Int, frameHeight: Int, type: Int) : Mat {
//
//    val result = Mat(frameHeight, frameWidth, type)
//
//    val page = Mat(200, 200, type)
//    page.setTo(Scalar(255.0, 0.0, 0.0))
//    Imgproc.putText(page, "Don't judge a book by it's cover", Point(0.0,
//            (page.cols() / 2).toDouble()), 3, 1.0,
//            Scalar(0.0, 0.0, 0.0))
//
//    result.setTo(Scalar(0.0, 255.0, 0.0))
//    //        result.setTo(new Scalar(0, 255, 0));
//    val subMat = result.submat(Rect(20, 20, page.cols(), page.rows()))
////        Mat submat= result.submat(new org.opencv.core.Rect(20,20, page.cols(), page.rows()));
//    page.copyTo(subMat)
//
//    return page
//}



@RunWith(AndroidJUnit4::class)
class SeriesModeTest {

    companion object{

        const val MY_CONSTANT = "Constants"

    }

    @Test
    fun succesfullPictures() {

        // GIVEN
//        val scenario = ActivityScenario.launch(CameraActivity::class.java)
        ActivityScenario.launch(CameraActivity::class.java)

        IPManager.getInstance().setIsTesting(true)

        // WHEN
//        onView(withId(R.id.shoot_mode_spinner)).perform(click())

//        Select the series mode:
        onData(allOf(`is`(instanceOf(String::class.java)),
                `is`("Series"))).perform(click())

        var lastMillis = System.currentTimeMillis()
        var minDiff = 0
        var state = IPManager.TestState.TEST_STATE_NO_PAGE

        while (true) {
            if (System.currentTimeMillis() - lastMillis > minDiff) {
                when (state) {
                    IPManager.TestState.TEST_STATE_NO_PAGE -> {
//                        Log.d("SeriesModeTest", "changing to TEST_STATE_PAGE")
                        state = IPManager.TestState.TEST_STATE_PAGE_A
                        minDiff = 5000
                    }
                    IPManager.TestState.TEST_STATE_PAGE_A -> {
//                        Log.d("SeriesModeTest", "changing to TEST_STATE_NO_PAGE")
                        state = IPManager.TestState.TEST_STATE_PAGE_B
                        minDiff = 5000
                    }
                    IPManager.TestState.TEST_STATE_PAGE_B -> {
//                        Log.d("SeriesModeTest", "changing to TEST_STATE_NO_PAGE")
                        state = IPManager.TestState.TEST_STATE_NO_PAGE
                        minDiff = 5000
                    }
                }
                Log.d("SeriesModeTest", "changed state to: " + state)
                IPManager.getInstance().setTestState(state)

                lastMillis = System.currentTimeMillis()
            }
        }

    }
}