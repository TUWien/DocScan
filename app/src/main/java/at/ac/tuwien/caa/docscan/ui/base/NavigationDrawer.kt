package at.ac.tuwien.caa.docscan.ui.base

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.User
import at.ac.tuwien.caa.docscan.extensions.safeStartActivity
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import at.ac.tuwien.caa.docscan.ui.info.AboutActivity
import at.ac.tuwien.caa.docscan.ui.info.LogActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import at.ac.tuwien.caa.docscan.ui.settings.PreferenceActivity
import at.ac.tuwien.caa.docscan.ui.account.TranskribusLoginActivity
import at.ac.tuwien.caa.docscan.ui.account.logout.LogoutActivity
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.navigation.NavigationView
import timber.log.Timber

/**
 * This file contains parts of this source file:
 * https://github.com/google/iosched/blob/e8c61e7e23f74aa6786696dad22e5136b423a334/android/src/main/java/com/google/samples/apps/iosched/navigation/AppNavigationViewAsDrawerImpl.java
 * Created by fabian on 30.11.2016.
 */
class NavigationDrawer(
    private val activity: BaseActivity,
    val selectedItem: NavigationItem
) : NavigationView.OnNavigationItemSelectedListener {


    companion object {
        private const val NAVDRAWER_LAUNCH_DELAY =
            250L // Delay to launch nav drawer item, to allow close animation to play
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mNavigationView: NavigationView

    private var isAccountGroupVisible = false

    init {
        setupNavigationDrawer()
        setupDrawerHeader(null)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        NavigationItem.getById(menuItem.itemId)?.apply {
            onNavDrawerItemClicked(this)
            return true
        } ?: kotlin.run {
            Timber.e("Navigation enum with the id ${menuItem.itemId} not found!")
            return false
        }
        return false
    }

    fun showNavigation() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setupNavigationDrawer() {
        drawerLayout = activity.findViewById(R.id.drawer_layout)

        val drawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            activity,
            drawerLayout,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                // Rotate the button to its initial state:
                val headerLayout = mNavigationView.getHeaderView(0)
                val button =
                    headerLayout.findViewById<View>(R.id.navigation_view_header_account_setting) as ImageButton
                button.rotation = 0f
                // Hide the account items:
                // TODO: Show the loggedIn state correctly!
                setAccountGroupVisible(false, false)
            }
        }
        // Set the drawer toggle as the DrawerListener
        drawerLayout.addDrawerListener(drawerToggle)
        mNavigationView = activity.findViewById(R.id.left_drawer)
        mNavigationView.setNavigationItemSelectedListener(this)
        createNavDrawerItems()
    }

    fun setupDrawerHeader(user: User?) {

        val headerLayout = mNavigationView.getHeaderView(0)
        val userTextView =
            headerLayout.findViewById<TextView>(R.id.navigation_view_header_user_textview)
        val connectionTextView =
            headerLayout.findViewById<TextView>(R.id.navigation_view_header_sync_textview)
        val userImageView =
            headerLayout.findViewById<ImageView>(R.id.navigation_view_header_user_image_view)

        val isUserLoggedIn = user != null

        connectionTextView.text =
            if (isUserLoggedIn) activity.getString(R.string.sync_transkribus_text) else ""

        GlideApp.with(activity)
            .load(if (isUserLoggedIn) R.drawable.transkribus else null)
            .apply(RequestOptions.circleCropTransform())
            .into(userImageView)

        user?.apply {
            userTextView.text = "$firstName $lastName"
        } ?: run {
            userTextView.text = activity.getString(R.string.account_not_logged_in)
        }

        // Add a callback to the account settings layout:
        val button =
            headerLayout.findViewById<ImageButton>(R.id.navigation_view_header_account_setting)
        val layout = headerLayout.findViewById<RelativeLayout>(R.id.account_layout)
        layout.setOnClickListener {
            val isGroupVisible = !isAccountGroupVisible
            setAccountGroupVisible(isGroupVisible, isUserLoggedIn)
            button.rotation = if (isGroupVisible) 0F else 180F
        }
    }

    private fun onNavDrawerItemClicked(item: NavigationItem) {
        if (item == selectedItem) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        Handler(activity.mainLooper).postDelayed({
            itemSelected(item)
        }, NAVDRAWER_LAUNCH_DELAY)

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setAccountGroupVisible(isVisible: Boolean, isUserLoggedIn: Boolean) {
        isAccountGroupVisible = isVisible
        showHideNavigationGroups(isVisible, isUserLoggedIn)
    }

    private fun showHideNavigationGroups(isAccountGroupVisible: Boolean, isUserLoggedIn: Boolean) {
        val menu = mNavigationView.menu
        menu.setGroupVisible(R.id.navigation_main_items_group, !isAccountGroupVisible)
        menu.setGroupVisible(R.id.navigation_settings_items_group, !isAccountGroupVisible)
        menu.setGroupVisible(R.id.account_group, isAccountGroupVisible)

        // logout and setup button are inverted
        menu.findItem(R.id.account_logout_item)?.isVisible = isAccountGroupVisible && isUserLoggedIn
        menu.findItem(R.id.account_edit_item)?.isVisible = isAccountGroupVisible && !isUserLoggedIn
    }

    private fun createNavDrawerItems() {
        val menu = mNavigationView.menu
        for (navigationItemEnum in NavigationItem.values()) {
            val item = menu.findItem(navigationItemEnum.id)
            if (item != null) {
                item.isVisible = true
                item.setIcon(navigationItemEnum.iconResource)
                item.setTitle(navigationItemEnum.titleResource)
            } else {
                Timber.d(
                    "Menu Item for navigation item with title " +
                            (if (navigationItemEnum.titleResource != 0) activity.resources.getString(
                                navigationItemEnum.titleResource
                            ) else "") + "not found"
                )
            }
        }
        mNavigationView.setNavigationItemSelectedListener(this)
        showHideNavigationGroups(isAccountGroupVisible, false)
    }

    private fun itemSelected(item: NavigationItem) {
        val intent = getIntentByNavItem(item)
        if (!activity.safeStartActivity(intent)) {
            activity.showDialog(ADialog.DialogAction.ACTIVITY_NOT_FOUND)
        }
        // if the activity has a drawer, it will mean that the root activity can be closed.
        if (item.hasDrawer) {
            activity.finish()
        }
    }

    private fun getIntentByNavItem(item: NavigationItem): Intent {
        return when (item) {
            NavigationItem.CAMERA -> {
                CameraActivity.newInstance(activity)
            }
            NavigationItem.ABOUT -> {
                AboutActivity.newInstance(activity)
            }
            NavigationItem.SHARE_LOG -> {
                LogActivity.newInstance(activity)
            }
            NavigationItem.LOGIN -> {
                TranskribusLoginActivity.newInstance(activity)
            }
            NavigationItem.LOGOUT -> {
                LogoutActivity.newInstance(activity)
            }
            NavigationItem.SETTINGS -> {
                PreferenceActivity.newInstance(activity)
            }
            NavigationItem.DOCUMENTS -> {
                DocumentViewerActivity.newInstance(activity)
            }
            NavigationItem.HELP -> {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://transkribus.eu/wiki/images/e/ed/How_to_use_DocScan_and_ScanTent.pdf")
                )
            }
        }
    }

    /**
     * Taken from: https://github.com/google/iosched
     * List of all possible navigation items.
     * Howto remove menu items: Comment the item below and change the return value in the
     * corresponding getSelfNavDrawerItem() function.
     */
    enum class NavigationItem(
        val id: Int, val titleResource: Int, val iconResource: Int, val hasDrawer: Boolean
    ) {
        CAMERA(
            R.id.camera_item, R.string.camera_item_text,
            R.drawable.ic_camera_alt_black_24dp,
            true
        ),
        ABOUT(
            R.id.about_item, R.string.about_item_text,
            R.drawable.ic_info_black_24dp,
            true
        ),
        SHARE_LOG(
            R.id.share_log_item, R.string.share_log_item_text,
            R.drawable.ic_share_black_24dp,
            true
        ),
        LOGIN(
            R.id.account_edit_item, R.string.account_edit_text,
            R.drawable.ic_account_box_black_24dp,
            false
        ),
        LOGOUT(
            R.id.account_logout_item, R.string.account_logout,
            R.drawable.ic_remove_circle_outline_black_24dp,
            false
        ),
        SETTINGS(
            R.id.settings_item, R.string.settings_item_text,
            R.drawable.ic_settings_black_24dp,
            true
        ),
        DOCUMENTS(
            R.id.documents_item, R.string.documents_item_text,
            R.drawable.ic_library_books_black_24dp,
            true
        ),
        HELP(
            R.id.help_item, R.string.help_item_text,
            R.drawable.ic_help_black_24dp,
            false
        );

        companion object {
            fun getById(id: Int): NavigationItem? {
                values().forEach {
                    if (it.id == id) {
                        return it
                    }
                }
                return null
            }
        }
    }
}
