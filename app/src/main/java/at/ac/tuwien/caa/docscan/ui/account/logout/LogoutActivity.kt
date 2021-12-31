package at.ac.tuwien.caa.docscan.ui.account.logout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityLogoutBinding
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer.NavigationItem
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Created by fabian on 24.08.2017.
 */
class LogoutActivity : BaseNoNavigationActivity() {

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, LogoutActivity::class.java)
        }
    }

    private val viewModel: LogoutViewModel by viewModel()
    private lateinit var binding: ActivityLogoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWithTitle(R.string.account_logout)
        binding.logoutConfirmButton.setOnClickListener {
            viewModel.logout()
            finish()
        }
        binding.logoutCancelButton.setOnClickListener {
            finish()
        }
    }
}
