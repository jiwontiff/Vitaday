package edu.sswu.vitaday

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StudyGraphFragment : Fragment() {

    private val viewModel: StatisticsViewModel by activityViewModels()

    private lateinit var barChart: BarChart
    private lateinit var btn7Days: Button
    private lateinit var btn28Days: Button

    private var currentDays = 7

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_study_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupBarChart()
        setupButtons()
        observeData()
    }

    private fun initViews(view: View) {
        barChart = view.findViewById(R.id.barChart)
        btn7Days = view.findViewById(R.id.btn7Days)
        btn28Days = view.findViewById(R.id.btn28Days)
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
                textSize = 10f
                granularity = 1f
            }

            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333333")
                axisMinimum = 0f
            }

            axisRight.isEnabled = false

            legend.apply {
                isEnabled = false
            }
        }
    }

    private fun setupButtons() {
        btn7Days.setOnClickListener {
            currentDays = 7
            updateButtonState()
            viewModel.refreshDailyDurations(7)
        }

        btn28Days.setOnClickListener {
            currentDays = 28
            updateButtonState()
            viewModel.refreshDailyDurations(28)
        }

        updateButtonState()
    }

    private fun updateButtonState() {
        if (currentDays == 7) {
            btn7Days.setBackgroundColor(Color.parseColor("#7C4DFF"))
            btn28Days.setBackgroundColor(Color.parseColor("#444444"))
        } else {
            btn7Days.setBackgroundColor(Color.parseColor("#444444"))
            btn28Days.setBackgroundColor(Color.parseColor("#7C4DFF"))
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyDurations.collect { durations ->
                updateBarChart(durations)
            }
        }
    }

    private fun updateBarChart(durations: List<DailyDuration>) {
        if (durations.isEmpty()) {
            barChart.clear()
            return
        }

        val entries = durations.mapIndexed { index, dailyDuration ->
            val hours = dailyDuration.totalDuration / (1000f * 60 * 60)
            BarEntry(index.toFloat(), hours)
        }

        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val labels = durations.map {
            dateFormat.format(Date(it.date))
        }
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val dataSet = BarDataSet(entries, "공부 시간").apply {
            color = Color.parseColor("#7C4DFF")
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val data = BarData(dataSet)
        data.barWidth = 0.8f
        barChart.data = data
        barChart.invalidate()
    }
}