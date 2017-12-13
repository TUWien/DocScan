package at.ac.tuwien.caa.docscan.ui;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 25.08.2017.
 */

public abstract class BaseNoNavigationActivity extends AppCompatActivity {

    /**
     * Sets the title of the toolbar.
     * @param titleID ID of the title string defined in strings.xml
     */
    protected void initToolbarTitle(int titleID) {

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(titleID));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
