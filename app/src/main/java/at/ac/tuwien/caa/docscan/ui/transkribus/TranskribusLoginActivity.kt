package at.ac.tuwien.caa.docscan.ui.transkribus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityLoginBinding
import at.ac.tuwien.caa.docscan.extensions.SnackbarOptions
import at.ac.tuwien.caa.docscan.extensions.bindInvisible
import at.ac.tuwien.caa.docscan.extensions.hideKeyboard
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.repository.is403
import at.ac.tuwien.caa.docscan.rest.User
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Created by fabian on 08.02.2017.
 */
class TranskribusLoginActivity : BaseNoNavigationActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var mParentClass: Class<*>? = null
    private val viewModel: LoginViewModel by viewModel()
    private val dialogViewModel: DialogViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.initToolbarTitle(R.string.login_title)
        binding.loginButton.setOnClickListener {
            login()
            hideKeyboard()
        }
        observe()

        // Check what is the parent class that should be shown after the login is done:
        val extras = intent.extras
        mParentClass = if (extras != null && extras.getString(PARENT_ACTIVITY_NAME, "").startsWith(
                DocumentViewerActivity::class.java.name
            )
        ) DocumentViewerActivity::class.java else CameraActivity::class.java
    }

    private fun observe() {
        viewModel.observableLogin.observe(this, {
            when (it) {
                is Failure -> {
                    // http forbidden means that the username/password is wrong.
                    if (it.exception.is403()) {
                        binding.passwordEdittext.error =
                            resources.getString(R.string.login_auth_error_text)
                    } else {
//                        TODO: Log error
//                        FirebaseCrashlytics.getInstance().log("login error: " + "login_timeout_error")
                        it.exception.handleError(this)
                    }
                }
                is Success -> {
                    val dialog = DialogModel(
                        dialogAction = ADialog.DialogAction.LOGIN_SUCCESS,
                        customTitle = String.format(
                            getString(R.string.login_dialog_success_title),
                            it.data.firstName
                        )
                    )
                    showDialog(dialog)
                }
            }
        })
        viewModel.observableProgress.observe(this, {
            showLoadingLayout(it)
        })
        dialogViewModel.observableDialogAction.observe(this, {
            it.getContentIfNotHandled()?.let { result ->
                when (result.dialogAction) {
                    ADialog.DialogAction.LOGIN_SUCCESS -> {
                        // Start the parent activity and remove everything from the back stack:
                        val intent = Intent(applicationContext, mParentClass)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    }
                    else -> {
                        // ignore
                    }
                }
            }
        })
    }

    private fun login() {
        binding.passwordEdittext.error = null
        val email = binding.usernameEdittext.text.toString()
        val pw = binding.passwordEdittext.text.toString()
        if (email.isEmpty() || pw.isEmpty()) {
            singleSnackbar(
                binding.root,
                SnackbarOptions(getString(R.string.login_check_input_toast))
            )
            return
        }
        User.getInstance().userName = email
        User.getInstance().password = pw

        viewModel.login(email, pw)
    }

    private fun showLoadingLayout(showLoading: Boolean) {
        binding.loginFieldsLayout.bindInvisible(showLoading)
        binding.loginLoadingLayout.bindInvisible(!showLoading)
    }

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, TranskribusLoginActivity::class.java)
        }

        const val PARENT_ACTIVITY_NAME = "PARENT_ACTIVITY_NAME"
    }
}