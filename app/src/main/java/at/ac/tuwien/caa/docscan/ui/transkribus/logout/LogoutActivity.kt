package at.ac.tuwien.caa.docscan.ui.transkribus.logout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.ac.tuwien.caa.docscan.databinding.ActivityLogoutBinding
import at.ac.tuwien.caa.docscan.ui.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.NavigationDrawer.NavigationItem
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Created by fabian on 24.08.2017.
 */
class LogoutActivity : BaseNavigationActivity() {

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, LogoutActivity::class.java)
        }
    }

    private val viewModel: LogoutViewModel by viewModel()
    private lateinit var binding: ActivityLogoutBinding

    override val selfNavDrawerItem = NavigationItem.LOGOUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.logoutConfirmButton.setOnClickListener {
            viewModel.logout()
            finish()
        }
        binding.logoutCancelButton.setOnClickListener {
            finish()
        }
    }
}
