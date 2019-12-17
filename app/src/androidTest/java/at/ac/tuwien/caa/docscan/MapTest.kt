package at.ac.tuwien.caa.docscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessor
import at.ac.tuwien.caa.docscan.ui.CameraActivity
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class MapTest {

    @Before
    fun register() {

        IdlingPolicies.setIdlingResourceTimeout(1, TimeUnit.HOURS)
        IdlingPolicies.setMasterPolicyTimeout(1, TimeUnit.HOURS)
//        IdlingRegistry.getInstance().register(ImageProcessor.getInstance().idling)
    }

    @Test
    fun shootAndMap() {

//        shoot(100)
        map()




    }

    @After
    fun unregister() {
//        IdlingRegistry.getInstance().unregister(ImageProcessor.getInstance().idling)
    }


    fun map() {

//        ImageProcessor.setEspressoIdling(true)
        registerIdlingResources();

//        Launch the GalleryActivity via intent:
        val intent = Intent(ApplicationProvider.getApplicationContext<Context>(), GalleryActivity::class.java)
        val bundle = Bundle()
        bundle.putString("KEY_DOCUMENT_FILE_NAME", "Untitled document")
        intent.putExtras(bundle)
        ActivityScenario.launch<GalleryActivity>(intent)

//        Select the first item:
        onView(withText("#: 1")).perform(click())

//        Select all items
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText("Select all items")).perform(click())

//        Crop the images
        onView(withId(R.id.gallery_menu_crop)).perform(click())


//        Confirm the replacing of the images:
        onView(withText(R.string.dialog_ok_text)).perform(click())


//        Thread.sleep(200)

//        ActivityScenario.launch(UploadActivity::class.java)
//
//        onView(allOf(withId(R.id.layout_listview_row_thumbnail), hasSibling(withText("Untitled document"))))
//                .perform(click())


//        //        Click the more button that is next to the title with value oldText
////        Unfortunately, there is no one liner for this so we use this package:
////        it.xabaras.android.espresso.recyclerviewchildactions
//        onView(withId(R.id.upload_list_view)).
//                perform(
//                        actionOnItemAtPosition<ViewHol>(pos,
//                                actionOnChild(click(), R.id.document_more_button)))

    }
//    @Test
    fun shoot(numImgs: Int) {

        ActivityScenario.launch(CameraActivity::class.java)

        for (i in 0..numImgs) {
            onView(withId(R.id.photo_button)).perform(click())
            Thread.sleep(200)
        }


//        onView(withId(R.id.username_edittext)).perform(typeText("test_user"))
//        closeSoftKeyboard()
//        onView(withId(R.id.password_edittext))
//                .perform(typeText("none"))
//        closeSoftKeyboard()
//        onView(withId(R.id.login_button)).perform(click())

        // THEN
//        assertThat(getIntents().first())
//                .hasComponentClass(HomeActivity::class.java)
    }
}