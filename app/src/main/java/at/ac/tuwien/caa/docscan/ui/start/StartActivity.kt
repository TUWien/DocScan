package at.ac.tuwien.caa.docscan.ui.start

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import at.ac.tuwien.caa.docscan.databinding.MainContainerViewBinding
import at.ac.tuwien.caa.docscan.logic.DocumentStorage
import at.ac.tuwien.caa.docscan.logic.PermissionHandler
import at.ac.tuwien.caa.docscan.sync.SyncStorage
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.intro.IntroActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class StartActivity : AppCompatActivity() {

    private val viewModel: StartActivityViewModel by viewModel()
    private lateinit var binding: MainContainerViewBinding

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()

        ) { map: MutableMap<String, Boolean> ->
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
                                // TODO: show rationale dialog
//                                return@let
                            }
                        }
                        requestPermissionLauncher.launch(PermissionHandler.requiredMandatoryPermissions)
                    }
                }
            }
        })
    }

    private fun startCameraIntent() {
        // TODO --- the old deprecated storage handling will be removed soon
        if (DocumentStorage.isStoreFileExisting(this)) {
            DocumentStorage.loadJSON(this)
        } else {
            DocumentStorage.clearInstance();
        }
        SyncStorage.loadJSON(this)
        // TODO --- the old deprecated storage handling will be removed soon

        startActivity(CameraActivity.newInstance(this))
        finish()
    }
}