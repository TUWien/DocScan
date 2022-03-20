package at.ac.tuwien.caa.docscan.log

import android.util.Log
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * A timber tree which records all [Timber.e] calls with [FirebaseCrashlytics].
 */
class FirebaseCrashlyticsTimberTree(
    private val preferencesHandler: PreferencesHandler
) : Timber.Tree() {

    companion object {
        val LOGGABLE_PRIORITIES = listOf(Log.ERROR)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        if (!preferencesHandler.isCrashReportingEnabled) return false
        return LOGGABLE_PRIORITIES.contains(priority)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        FirebaseCrashlytics.getInstance().log((tag ?: "") + message)
        t?.let {
            FirebaseCrashlytics.getInstance().recordException(it)
        }
    }
}
