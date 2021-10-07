package at.ac.tuwien.caa.docscan.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import at.ac.tuwien.caa.docscan.databinding.ActivityIntroBinding
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler
import org.koin.android.ext.android.inject

class IntroActivity : AppCompatActivity() {

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

        val pagerAdapter = PageSlideAdapter(supportFragmentManager)

        binding.introViewpager.setPageTransformer(true, ZoomOutPageTransformer())
        binding.introViewpager.adapter = pagerAdapter
        binding.tabDots.setupWithViewPager(binding.introViewpager, true)

        binding.introSkipButton.setOnClickListener {
            // fallback to previous activities
            finish()
        }
    }

    private class PageSlideAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): IntroFragment = IntroFragment.newInstance(position)
        override fun getCount() = 5
    }
}
