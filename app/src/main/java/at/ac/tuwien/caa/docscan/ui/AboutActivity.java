package at.ac.tuwien.caa.docscan.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 30.11.2016.
 */
public class AboutActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Show the app version number and build time:
        try {
            String versionName = getResources().getString(R.string.about_version_prefix_text);
            versionName +=  " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            TextView versionTextView = findViewById(R.id.about_version_textview);
            versionTextView.setText(versionName);

            String buildTime = getResources().getString(R.string.about_buildtime_prefix_text);
            Date buildDate = new Date(BuildConfig.TIMESTAMP);
            TextView buildDateTextView = findViewById(R.id.about_buildtime_textview);
            SimpleDateFormat sdf = new SimpleDateFormat();
            String date = sdf.format(buildDate);
            buildTime += " " + date;
            buildDateTextView.setText(buildTime);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ABOUT;
    }
}
