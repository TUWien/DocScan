package at.ac.tuwien.caa.docscan

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import it.xabaras.android.espresso.recyclerviewchildactions.RecyclerViewChildActions.Companion.actionOnChild
import org.hamcrest.CoreMatchers.*


@RunWith(AndroidJUnit4::class)
class DocumentProcessTest {

    @Test
    fun exportToPdf() {

//        TODO: grant permissions

        ActivityScenario.launch(CameraActivity::class.java)

        // Take n images
        takeImages(5)
//
//        Click on the documents button and open the DocumentViewerActivity:
        onView(withId(R.id.document_fab)).perform(click())
        BottomNavigationTest.assertFirstScreen()
        createPdf(0)
        Thread.sleep(2000)
        openPdf(0)

//        TODO: keep old functions...

//        renameFirstDocument()

//        createDocument("permanent record", 3)
////        Click on the documents button and open the DocumentViewerActivity:
//        onView(withId(R.id.document_fab)).perform(click())
//        createDocument("designing interfaces", 3)
//
//        onView(withId(R.id.document_fab)).perform(click())
//        deleteDocumentFromDocumentFragment(0)

//        onView(withId(R.id.document_fab)).perform(click())
//        createPdf(0)

//        Thread.sleep(5000)
//        onView(withId(R.id.viewer_pdfs)).perform((click()))
//        Thread.sleep(5000)



//        onView(withId(R.id.viewer_documents))
//                .check(matches(isDisplayed()))



////        val newText = "hitchhikers guide to the galaxy"
////        changeDocumentTitleFromImagesFragment(newText)
////
//        val muchNewerText = "thanks for all the fish"
//        changeDocumentTitleFromDocumentFragment(muchNewerText)

    }



    /**
     * Deletes the document at position pos from the DocumentsFragment
     */
    private fun deleteDocumentFromDocumentFragment(pos: Int) {

        BottomNavigationTest.openFirstScreen()
        BottomNavigationTest.assertFirstScreen()

//        Click the more button that is next to the title with value oldText
//        Unfortunately, there is no one liner for this so we use this package:
//        it.xabaras.android.espresso.recyclerviewchildactions
        onView(withId(R.id.documents_list)).
                perform(
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(pos,
                                actionOnChild(click(), R.id.document_more_button)))
//        Click on the delete button in the ActionSheet:
        onView(allOf(withText(R.string.action_document_delete_document), isDisplayed()))
                .perform(click())
//        Click on the delete button in the dialog:
        onView(withText(R.string.sync_confirm_delete_button_text)).perform(click())


    }

    /**
     * Opens a pdf from the pdf list at position pos from the DocumentsFragment
     */
    private fun openPdf(pos: Int) {

        BottomNavigationTest.openThirdScreen()
//        try {
            onView(withText(R.string.pdf_fragment_persisted_permission_title)).inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(R.string.dialog_ok_text)).perform(click())
            Thread.sleep(2000)

