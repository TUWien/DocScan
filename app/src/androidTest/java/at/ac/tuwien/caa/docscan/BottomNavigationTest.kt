/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.ac.tuwien.caa.docscan

import android.widget.Toast
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import junit.framework.Assert.fail
import me.drakeet.support.toast.ToastCompat
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test


class BottomNavigationTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule var activityTestRule = ActivityTestRule(DocumentViewerActivity::class.java)

    companion object {
        fun assertFirstScreen() {
            onView(withId(R.id.viewer_documents))
                    .check(matches(isDisplayed()))
        }
        fun assertThirdScreen() {
            onView(withId(R.id.viewer_pdfs))
                    .check(matches(isDisplayed()))
        }
        fun assertSecondScreen() {
            onView(withId(R.id.viewer_images))
                    .check(matches(isDisplayed()))
        }

        fun openFirstScreen() {
            onView(withId(R.id.viewer_documents))
                    .perform(click())
        }

        fun openSecondScreen() {
            onView(withId(R.id.viewer_images))
                    .perform(click())
        }

        fun openThirdScreen() {
            onView(withId(R.id.viewer_pdfs))
                    .perform(click())
        }
    }

    @Test
    fun bottomNavView_clickOnAllItems() {
        // All screens open at their first destinations
        assertFirstScreen()

        openThirdScreen()

        assertThirdScreen()

        openSecondScreen()

        assertSecondScreen()

        openFirstScreen()

        assertFirstScreen()
    }

    @Test
    fun bottomNavView_testBackPress() {

        // All screens open at their first destinations
        assertFirstScreen()

        openThirdScreen()

        Espresso.pressBack()

        assertFirstScreen()

//        assertThirdScreen()
//
//        openSecondScreen()
//
//        assertSecondScreen()
//
//        openFirstScreen()
//
//        assertFirstScreen()
    }

//    @Test
//    fun bottomNavView_backGoesToFirstItem() {
//        // From the 2nd or 3rd screens, back takes you to the 1st.
//        openThirdScreen()
//
//        Espresso.pressBack()
//
//        assertFirstScreen()
//    }
//
//    @Test(expected = NoActivityResumedException::class)
//    fun bottomNavView_backfromFirstItemExits() {
//        // From the first screen, back finishes the activity
//        assertFirstScreen()
//
//        Espresso.pressBack() // This should throw NoActivityResumedException
//
//        fail() // If it doesn't throw
//    }

//    @Test
//    fun bottomNavView_backstackMaintained() {
//        // The back stack of any screen is maintained when returning to it
//        openThirdScreen()
//
//        onView(withContentDescription(R.string.sign_up))
//                .perform(click())
//
//        assertDeeperThirdScreen()
//
//        openFirstScreen()
//
//        // Return to 3rd
//        openThirdScreen()
//
//        // Assert it maintained the back stack
//        assertDeeperThirdScreen()
//    }
//
//    @Test
//    fun bottomNavView_registerBackRegister() {
//        openThirdScreen()
//
//        pressBack() // This is handled in a especial way in code.
//
//        openThirdScreen()
//
//        onView(withContentDescription(R.string.sign_up))
//                .perform(click())
//
//        // Assert it maintained the back stack
//        assertDeeperThirdScreen()
//    }
//
//    @Test
//    fun bottomNavView_itemReselected_goesBackToStart() {
//        openThirdScreen()
//
//        assertThirdScreen()
//
//        onView(withContentDescription(R.string.sign_up))
//                .perform(click())
//
//        assertDeeperThirdScreen()
//
//        // Reselect the current item
//        openThirdScreen()
//
//        // Verify that it popped the back stack until the start destination.
//        assertThirdScreen()
//    }





//    private fun assertDeeperThirdScreen() {
//        onView(withText(R.string.done))
//                .check(matches(isDisplayed()))
//    }

//    private fun openFirstScreen() {
////        onView(allOf(withText(R.string.document_navigation_documents), isDisplayed()))
////                .perform(click())
//        onView(withId(R.id.viewer_documents))
//                .perform(click())
//
//    }
//
//    fun openSecondScreen() {
//        onView(withId(R.id.viewer_images))
//                .perform(click())
//        onView(allOf(withText(R.string.document_navigation_images), isDisplayed()))
//                .perform(click())
//    }
//
//
//    private fun openThirdScreen() {
//        onView(withId(R.id.viewer_pdfs))
//                .perform(click())
////        onView(allOf(withText(R.string.document_navigation_pdfs), isDisplayed()))
////                .perform(click())
//    }


}