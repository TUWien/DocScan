package at.ac.tuwien.caa.docscan.ui.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityShareLogBinding
import at.ac.tuwien.caa.docscan.extensions.bindVisible
import at.ac.tuwien.caa.docscan.extensions.shareFileAsEmailLog
import at.ac.tuwien.caa.docscan.logic.ConsumableEvent
import at.ac.tuwien.caa.docscan.logic.PageFileType
import at.ac.tuwien.caa.docscan.logic.handleError
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer.NavigationItem
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Created by fabian on 09.03.2017.
 */
class LogActivity : BaseNavigationActivity() {
    override val selfNavDrawerItem = NavigationItem.SHARE_LOG

    private val viewModel: LogViewModel by viewModel()
    private lateinit var binding: ActivityShareLogBinding

    companion object {
        fun newInstance(context: Context?): Intent {
            return Intent(context, LogActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val sendButton = findViewById<Button>(R.id.share_log_button)
        sendButton.setOnClickListener {
            viewModel.shareLog()
        }
        observe()
    }

    private fun observe() {
        viewModel.observableShareUris.observe(this, ConsumableEvent { uri ->
            if (!shareFileAsEmailLog(this, PageFileType.ZIP, uri)) {
                showDialog(ADialog.DialogAction.ACTIVITY_NOT_FOUND_EMAIL)
            }
        })
        viewModel.observableError.observe(this, ConsumableEvent { throwable ->
            throwable.handleError(this, logAsWarning = true)
        })
        viewModel.observableProgress.observe(this) {
            binding.progress.bindVisible(it)
        }
    }
}
