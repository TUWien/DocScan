package at.ac.tuwien.caa.docscan

import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.ac.tuwien.caa.docscan.ui.TranskribusLoginActivity

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Test
    fun successfulLogin() {
        // GIVEN
        val scenario = ActivityScenario.launch(TranskribusLoginActivity::class.java)

        // WHEN
        onView(withId(R.id.username_edittext)).perform(typeText("test_user"))
        closeSoftKeyboard()
        onView(withId(R.id.password_edittext))
                .perform(typeText("none"))
        closeSoftKeyboard()
        onView(withId(R.id.login_button)).perform(click())

        // THEN
//        assertThat(getIntents().first())
//                .hasComponentClass(HomeActivity::class.java)
    }

    val isRunningTest : Boolean by lazy {
        try {
            Class.forName("android.support.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}