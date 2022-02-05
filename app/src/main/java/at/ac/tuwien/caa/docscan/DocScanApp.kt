package at.ac.tuwien.caa.docscan

import android.app.Application
import androidx.work.WorkManager
import at.ac.tuwien.caa.docscan.koin.*
import at.ac.tuwien.caa.docscan.log.FirebaseCrashlyticsTimberTree
import at.ac.tuwien.caa.docscan.log.InternalLogTimberTree
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.logic.notification.NotificationHandler
import at.ac.tuwien.caa.docscan.worker.DocumentSanitizeWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author matejbart
 */
class DocScanApp : Application() {

    private val preferencesHandler by inject<PreferencesHandler>()
    private val workManager by inject<WorkManager>()
    private val notificationHandler by inject<NotificationHandler>()
    private val fileHandler by inject<FileHandler>()
    private val appIOScope = CoroutineScope(Dispatchers.IO)

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

        Timber.plant(FirebaseCrashlyticsTimberTree(preferencesHandler))
        Timber.plant(InternalLogTimberTree(fileHandler, appIOScope))

        // initializes notification channels
        notificationHandler.initNotificationChannels()
        // spawn the sanitizer to check potential broken states
        DocumentSanitizeWorker.spawnSanitize(workManager)
        logFirstAppStart()
    }

    private fun insertKoin() {
        startKoin {
            // currently a workaround for koin (see https://github.com/InsertKoinIO/koin/issues/1188)
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@DocScanApp)
            modules(listOf(appModule, daoModule, repositoryModule, viewModelModule, networkModule))
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
