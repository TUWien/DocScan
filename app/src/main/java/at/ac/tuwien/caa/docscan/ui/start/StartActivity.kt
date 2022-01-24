package at.ac.tuwien.caa.docscan.ui.start

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import at.ac.tuwien.caa.docscan.databinding.MainContainerViewBinding
import at.ac.tuwien.caa.docscan.logic.PermissionHandler
import at.ac.tuwien.caa.docscan.logic.getMessage
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.intro.IntroActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * TODO: MIGRATION_LOGIC - Check if more sophisticated migration options with user feedback are necessary.
 */
class StartActivity : BaseActivity() {

    private val viewModel: StartViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()

    private lateinit var binding: MainContainerViewBinding

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()

        ) { map: Map<String, Boolean> ->
            if (map.filter { entry -> !entry.value }.isEmpty()) {
                viewModel.checkStartUpConditions()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }


    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, StartActivity::class.java).apply {
                // TODO: This is necessary when the intro needs to be shown again.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainContainerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkStartUpConditions()
    }

    private fun observe() {
        viewModel.loadingProgress.observe(this, {
            binding.progress.visibility = if (it) View.VISIBLE else View.GONE
        })
        viewModel.destination.observe(this, {
            it.getContentIfNotHandled()?.let { destination ->
                when (destination) {
                    StartDestination.CAMERA -> {
                        startCameraIntent()
                    }
                    StartDestination.INTRO -> {
                        startActivity(IntroActivity.newInstance(this))
                    }
                    StartDestination.PERMISSIONS -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(PermissionHandler.requiredMandatoryPermissions.first())) {
                                showDialog(ADialog.DialogAction.RATIONALE_CAMERA_PERMISSION)
                                return@let
                            }
                        }
                        requestPermissionLauncher.launch(PermissionHandler.requiredMandatoryPermissions)
                    }
                }
            }
        })
        viewModel.migrationError.observe(this, {
            it.getContentIfNotHandled()?.let { throwable ->
                // TODO: finalize this call
                val dialogModel = DialogModel(
                    ADialog.DialogAction.MIGRATION_FAILED,
                    customMessage = throwable.getMessage(this, true)
                )
                showDialog(dialogModel)
            }
        })
        dialogViewModel.observableDialogAction.observe(this, {
            it.getContentIfNotHandled()?.let { dialogResult ->
                when (dialogResult.dialogAction) {
                    ADialog.DialogAction.RATIONALE_CAMERA_PERMISSION -> {
                        requestPermissionLauncher.launch(PermissionHandler.requiredMandatoryPermissions)
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
    }

    private fun startCameraIntent() {
        startActivity(CameraActivity.newInstance(this, false))
        finish()
    }
}
