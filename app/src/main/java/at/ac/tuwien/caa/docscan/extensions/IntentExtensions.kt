package at.ac.tuwien.caa.docscan.extensions

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Safely handles cases in which no activities are installed to handle the given intent.
 * If no activity can handle the intent, the intent is not started, but an error dialog is shown instead.
 *
 * @return returns true if the intent was started; false otherwise
 */
fun FragmentActivity.safeStartActivity(intent: Intent): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (exception: ActivityNotFoundException) {
        FirebaseCrashlytics.getInstance().recordException(exception)
        Timber.d(exception, "No activity found to start intent.")
        false
    }
}
