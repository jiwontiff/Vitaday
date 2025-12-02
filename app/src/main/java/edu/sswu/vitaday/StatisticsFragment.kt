package edu.sswu.vitaday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private val viewModel: StatisticsViewModel by viewModels {
        val database = UserDatabase.getDatabase(requireContext())
        val repository = StatisticsRepository(
            database.timerSessionDao(),
            database.subjectDao()
        )
        StatisticsViewModelFactory(repository)
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tvTotalCount: TextView
    private lateinit var tvTotalTime: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewPager()
        setupTabs()
        observeData()
    }

    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
    }

    private fun setupViewPager() {
        val adapter = StatisticsPagerAdapter(this)
        viewPager.adapter = adapter
    }

    private fun setupTabs() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "누적 시간"
                1 -> "과목별 분포"
                2 -> "그래프"
                else -> ""
            }
        }.attach()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalSessionCount.collect { count ->
                tvTotalCount.text = "${count}회"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalDuration.collect { duration ->
                tvTotalTime.text = viewModel.formatTimeKorean(duration)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAllStatistics()
    }
}