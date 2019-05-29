package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.text.SimpleDateFormat;
import java.util.Date;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.license.LicenseActivity;

/**
 * Created by fabian on 30.11.2016.
 */
public class AboutActivity extends BaseNavigationActivity {

    public static String KEY_SHOW_INTRO = "KEY_SHOW_INTRO";

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
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        final Context context = this;

        Button introButton = findViewById(R.id.about_intro_button);
        introButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, StartActivity.class);
                intent.putExtra(KEY_SHOW_INTRO, true);
                startActivity(intent);
            }
        });

        Button licensesButton = findViewById(R.id.about_licenses_button);
        licensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, LicenseActivity.class));
            }
        });

    }

    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.ABOUT;
    }
}
