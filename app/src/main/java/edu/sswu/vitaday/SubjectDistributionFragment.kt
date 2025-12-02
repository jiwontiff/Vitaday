package edu.sswu.vitaday

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

class SubjectDistributionFragment : Fragment() {

    private val viewModel: StatisticsViewModel by activityViewModels()

    private lateinit var pieChart: PieChart
    private lateinit var rvSubjectList: RecyclerView
    private lateinit var adapter: SubjectDistributionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subject_distribution, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupPieChart()
        observeData()
    }

    private fun initViews(view: View) {
        pieChart = view.findViewById(R.id.pieChart)
        rvSubjectList = view.findViewById(R.id.rvSubjectList)

        adapter = SubjectDistributionAdapter()
        rvSubjectList.layoutManager = LinearLayoutManager(context)
        rvSubjectList.adapter = adapter
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.parseColor("#121212"))
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(false)
            legend.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subjectDistribution.collect { distribution ->
                updatePieChart(distribution)
                adapter.submitList(distribution)
            }
        }
    }

    private fun updatePieChart(distribution: List<SubjectDuration>) {
        if (distribution.isEmpty()) {
            pieChart.clear()
            return
        }

        val entries = distribution.map {
            PieEntry(it.totalDuration.toFloat(), it.subjectName)
        }

        val colors = listOf(
            Color.parseColor("#FF5252"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#E91E63")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }
}