        val intent = genIntentWithPersistedReadPermissionForFile()
        intending(
            allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(`is`(Intent.EXTRA_INTENT),
                    allOf(
                        hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
                        hasType("*/*"),
                        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE)))
                    )
                )
            )
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))


        onView(withId(R.id.pdf_list)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, click()))
    }

    fun genIntentWithPersistedReadPermissionForFile(): Intent {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/home%3ADocScan")
        return Intent().apply {
            val context: Context = ApplicationProvider.getApplicationContext()
//            val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
            context.grantUriPermission(
                BuildConfig.APPLICATION_ID,
                uri,
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            data = uri
            flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
//        }
//        catch (e: Exception) {
//
//        }
//

//        BottomNavigationTest.assertThirdScreen()

//        Click the more button that is next to the title with value oldText
//        Unfortunately, there is no one liner for this so we use this package:
//        it.xabaras.android.espresso.recyclerviewchildactions
//        onView(withId(R.id.pdf_list)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, click()))
//        perform(
//            actionOnItemAtPosition<RecyclerView.ViewHolder>(pos,
//                actionOnChild(click(), R.id.document_more_button)))
////        Click on the delete button in the ActionSheet:
//        onView(allOf(withText(R.string.action_document_pdf_title), isDisplayed()))
//            .perform(click())
////        Click on the yes button in the dialog:
//        onView(withText(R.string.dialog_yes_text)).perform(click())
////        Do not do OCR:
//        onView(withText(R.string.dialog_no_text)).perform(click())

    }

    /**
     * Creates a pdf from the document at position pos from the DocumentsFragment
     */
    private fun createPdf(pos: Int) {

        BottomNavigationTest.openFirstScreen()
        BottomNavigationTest.assertFirstScreen()

//        Click the more button that is next to the title with value oldText
//        Unfortunately, there is no one liner for this so we use this package:
//        it.xabaras.android.espresso.recyclerviewchildactions
        onView(withId(R.id.documents_list)).
        perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(pos,
                actionOnChild(click(), R.id.document_more_button)))
//        Click on the delete button in the ActionSheet:
        onView(allOf(withText(R.string.action_document_pdf_title), isDisplayed()))
            .perform(click())
//        Click on the yes button in the dialog:
        onView(withText(R.string.dialog_yes_text)).perform(click())
//        Do not do OCR:
        onView(withText(R.string.dialog_no_text)).perform(click())

    }


    /**
     * Creates a new document and takes n pictures:
     */
    private fun createDocument(title: String, numPics: Int) {

        BottomNavigationTest.openFirstScreen()
        BottomNavigationTest.assertFirstScreen()

//        Click the add new fab:
        onView(withId(R.id.viewer_add_fab))
                .perform(click())

//        The EditDocumentActivity is now launched
        onView(withId(R.id.create_series_name_edittext))
                .perform(replaceText(title))
        onView(withId(R.id.create_series_done_button))
                .perform(click())

        takeImages(numPics)

    }

    private fun takeImages(n: Int) {
        for (i in 0..n) {
            onView(withId(R.id.photo_button)).perform(click())
            Thread.sleep(200)
        }
    }


//    private fun renameFirstDocument() {
//        val muchNewerText = "thanks for all the fish"
//        changeDocumentTitleFromDocumentFragment(muchNewerText)
//
//        val newText = "hitchhikers guide to the galaxy"
//        changeDocumentTitleFromImagesFragment(newText)
//    }

    private fun changeDocumentTitleFromDocumentFragment(newText: String) {

//        Click the more button that is next to the title with value oldText
//        Unfortunately, there is no one liner for this so we use this package:
//        it.xabaras.android.espresso.recyclerviewchildactions
        onView(withId(R.id.documents_list)).
                perform(
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                                actionOnChild(click(), R.id.document_more_button)))
//
        //        Click on the change title button:
        onView(allOf(withText(R.string.action_document_edit_document), isDisplayed()))
                .perform(click())
//
//        The EditDocumentActivity is now launched
        onView(withId(R.id.create_series_name_edittext))
                .perform(replaceText(newText))
        onView(withId(R.id.create_series_done_button))
                .perform(click())

        BottomNavigationTest.assertFirstScreen()

//        Check if there is now an item with the much newer text:
        onView(withId(R.id.documents_list))
                .check(matches(hasDescendant(withText(newText))))

    }

//    private fun changeDocumentTitleFromImagesFragment(newText: String) {
//        //        Open the first document:
//        onView(withId(R.id.documents_list)).perform(
//                actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
//
//        BottomNavigationTest.assertSecondScreen()
//        //        Click on the FAB:
//        onView(withId(R.id.viewer_edit_fab))
//                .perform(click())
//
//        //        Click on the change title button:
//        onView(allOf(withText(R.string.action_document_edit_document), isDisplayed()))
//                .perform(click())
//
//        //        The EditDocumentActivity is now launched
//        onView(withId(R.id.create_series_name_edittext))
//                .perform(replaceText(newText))
//        onView(withId(R.id.create_series_done_button))
//                .perform(click())
//
////        Check the title of the toolbar:
//        onView(allOf(instanceOf(TextView::class.java), withParent(withId(R.id.main_toolbar))))
//                .check(matches(withText(newText)))
//    }
