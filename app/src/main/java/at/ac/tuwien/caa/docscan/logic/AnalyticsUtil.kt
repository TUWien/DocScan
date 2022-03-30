package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AnalyticsUtil {

    fun setAnalyticsReportingEnabled(context: Context, isEnabled: Boolean) {
        // crashlytics and analytics are kind of tight together, therefore both needs to be explicitly enabled/disabled.
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(isEnabled)
        FirebaseAnalytics.getInstance(context)
            .setAnalyticsCollectionEnabled(isEnabled)
    }
}