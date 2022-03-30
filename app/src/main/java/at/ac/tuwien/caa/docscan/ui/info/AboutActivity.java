package at.ac.tuwien.caa.docscan.ui.info;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import at.ac.tuwien.caa.docscan.BuildConfig;
import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.extensions.DateExtensionKt;
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer;
import at.ac.tuwien.caa.docscan.ui.intro.IntroActivity;
import at.ac.tuwien.caa.docscan.ui.license.LicenseActivity;

/**
 * Created by fabian on 30.11.2016.
 */
public class AboutActivity extends BaseNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Show the app version number and build time:
        String versionName = getResources().getString(R.string.about_version_prefix_text);
        versionName += " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
        TextView versionTextView = findViewById(R.id.about_version_textview);
        versionTextView.setText(versionName);

        String buildTime = getResources().getString(R.string.about_buildtime_prefix_text);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        TextView buildDateTextView = findViewById(R.id.about_buildtime_textview);
        buildTime += " " + DateExtensionKt.getTimeStamp(buildDate);
        buildDateTextView.setText(buildTime);

        final Context context = this;

        Button introButton = findViewById(R.id.about_intro_button);
        introButton.setOnClickListener(v -> startActivity(IntroActivity.Companion.newInstance(context)));

        Button licensesButton = findViewById(R.id.about_licenses_button);
        licensesButton.setOnClickListener(v -> startActivity(new Intent(context, LicenseActivity.class)));

    }

    @NonNull
    @Override
    protected NavigationDrawer.NavigationItem getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItem.ABOUT;
    }

    public static Intent newInstance(Context context) {
        return new Intent(context, AboutActivity.class);
    }
}
