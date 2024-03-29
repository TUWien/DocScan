package at.ac.tuwien.caa.docscan.ui.base

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import at.ac.tuwien.caa.docscan.R

/**
 * Created by fabian on 25.08.2017.
 */
abstract class BaseNoNavigationActivity : BaseActivity() {

    /**
     * Sets the title of the toolbar.
     *
     * @param titleID ID of the title string defined in strings.xml
     */
    protected fun initWithTitle(titleID: Int?) {
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if(titleID != null) getString(titleID) else ""
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
