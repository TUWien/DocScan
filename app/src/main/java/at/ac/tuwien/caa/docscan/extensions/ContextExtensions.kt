package at.ac.tuwien.caa.docscan.extensions

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

/**
 * Checks the google API availability, if the error can be recovered by the user, then a dialog
 * is shown.
 */
fun checksGoogleAPIAvailability(context: Context, showErrorDialog: Boolean = true): Boolean {
    val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (result == ConnectionResult.SUCCESS) {
        return true
    }
    if (GoogleApiAvailability.getInstance().isUserResolvableError(result)) {
        Timber.w("Google PlayServices out of date!")
        if (showErrorDialog) {
            GoogleApiAvailability.getInstance().showErrorNotification(context, result)
        }
    } else {
        Timber.e("Google PlayServices not available!")
    }
    return false
}
