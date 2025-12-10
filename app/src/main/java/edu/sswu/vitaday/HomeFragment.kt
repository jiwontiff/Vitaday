package edu.sswu.vitaday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.sswu.vitaday.ui.timer.PomodoroTimerFragment
import edu.sswu.vitaday.ui.timer.TimerViewModel
import edu.sswu.vitaday.ui.timer.TimerViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class HomeFragment : Fragment() {

    private val sharedViewModel: SharedSubjectViewModel by activityViewModels()

    private val timerViewModel: TimerViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            TimerViewModelFactory(requireActivity().application)
        )[TimerViewModel::class.java]
    }

    private var rvSubjects: RecyclerView? = null
    private var btnAddSubject: Button? = null
    private var tvTodayStudyTime: TextView? = null
    private var tvCurrentDate: TextView? = null
    private var tvEmptyMessage: TextView? = null

    private var subjectAdapter: SubjectAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_home, container, false)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error inflating layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initViews(view)
            setupRecyclerView()
            setupButtons()
            observeViewModel()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onViewCreated", e)
            Toast.makeText(requireContext(), "초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews(view: View) {
        rvSubjects = view.findViewById(R.id.rv_subjects)
        btnAddSubject = view.findViewById(R.id.btn_add_subject)
        tvTodayStudyTime = view.findViewById(R.id.tv_today_study_time)
        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        if (rvSubjects == null || btnAddSubject == null || tvTodayStudyTime == null ||
            tvCurrentDate == null || tvEmptyMessage == null) {
            throw IllegalStateException("Required views not found")
        }

        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
        tvCurrentDate?.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        try {
            subjectAdapter = SubjectAdapter(
                onSubjectClick = { subject ->
                    openTimerFragment(subject.id)
                },
                onSubjectDoubleClick = { subject ->
                    showEditSubjectDialog(subject)
                },
                onSubjectDelete = { subject ->
                    showDeleteConfirmDialog(subject)
                },
                getTodayTime = { subjectId ->
                    val todayTime = timerViewModel.getTimeBySubjectForDate(Date())[subjectId] ?: 0L
                    "오늘: ${timerViewModel.formatTime(todayTime)}"
                }
            )

            rvSubjects?.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = subjectAdapter
            }

            val callback = SubjectItemTouchHelper(subjectAdapter!!) {
                val reorderedList = subjectAdapter?.getReorderedList() ?: return@SubjectItemTouchHelper
                sharedViewModel.reorderSubjects(reorderedList)
                Toast.makeText(requireContext(), "순서가 변경되었습니다", Toast.LENGTH_SHORT).show()
            }
            itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper?.attachToRecyclerView(rvSubjects)

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up RecyclerView", e)
            Toast.makeText(requireContext(), "RecyclerView 설정 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        btnAddSubject?.setOnClickListener {
            showAddSubjectDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.subjects.collectLatest { subjects ->
                if (subjects.isEmpty()) {
                    rvSubjects?.visibility = View.GONE
                    tvEmptyMessage?.visibility = View.VISIBLE
                } else {
                    rvSubjects?.visibility = View.VISIBLE
                    tvEmptyMessage?.visibility = View.GONE
                    subjectAdapter?.submitList(subjects)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            timerViewModel.sessions.collectLatest {
                val todayTime = timerViewModel.getTotalTimeForDate(Date())
                tvTodayStudyTime?.text = "오늘 공부 시간: ${timerViewModel.formatTime(todayTime)}"
                subjectAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun openTimerFragment(subjectId: Int) {
        try {
            val fragment = PomodoroTimerFragment.newInstance(subjectId)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error opening timer", e)
            Toast.makeText(requireContext(), "타이머 열기 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddSubjectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)

        val colors = listOf(
            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
        )

        var selectedColor = colors[0]

        colors.forEach { colorHex ->
            val colorButton = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(colorHex.toColorInt())
                setOnClickListener {
                    selectedColor = colorHex
                    for (i in 0 until llColorPicker.childCount) {
                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
                    }
                    setPadding(4, 4, 4, 4)
                }
            }
            llColorPicker.addView(colorButton)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val name = etSubjectName.text.toString().trim()
                if (name.isNotEmpty()) {
                    sharedViewModel.addSubject(name, selectedColor)
                    Toast.makeText(requireContext(), "과목이 추가되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditSubjectDialog(subject: SubjectData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_subject, null)
        val etSubjectName = dialogView.findViewById<EditText>(R.id.et_subject_name)
        val llColorPicker = dialogView.findViewById<LinearLayout>(R.id.ll_color_picker)

        etSubjectName.setText(subject.name)

        val colors = listOf(
            "#FF5252", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#FFEB3B", "#FF9800"
        )

        var selectedColor = subject.colorHex

        colors.forEach { colorHex ->
            val colorButton = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(colorHex.toColorInt())

                if (colorHex == subject.colorHex) {
                    setPadding(4, 4, 4, 4)
                }

                setOnClickListener {
                    selectedColor = colorHex
                    for (i in 0 until llColorPicker.childCount) {
                        llColorPicker.getChildAt(i).setPadding(0, 0, 0, 0)
                    }
                    setPadding(4, 4, 4, 4)
                }
            }
            llColorPicker.addView(colorButton)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("과목 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val name = etSubjectName.text.toString().trim()
                if (name.isNotEmpty()) {
                    sharedViewModel.updateSubject(subject.id, name, selectedColor)
                    Toast.makeText(requireContext(), "과목이 수정되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteConfirmDialog(subject: SubjectData) {
        AlertDialog.Builder(requireContext())
            .setTitle("과목 삭제")
            .setMessage("'${subject.name}' 과목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                sharedViewModel.removeSubject(subject.id)
                Toast.makeText(requireContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvSubjects = null
        btnAddSubject = null
        tvTodayStudyTime = null
        tvCurrentDate = null
        tvEmptyMessage = null
        subjectAdapter = null
        itemTouchHelper = null
    }
}