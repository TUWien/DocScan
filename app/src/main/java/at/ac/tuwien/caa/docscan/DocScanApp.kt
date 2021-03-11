package at.ac.tuwien.caa.docscan

import androidx.multidex.MultiDexApplication
import at.ac.tuwien.caa.docscan.koin.appModule
import at.ac.tuwien.caa.docscan.koin.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * @author matejbart
 */
class DocScanApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        insertKoin()
    }

    private fun insertKoin() {
        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@DocScanApp)
            modules(listOf(appModule, viewModelModule))
        }
    }
}
