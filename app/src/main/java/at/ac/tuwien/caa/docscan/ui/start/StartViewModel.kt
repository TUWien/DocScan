package at.ac.tuwien.caa.docscan.ui.start

import android.os.Bundle
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
    val destination: MutableLiveData<Event<Pair<StartDestination, Bundle>>> = MutableLiveData()
    val migrationError: MutableLiveData<Event<Throwable>> = MutableLiveData()

    fun checkStartUpConditions(arguments: Bundle) {
        loadingProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) job@{

            // permissions are only necessary if the migration is performed
            if (preferencesHandler.shouldPerformDBMigration && arePermissionsMissing()) {
                proceed(StartDestination.PERMISSIONS, arguments)
                return@job
            }
            // 2. perform the migration if necessary
            if (preferencesHandler.shouldPerformDBMigration) {
                // check the info dialogs first
                if (!arguments.extractMigrationDialogOne()) {
                    proceed(StartDestination.MIGRATION_DIALOG_1, arguments)
                    return@job
                } else if (!arguments.extractMigrationDialogTwo()) {
                    proceed(StartDestination.MIGRATION_DIALOG_2, arguments)
                    return@job
                }
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
                proceed(StartDestination.INTRO, arguments)
                return@job
            }

            // 4. as a final option, move to the camera
            proceed(StartDestination.CAMERA, arguments)
        }
    }

    private fun proceed(startDestination: StartDestination, bundle: Bundle) {
        loadingProgress.postValue(false)
        destination.postValue(Event(Pair(startDestination, bundle)))
    }

    private fun arePermissionsMissing() = !PermissionHandler.checkMandatoryPermissions(app)
}

enum class StartDestination {
    CAMERA,
    INTRO,
    PERMISSIONS,
    MIGRATION_DIALOG_1,
    MIGRATION_DIALOG_2
}