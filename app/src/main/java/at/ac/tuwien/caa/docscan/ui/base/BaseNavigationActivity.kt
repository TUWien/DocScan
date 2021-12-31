package at.ac.tuwien.caa.docscan.ui.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer.NavigationItem
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Abstract class used for inherited activities whose properties are partially defined in
 * NavigationDrawer.NavigationItemEnum (for example title shown in the Actionbar).
 * Steps to create a child class:
 * 1: Create the XML layout.
 * 2: Implement the Activity. Take care to implement getSelfNavDrawerItem properly.
 * 3: Put the menu item in drawer_menu.xml
 * 4: Define the NavigationItemEnum (in NavigationDrawer)
 * 5: Define the Activity in AndroidManifest.xml
 */
abstract class BaseNavigationActivity : BaseActivity() {
    private lateinit var drawer: NavigationDrawer

    private val viewModel: UserViewModel by viewModel()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawer = NavigationDrawer(this, selfNavDrawerItem)
        observe()
    }

    private fun observe() {
        viewModel.observableUser.observe(this, {
            if (!isFinishing) {
                drawer.setupDrawerHeader(it)
            }
        })
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        initToolbar()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        initToolbar()
    }

    private fun initToolbar() {
        findViewById<Toolbar>(R.id.main_toolbar)?.let {
            setSupportActionBar(it)
            supportActionBar?.setTitle(selfNavDrawerItem.titleResource)
            setToolbarForNavigation(it)
        }
    }

    private fun setToolbarForNavigation(toolbar: Toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp)
        toolbar.setNavigationOnClickListener { drawer.showNavigation() }
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses of
     * BaseNavigationActivity override this to indicate what nav drawer item corresponds to them Return
     * NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected abstract val selfNavDrawerItem: NavigationItem
}
