package at.ac.tuwien.caa.docscan.ui.docviewer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentTransaction
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.camera.ActionSheet
import at.ac.tuwien.caa.docscan.camera.DocumentActionSheet
import at.ac.tuwien.caa.docscan.gallery.GalleryAdapter
import at.ac.tuwien.caa.docscan.logic.Document
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.Helper
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity
import at.ac.tuwien.caa.docscan.ui.syncui.DocumentAdapter
import at.ac.tuwien.caa.docscan.ui.widget.SelectionToolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Partly based on this tutorial:
 * https://pspdfkit.com/blog/2019/using-the-bottom-navigation-view-in-android/
 */

class ViewerActivity : BaseNavigationActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener,
        DocumentAdapter.DocumentAdapterCallback,
        DocumentsFragment.DocumentListener,
        GalleryAdapter.GalleryAdapterCallback,
        ActionSheet.SheetSelection,
        ActionSheet.DocumentSheetSelection,
        ActionSheet.DialogStatus
{
    override fun onDocumentSheetSelected(document: Document, sheetAction: ActionSheet.SheetAction) {

        when (sheetAction.mId) {
            R.id.action_document_continue_item -> {
                DocumentStorage.getInstance(this).title = document.getTitle()
                Helper.startCameraActivity(this)
                DocumentStorage.saveJSON(this)
                finish()
            }
        }

    }

    override fun onSheetSelected(sheetAction: ActionSheet.SheetAction) {

    }

    override fun onShown() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDismiss() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startCamera(view: View) {

        Helper.startCameraActivity(this)
        finish()

    }

    fun newDocument(view: View) {

        startActivity(Intent(applicationContext, CreateDocumentActivity::class.java))

    }

    fun openDocumentOptions(view: View) {

        selectedDocument?.title?.let { showDocumentOptions(selectedDocument!!) }

    }

    override fun onDocumentOptions(document: Document) {

        showDocumentOptions(document)

    }

    private fun showDocumentOptions(document: Document) {

        val sheetActions = ArrayList<ActionSheet.SheetAction>()
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_crop_item,
                getString(R.string.action_document_crop_title),
                R.drawable.ic_crop_black_24dp))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_pdf_item,
                getString(R.string.action_document_pdf_title),
                R.drawable.ic_baseline_picture_as_pdf_24px))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_upload_item,
                getString(R.string.action_document_upload_document),
                R.drawable.ic_cloud_upload_black_24dp))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_delete_item,
                getString(R.string.action_document_delete_document),
                R.drawable.ic_delete_black_24dp))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_edit_item,
                getString(R.string.action_document_edit_document),
                R.drawable.ic_edit_black_24dp))
        sheetActions.add(ActionSheet.SheetAction(R.id.action_document_continue_item,
                getString(R.string.action_document_continue_document),
                R.drawable.ic_add_a_photo_black_24dp))

        val actionSheet = DocumentActionSheet(document, sheetActions, this, this)
        supportFragmentManager.beginTransaction().add(actionSheet, "TAG").commit()

    }

    override fun onSelectionChange(selectionCount: Int) {

    }

    private var selectedDocument: Document? = null

    override fun onDocumenOpened(document: Document) {
        selectedDocument = document
//        Opens the ImagesFragment:
        bottomNavigationView.selectedItemId = R.id.viewer_images
    }

    override fun onSelectionChange() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var bottomNavigationView: BottomNavigationView

    /**
     * Shows the new floating action button and hides any previous fab. If newFABID is -1 all fab's
     * are hidden.
     */
    private fun showFAB(newFABID: Int) {

        var newFAB: FloatingActionButton? = null
        if (newFABID != -1)
            newFAB = findViewById(newFABID)

        if (newFABID == R.id.viewer_edit_fab || newFABID == -1) {
            val addFab: FloatingActionButton = findViewById(R.id.viewer_add_fab)
            if (addFab.visibility == View.VISIBLE)
                addFab.hide()
        }
        if (newFABID == R.id.viewer_add_fab || newFABID == -1) {
            val editFab: FloatingActionButton = findViewById(R.id.viewer_edit_fab)
            if (editFab.visibility == View.VISIBLE)
                editFab.hide()
        }
//        Show the button if it is assigned:
        newFAB?.show()

    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

//        Do nothing in case the item is already selected:
        if (menuItem.itemId == bottomNavigationView.selectedItemId)
            return false

        when(menuItem.itemId) {
            R.id.viewer_documents -> {
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.setCustomAnimations(R.anim.translate_right_to_left_in,
                        R.anim.translate_right_to_left_out)

//                ft.setCustomAnimations(R.anim.translate_left_to_right, R.anim.translate_right_to_left)
                ft.replace(R.id.viewer_fragment_layout, DocumentsFragment(this)).commit()

                showFAB(R.id.viewer_add_fab)

                return true
            }
            R.id.viewer_images -> {

//                If no document is opened, show the images of the active document:
//                val openDocument: Document =
//                        if (selectedDocument == null)
//                            DocumentStorage.getInstance(this).activeDocument
//                        else
//                            selectedDocument as Document
//                The user just clicked images tab and did not open any document:
                if (selectedDocument == null)
                    selectedDocument = DocumentStorage.getInstance(this).activeDocument

                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()

//                The animation depends on the position of the selected item:
                if (bottomNavigationView.selectedItemId == R.id.viewer_documents)
                    ft.setCustomAnimations(R.anim.translate_left_to_right_in,
                            R.anim.translate_left_to_right_out)
                else
                    ft.setCustomAnimations(R.anim.translate_right_to_left_in,
                            R.anim.translate_right_to_left_out)

//                val editFab = findViewById<FloatingActionButton>(R.id.viewer_edit_fab)
//                editFab.show()

////                Add this to the backstack so that the user can get back to the DocumentsFragment
//                ft.addToBackStack(null)
                ft.replace(R.id.viewer_fragment_layout, ImagesFragment(selectedDocument)).commit()

                showFAB(R.id.viewer_edit_fab)
//                supportActionBar?.setTitle("asdf")
                supportActionBar?.setTitle(selectedDocument?.title)

                return true
            }
            R.id.viewer_pdfs -> {
                val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                ft.setCustomAnimations(R.anim.translate_left_to_right_in,
                        R.anim.translate_left_to_right_out)
//                Add this to the backstack so that the user can get back to the DocumentsFragment
                ft.addToBackStack(null)
                ft.replace(R.id.viewer_fragment_layout, PdfFragment()).commit()

                showFAB(-1)

                return true
            }

        }
        return false

    }

    private fun initToolbar() {

        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        val appBarLayout: AppBarLayout = findViewById(R.id.gallery_appbar)

        val selectionToolbar = SelectionToolbar(this, toolbar, appBarLayout)
        //        Enable back navigation in action bar:
        setSupportActionBar(toolbar)
        //        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


//    private fun crossfade() {
//
//        val addFab = findViewById<FloatingActionButton>(R.id.viewer_add_fab)
//
//        addFab.apply {
//            // Set the content view to 0% opacity but visible, so that it is visible
//            // (but fully transparent) during the animation.
//            alpha = 0f
//            visibility = View.VISIBLE
//
//            // Animate the content view to 100% opacity, and clear any animation
//            // listener set on the view.
//            animate()
//                    .alpha(1f)
//                    .setDuration(1000)
//                    .setListener(null)
//        }
//        // Animate the loading view to 0% opacity. After the animation ends,
//        // set its visibility to GONE as an optimization step (it won't
//        // participate in layout passes, etc.)
//        loadingView.animate()
//                .alpha(0f)
//                .setDuration(shortAnimationDuration.toLong())
//                .setListener(object : AnimatorListenerAdapter() {
//                    override fun onAnimationEnd(animation: Animator) {
//                        loadingView.visibility = View.GONE
//                    }
//                })
//    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        bottomNavigationView = findViewById(R.id.viewer_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

//        Open the DocumentsFragment first / as default view:
        supportFragmentManager.beginTransaction().replace(
                R.id.viewer_fragment_layout, DocumentsFragment(this)).commit()

        initToolbar()

    }


//    /**
//     * Selects the specified item in the bottom navigation view.
//     */
//    private fun selectBottomNavigationViewMenuItem(@IdRes menuItemId: Int) {
//        bottomNavigationView.setOnNavigationItemSelectedListener(null)
//        bottomNavigationView.selectedItemId = menuItemId
//        bottomNavigationView.setOnNavigationItemSelectedListener(this)
//    }

}