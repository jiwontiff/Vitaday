package edu.sswu.vitaday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AccumulatedTimeFragment : Fragment() {

    private val viewModel: StatisticsViewModel by activityViewModels()

    private lateinit var tvToday: TextView
    private lateinit var tvThisWeek: TextView
    private lateinit var tvThisMonth: TextView
    private lateinit var tvLast7Days: TextView
    private lateinit var tvLast28Days: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accumulated_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        observeData()
    }

    private fun initViews(view: View) {
        tvToday = view.findViewById(R.id.tvToday)
        tvThisWeek = view.findViewById(R.id.tvThisWeek)
        tvThisMonth = view.findViewById(R.id.tvThisMonth)
        tvLast7Days = view.findViewById(R.id.tvLast7Days)
        tvLast28Days = view.findViewById(R.id.tvLast28Days)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayDuration.collect { duration ->
                tvToday.text = viewModel.formatTime(duration)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.thisWeekDuration.collect { duration ->
                tvThisWeek.text = viewModel.formatTime(duration)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.thisMonthDuration.collect { duration ->
                tvThisMonth.text = viewModel.formatTime(duration)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.last7DaysDuration.collect { duration ->
                tvLast7Days.text = viewModel.formatTime(duration)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.last28DaysDuration.collect { duration ->
                tvLast28Days.text = viewModel.formatTime(duration)
            }
        }
    }
}