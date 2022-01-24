package at.ac.tuwien.caa.docscan.ui.start

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.repository.migration.MigrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StartViewModel(
    private val app: DocScanApp,
    private val migrationRepository: MigrationRepository,
    private val preferencesHandler: PreferencesHandler
) : ViewModel() {
    val loadingProgress = MutableLiveData(true)
    val destination: MutableLiveData<Event<StartDestination>> = MutableLiveData()
    val migrationError: MutableLiveData<Event<Throwable>> = MutableLiveData()

    fun checkStartUpConditions() {
        loadingProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) job@{
            // 1. check if the permissions are given, otherwise it's not possible to use the app.
            if (arePermissionsMissing()) {
                proceed(StartDestination.PERMISSIONS)
                return@job
            }
            // 2. perform the migration if necessary
            if (preferencesHandler.shouldPerformDBMigration) {
                // the migration shouldn't be cancellable at all
                val result = withContext(NonCancellable) {
                    migrationRepository.migrateJsonDataToDatabase(app)
                }
                when (result) {
                    is Failure -> {
                        migrationError.postValue(Event(result.exception))
                    }
                    is Success -> {
                        // ignore, fallthrough to next requirement
                    }
                }
            }
            // 3. Check if the intro needs to be shown
            if (preferencesHandler.showIntro) {
                proceed(StartDestination.INTRO)
                return@job
            }

            // 4. as a final option, move to the camera
            proceed(StartDestination.CAMERA)
        }
    }

    private fun proceed(startDestination: StartDestination) {
        loadingProgress.postValue(false)
        destination.postValue(Event(startDestination))
    }

    private fun arePermissionsMissing() = !PermissionHandler.checkMandatoryPermissions(app)
}

enum class StartDestination {
    CAMERA,
    INTRO,
    PERMISSIONS
}