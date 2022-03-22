package at.ac.tuwien.caa.docscan.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.MainContainerViewBinding
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.extensions.shareFileAsEmailLog
import at.ac.tuwien.caa.docscan.extensions.showAppSettings
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.dialog.*
import at.ac.tuwien.caa.docscan.ui.info.LogViewModel
import at.ac.tuwien.caa.docscan.ui.intro.IntroActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class StartActivity : BaseActivity() {

    private val viewModel: StartViewModel by viewModel()
    private val logViewModel: LogViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()
    private val preferenceHandler: PreferencesHandler by inject()

    private lateinit var binding: MainContainerViewBinding

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()

        ) { map: Map<String, Boolean> ->
            if (map.filter { entry -> !entry.value }.isEmpty()) {
                viewModel.checkStartUpConditions(Bundle())
            } else {
                showDialog(ADialog.DialogAction.RATIONALE_MIGRATION_PERMISSION)
            }
        }

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, StartActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash screen visible for this Activity
        val shouldPerformDBMigration = preferenceHandler.shouldPerformDBMigration
        splashScreen.setKeepOnScreenCondition { !shouldPerformDBMigration }
        binding = MainContainerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observe()
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkStartUpConditions(Bundle())
    }

    private fun observe() {
        viewModel.loadingProgress.observe(this) {
            binding.progress.visibility = if (it) View.VISIBLE else View.GONE
        }
        logViewModel.observableShareUris.observe(this, ConsumableEvent { uri ->
            if (!shareFileAsEmailLog(this, PageFileType.ZIP, uri)) {
                showDialog(ADialog.DialogAction.ACTIVITY_NOT_FOUND_EMAIL)
            }
        })
        viewModel.destination.observe(this, ConsumableEvent { pair ->
            when (pair.first) {
                StartDestination.CAMERA -> {
                    startCameraIntent()
                }
                StartDestination.INTRO -> {
                    startActivity(IntroActivity.newInstance(this))
                }
                StartDestination.PERMISSIONS -> {
                    requestPermissionLauncher.launch(PermissionHandler.requiredMandatoryPermissions)
                }
                StartDestination.MIGRATION_DIALOG_1 -> {
                    showDialog(ADialog.DialogAction.MIGRATION_DIALOG_ONE, pair.second)
                }
                StartDestination.MIGRATION_DIALOG_2 -> {
                    showDialog(ADialog.DialogAction.MIGRATION_DIALOG_TWO, pair.second)
                }
            }
        })
        viewModel.migrationError.observe(this, ConsumableEvent { throwable ->
            Timber.e(throwable, "Migration error occurred")
            val hasIOErrorHappenedDueToMissingSpace =
                when (throwable.getDocScanIOError()?.ioErrorCode) {
                    // it is very likely that these errors have happened due to missing storage
                    IOErrorCode.FILE_COPY_ERROR, IOErrorCode.NOT_ENOUGH_DISK_SPACE -> {
                        true
                    }
                    else -> {
                        false
                    }
                }
            val dialogModel = DialogModel(
                ADialog.DialogAction.MIGRATION_FAILED,
                customMessage = getString(if (hasIOErrorHappenedDueToMissingSpace) R.string.migration_failed_missing_space_text else R.string.migration_failed_text)
            )
            showDialog(dialogModel)
        })
        dialogViewModel.observableDialogAction.observe(this, ConsumableEvent { dialogResult ->
            when (dialogResult.dialogAction) {
                ADialog.DialogAction.RATIONALE_MIGRATION_PERMISSION -> {
                    when {
                        dialogResult.isPositive() -> {
                            requestPermissionLauncher.launch(PermissionHandler.requiredMandatoryPermissions)
                        }
                        dialogResult.isNegative() -> {
                            finish()
                        }
                        dialogResult.isNeutral() -> {
                            showAppSettings(this)
                        }
                    }
                }
                ADialog.DialogAction.MIGRATION_DIALOG_ONE -> {
                    if (dialogResult.isPositive()) {
                        viewModel.checkStartUpConditions(dialogResult.arguments.appendMigrationDialogOne())
                    } else if (dialogResult.isNegative()) {
                        finish()
                    }
                }
                ADialog.DialogAction.MIGRATION_DIALOG_TWO -> {
                    if (dialogResult.isPositive()) {
                        viewModel.checkStartUpConditions(dialogResult.arguments.appendMigrationDialogTwo())
                    } else if (dialogResult.isNegative()) {
                        finish()
                    }
                }
                ADialog.DialogAction.MIGRATION_FAILED -> {
                    if (dialogResult.isPositive()) {
                        finish()
                    } else if (dialogResult.isNegative()) {
                        logViewModel.shareLog()
                    }
                }
                else -> {
                    // ignore
                }
            }
        })
    }

    private fun startCameraIntent() {
        startActivity(CameraActivity.newInstance(this, false))
        finish()
    }
}
