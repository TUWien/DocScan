package at.ac.tuwien.caa.docscan

import android.app.Application
import at.ac.tuwien.caa.docscan.koin.appModule
import at.ac.tuwien.caa.docscan.koin.daoModule
import at.ac.tuwien.caa.docscan.koin.repositoryModule
import at.ac.tuwien.caa.docscan.koin.viewModelModule
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author matejbart
 */
class DocScanApp : Application() {

    private val preferencesHandler by inject<PreferencesHandler>()

    override fun onCreate() {
        super.onCreate()
        insertKoin()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            // initializing the firebase app is not necessary but only performed to indicate that if
            // there is a problem with the google services json
            val a = FirebaseApp.initializeApp(this)
            if (a == null || a.options.apiKey.isEmpty()) Timber.d(getString(R.string.start_firebase_not_auth_text))
        }

        logFirstAppStart()
    }

    private fun insertKoin() {
        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@DocScanApp)
            modules(listOf(appModule, daoModule, repositoryModule, viewModelModule))
        }
    }

    private fun logFirstAppStart() {
        if (preferencesHandler.firstStartDate == null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            preferencesHandler.firstStartDate = timeStamp
            // set for the crashlytics instance
            FirebaseCrashlytics.getInstance()
                .setCustomKey(PreferencesHandler.KEY_FIRST_START_DATE, timeStamp)
        }
    }
}
