package at.ac.tuwien.caa.docscan.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import at.ac.tuwien.caa.docscan.databinding.ActivityIntroBinding
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject

class IntroActivity : BaseActivity() {

    private val preferencesHandler by inject<PreferencesHandler>()
    private lateinit var binding: ActivityIntroBinding

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, IntroActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // mark intro as false to not show it again
        preferencesHandler.showIntro = false

        binding.viewpager2?.run {
            adapter = PageSlideAdapter()
            setPageTransformer(ZoomOutPageTransformer())
            TabLayoutMediator(binding.tabDots, this) { _, _ ->
            }.attach()
        }

        binding.introSkipButton.setOnClickListener {
            // fallback to previous activities
            finish()
        }
    }

    inner class PageSlideAdapter :
            FragmentStateAdapter(this) {
        override fun getItemCount() = 5
        override fun createFragment(position: Int): IntroFragment =
                IntroFragment.newInstance(position)
    }
}
